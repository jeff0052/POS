package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.settlement.application.command.ApproveRefundCommand;
import com.developer.pos.v2.settlement.application.command.CreateRefundCommand;
import com.developer.pos.v2.settlement.application.dto.RefundRecordDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.RefundLineItemEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.RefundRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementPaymentHoldEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaRefundLineItemRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaRefundRecordRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementPaymentHoldRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberCouponRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RefundApplicationService implements UseCase {

    private final JpaRefundRecordRepository refundRecordRepository;
    private final JpaSettlementRecordRepository settlementRecordRepository;
    private final JpaRefundLineItemRepository refundLineItemRepository;
    private final JpaSettlementPaymentHoldRepository paymentHoldRepository;
    private final JpaMemberAccountRepository memberAccountRepository;
    private final JpaMemberCouponRepository memberCouponRepository;
    private final StoreAccessEnforcer storeAccessEnforcer;

    public RefundApplicationService(
            JpaRefundRecordRepository refundRecordRepository,
            JpaSettlementRecordRepository settlementRecordRepository,
            JpaRefundLineItemRepository refundLineItemRepository,
            JpaSettlementPaymentHoldRepository paymentHoldRepository,
            JpaMemberAccountRepository memberAccountRepository,
            JpaMemberCouponRepository memberCouponRepository,
            StoreAccessEnforcer storeAccessEnforcer
    ) {
        this.refundRecordRepository = refundRecordRepository;
        this.settlementRecordRepository = settlementRecordRepository;
        this.refundLineItemRepository = refundLineItemRepository;
        this.paymentHoldRepository = paymentHoldRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.memberCouponRepository = memberCouponRepository;
        this.storeAccessEnforcer = storeAccessEnforcer;
    }

    @Transactional
    public RefundRecordDto createRefund(CreateRefundCommand command) {
        AuthenticatedActor actor = AuthContext.current();

        SettlementRecordEntity settlement = settlementRecordRepository.findByIdForUpdate(command.settlementId())
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + command.settlementId()));

        storeAccessEnforcer.enforce(settlement.getStoreId());

        if (!"SETTLED".equals(settlement.getFinalStatus())) {
            throw new IllegalStateException("Can only refund SETTLED orders. Current: " + settlement.getFinalStatus());
        }

        long alreadyRefunded = settlement.getRefundedAmountCents();
        long maxRefundable = settlement.getCollectedAmountCents() - alreadyRefunded;

        String refundType = command.refundType() == null ? "FULL" : command.refundType().toUpperCase();
        long refundAmount;

        if ("FULL".equals(refundType)) {
            refundAmount = maxRefundable;
        } else {
            refundAmount = command.refundAmountCents();
        }

        if (refundAmount <= 0) {
            throw new IllegalStateException("Nothing to refund. Already fully refunded.");
        }

        if (refundAmount > maxRefundable) {
            throw new IllegalStateException(
                    "Refund amount " + refundAmount + " exceeds refundable " + maxRefundable);
        }

        // Approval threshold from RBAC-resolved maxRefundCents (0 = unlimited)
        long roleThreshold = actor.maxRefundCents();
        boolean needsApproval = roleThreshold > 0 && refundAmount > roleThreshold;

        // Calculate reversal amounts based on REMAINING unreversed totals (not original)
        // This prevents cumulative drift across multiple partial refunds
        List<RefundRecordEntity> priorRefunds = refundRecordRepository.findBySettlementId(settlement.getId());
        long alreadyReversedPoints = priorRefunds.stream()
                .filter(r -> "COMPLETED".equals(r.getRefundStatus()) || "AWAITING_EXTERNAL_REFUND".equals(r.getRefundStatus()))
                .mapToLong(RefundRecordEntity::getPointsReversedCents).sum();
        long alreadyReversedCash = priorRefunds.stream()
                .filter(r -> "COMPLETED".equals(r.getRefundStatus()) || "AWAITING_EXTERNAL_REFUND".equals(r.getRefundStatus()))
                .mapToLong(RefundRecordEntity::getCashReversedCents).sum();
        boolean alreadyReversedCoupon = priorRefunds.stream()
                .filter(r -> "COMPLETED".equals(r.getRefundStatus()) || "AWAITING_EXTERNAL_REFUND".equals(r.getRefundStatus()))
                .anyMatch(RefundRecordEntity::isCouponReversed);

        long remainingPoints = settlement.getPointsDeductCents() - alreadyReversedPoints;
        long remainingCash = settlement.getCashBalanceDeductCents() - alreadyReversedCash;

        long pointsReversed;
        long cashReversed;
        boolean couponReversed;

        if ("FULL".equals(refundType)) {
            // Full refund: reverse whatever remains
            pointsReversed = Math.max(0, remainingPoints);
            cashReversed = Math.max(0, remainingCash);
            couponReversed = !alreadyReversedCoupon && settlement.getCouponDiscountCents() > 0;
        } else {
            // Partial: proportional to this refund's share, capped by remaining
            double refundRatio = (double) refundAmount / settlement.getCollectedAmountCents();
            pointsReversed = Math.min(
                    Math.round(settlement.getPointsDeductCents() * refundRatio),
                    Math.max(0, remainingPoints));
            cashReversed = Math.min(
                    Math.round(settlement.getCashBalanceDeductCents() * refundRatio),
                    Math.max(0, remainingCash));
            couponReversed = false; // partial refunds do not restore coupons
        }

        boolean hasExternalPayment = settlement.getExternalPaymentCents() > 0;
        String externalRefundStatus = hasExternalPayment ? "PENDING" : null;

        RefundRecordEntity refund = new RefundRecordEntity();
        refund.setRefundNo("REF" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        refund.setSettlementId(settlement.getId());
        refund.setSettlementNo(settlement.getSettlementNo());
        refund.setMerchantId(settlement.getMerchantId());
        refund.setStoreId(settlement.getStoreId());
        refund.setRefundAmountCents(refundAmount);
        refund.setRefundType(refundType);
        refund.setRefundReason(command.reason());
        refund.setPaymentMethod(settlement.getPaymentMethod());
        refund.setOperatedBy(actor.userId());
        refund.setPointsReversedCents(pointsReversed);
        refund.setCashReversedCents(cashReversed);
        refund.setCouponReversed(couponReversed);
        refund.setExternalRefundStatus(externalRefundStatus);

        if (needsApproval) {
            refund.setRefundStatus("PENDING");
            refund.setApprovalStatus("PENDING_APPROVAL");
        } else {
            refund.setApprovalStatus("AUTO_APPROVED");
            applyRefundToSettlement(settlement, refundAmount);
            reverseAssets(settlement, pointsReversed, cashReversed, couponReversed);
            // Final status depends on whether external refund is still pending
            refund.setRefundStatus(hasExternalPayment ? "AWAITING_EXTERNAL_REFUND" : "COMPLETED");
        }

        refundRecordRepository.save(refund);

        // Save line items — amounts come from client (validated to sum to refundAmount)
        List<RefundLineItemEntity> savedLineItems = new ArrayList<>();
        if (command.refundItems() != null && !command.refundItems().isEmpty()) {
            long itemTotal = command.refundItems().stream()
                    .mapToLong(CreateRefundCommand.RefundItemCommand::amountCents).sum();
            if (itemTotal != refundAmount) {
                throw new IllegalArgumentException(
                        "Refund item amounts sum (" + itemTotal + ") does not match refundAmountCents (" + refundAmount + ")");
            }
            List<RefundLineItemEntity> lineItems = command.refundItems().stream().map(item -> {
                RefundLineItemEntity li = new RefundLineItemEntity();
                li.setRefundId(refund.getId());
                li.setOrderItemId(item.itemId());
                li.setQuantity(item.quantity());
                li.setRefundAmountCents(item.amountCents());
                return li;
            }).toList();
            savedLineItems = refundLineItemRepository.saveAll(lineItems);
        }

        return toDto(refund, savedLineItems);
    }

    @Transactional
    public RefundRecordDto approveRefund(ApproveRefundCommand command) {
        AuthenticatedActor actor = AuthContext.current();

        RefundRecordEntity refund = refundRecordRepository.findByRefundNoForUpdate(command.refundNo())
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + command.refundNo()));

        storeAccessEnforcer.enforce(refund.getStoreId());

        if (!"PENDING_APPROVAL".equals(refund.getApprovalStatus())) {
            throw new IllegalStateException("Refund is not pending approval. Current: " + refund.getApprovalStatus());
        }

        refund.setApprovedBy(actor.userId());

        if (command.approved()) {
            refund.setApprovalStatus("APPROVED");

            SettlementRecordEntity settlement = settlementRecordRepository.findByIdForUpdate(refund.getSettlementId())
                    .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + refund.getSettlementId()));
            applyRefundToSettlement(settlement, refund.getRefundAmountCents());
            reverseAssets(settlement, refund.getPointsReversedCents(),
                    refund.getCashReversedCents(), refund.isCouponReversed());

            boolean hasExternalPayment = settlement.getExternalPaymentCents() > 0;
            refund.setRefundStatus(hasExternalPayment ? "AWAITING_EXTERNAL_REFUND" : "COMPLETED");
        } else {
            refund.setApprovalStatus("REJECTED");
            refund.setRefundStatus("REJECTED");
        }

        refundRecordRepository.save(refund);
        List<RefundLineItemEntity> lineItems = refundLineItemRepository.findByRefundId(refund.getId());
        return toDto(refund, lineItems);
    }

    @Transactional(readOnly = true)
    public RefundRecordDto getRefund(String refundNo) {
        RefundRecordEntity refund = refundRecordRepository.findByRefundNo(refundNo)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + refundNo));
        storeAccessEnforcer.enforce(refund.getStoreId());
        List<RefundLineItemEntity> lineItems = refundLineItemRepository.findByRefundId(refund.getId());
        return toDto(refund, lineItems);
    }

    @Transactional(readOnly = true)
    public List<RefundRecordDto> getRefundsBySettlement(Long settlementId) {
        List<RefundRecordEntity> refunds = refundRecordRepository.findBySettlementId(settlementId);
        if (!refunds.isEmpty()) {
            storeAccessEnforcer.enforce(refunds.get(0).getStoreId());
        }
        return refunds.stream()
                .map(r -> toDto(r, refundLineItemRepository.findByRefundId(r.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<RefundRecordDto> listRefunds(Long storeId, int page, int size) {
        storeAccessEnforcer.enforce(storeId);
        return refundRecordRepository.findByStoreIdOrderByCreatedAtDesc(storeId, PageRequest.of(page, size))
                .map(r -> toDto(r, refundLineItemRepository.findByRefundId(r.getId())));
    }

    private void applyRefundToSettlement(SettlementRecordEntity settlement, long refundAmount) {
        long newRefunded = settlement.getRefundedAmountCents() + refundAmount;
        settlement.setRefundedAmountCents(newRefunded);
        if (newRefunded >= settlement.getCollectedAmountCents()) {
            settlement.setRefundStatus("FULLY_REFUNDED");
        } else {
            settlement.setRefundStatus("PARTIALLY_REFUNDED");
        }
        settlementRecordRepository.save(settlement);
    }

    /**
     * Actually reverse member assets: credit back points and cash balance,
     * restore coupon to AVAILABLE. External payment refund is deferred (status PENDING).
     */
    private void reverseAssets(SettlementRecordEntity settlement,
                               long pointsReversedCents, long cashReversedCents, boolean couponReversed) {
        List<SettlementPaymentHoldEntity> confirmedHolds =
                paymentHoldRepository.findAllBySettlementRecordIdAndHoldStatus(settlement.getId(), "CONFIRMED");

        Long memberId = confirmedHolds.stream()
                .filter(h -> h.getMemberId() != null)
                .map(SettlementPaymentHoldEntity::getMemberId)
                .findFirst()
                .orElse(null);

        if (memberId != null && (pointsReversedCents > 0 || cashReversedCents > 0)) {
            // Convert points-cents to points-count proportionally
            long totalPointsCents = confirmedHolds.stream()
                    .filter(h -> "POINTS".equals(h.getHoldType()))
                    .mapToLong(SettlementPaymentHoldEntity::getHoldAmountCents)
                    .sum();
            long totalPointsCount = confirmedHolds.stream()
                    .filter(h -> "POINTS".equals(h.getHoldType()) && h.getPointsHeld() != null)
                    .mapToLong(SettlementPaymentHoldEntity::getPointsHeld)
                    .sum();

            long pointsToReverse = 0;
            if (pointsReversedCents > 0 && totalPointsCents > 0 && totalPointsCount > 0) {
                pointsToReverse = Math.round((double) pointsReversedCents / totalPointsCents * totalPointsCount);
            }

            final long finalPointsToReverse = pointsToReverse;
            memberAccountRepository.findByMemberId(memberId).ifPresent(account -> {
                if (finalPointsToReverse > 0) {
                    account.setAvailablePoints(account.getAvailablePoints() + finalPointsToReverse);
                    account.setPointsBalance(account.getPointsBalance() + finalPointsToReverse);
                }
                if (cashReversedCents > 0) {
                    account.setAvailableCashCents(account.getAvailableCashCents() + cashReversedCents);
                    account.setCashBalanceCents(account.getCashBalanceCents() + cashReversedCents);
                }
                memberAccountRepository.save(account);
            });
        }

        if (couponReversed && settlement.getCouponId() != null) {
            memberCouponRepository.findById(settlement.getCouponId()).ifPresent(coupon -> {
                coupon.setCouponStatus("AVAILABLE");
                coupon.setUsedAt(null);
                coupon.setUsedOrderId(null);
                coupon.setUsedStoreId(null);
                memberCouponRepository.save(coupon);
            });
        }
    }

    private RefundRecordDto toDto(RefundRecordEntity entity, List<RefundLineItemEntity> lineItems) {
        List<RefundRecordDto.RefundLineItemDto> itemDtos = lineItems == null ? List.of() :
                lineItems.stream().map(li -> new RefundRecordDto.RefundLineItemDto(
                        li.getId(), li.getOrderItemId(), li.getQuantity(), li.getRefundAmountCents()
                )).toList();

        return new RefundRecordDto(
                entity.getId(), entity.getRefundNo(), entity.getSettlementId(),
                entity.getSettlementNo(), entity.getMerchantId(), entity.getStoreId(),
                entity.getRefundAmountCents(), entity.getRefundType(), entity.getRefundReason(),
                entity.getRefundStatus(), entity.getApprovalStatus(), entity.getPaymentMethod(),
                entity.getOperatedBy(), entity.getApprovedBy(), entity.getPointsReversedCents(),
                entity.getCashReversedCents(), entity.isCouponReversed(),
                entity.getExternalRefundStatus(), itemDtos, entity.getCreatedAt()
        );
    }
}
