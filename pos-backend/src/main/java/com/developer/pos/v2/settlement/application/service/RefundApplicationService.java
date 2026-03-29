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
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaRefundLineItemRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaRefundRecordRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
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
    private final StoreAccessEnforcer storeAccessEnforcer;

    public RefundApplicationService(
            JpaRefundRecordRepository refundRecordRepository,
            JpaSettlementRecordRepository settlementRecordRepository,
            JpaRefundLineItemRepository refundLineItemRepository,
            StoreAccessEnforcer storeAccessEnforcer
    ) {
        this.refundRecordRepository = refundRecordRepository;
        this.settlementRecordRepository = settlementRecordRepository;
        this.refundLineItemRepository = refundLineItemRepository;
        this.storeAccessEnforcer = storeAccessEnforcer;
    }

    @Transactional
    public RefundRecordDto createRefund(CreateRefundCommand command) {
        AuthenticatedActor actor = AuthContext.current();

        SettlementRecordEntity settlement = settlementRecordRepository.findByIdForUpdate(command.settlementId())
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + command.settlementId()));

        // Enforce store access — caller must belong to the settlement's store
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

        boolean needsApproval = command.maxRefundCents() > 0 && refundAmount > command.maxRefundCents();

        // Calculate reversal amounts proportionally
        double refundRatio = "FULL".equals(refundType) ? 1.0 : (double) refundAmount / settlement.getCollectedAmountCents();
        long pointsReversed = "FULL".equals(refundType) ? settlement.getPointsDeductCents()
                : Math.round(settlement.getPointsDeductCents() * refundRatio);
        long cashReversed = "FULL".equals(refundType) ? settlement.getCashBalanceDeductCents()
                : Math.round(settlement.getCashBalanceDeductCents() * refundRatio);
        boolean couponReversed = "FULL".equals(refundType) && settlement.getCouponDiscountCents() > 0;
        String externalRefundStatus = settlement.getExternalPaymentCents() > 0 ? "PENDING" : null;

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
            refund.setRefundStatus("COMPLETED");
            refund.setApprovalStatus("AUTO_APPROVED");
            applyRefundToSettlement(settlement, refundAmount);
        }

        refundRecordRepository.save(refund);

        // Save line items if provided
        List<RefundLineItemEntity> savedLineItems = new ArrayList<>();
        if (command.refundItems() != null && !command.refundItems().isEmpty()) {
            List<RefundLineItemEntity> lineItems = command.refundItems().stream().map(item -> {
                RefundLineItemEntity li = new RefundLineItemEntity();
                li.setRefundId(refund.getId());
                li.setOrderItemId(item.itemId());
                li.setQuantity(item.quantity());
                li.setRefundAmountCents(0); // amount per item not calculated here
                return li;
            }).toList();
            savedLineItems = refundLineItemRepository.saveAll(lineItems);
        }

        return toDto(refund, savedLineItems);
    }

    @Transactional
    public RefundRecordDto approveRefund(ApproveRefundCommand command) {
        AuthenticatedActor actor = AuthContext.current();

        // Pessimistic lock prevents concurrent double-approval
        RefundRecordEntity refund = refundRecordRepository.findByRefundNoForUpdate(command.refundNo())
                .orElseThrow(() -> new IllegalArgumentException("Refund not found: " + command.refundNo()));

        // Enforce store access — approver must belong to the refund's store
        storeAccessEnforcer.enforce(refund.getStoreId());

        if (!"PENDING_APPROVAL".equals(refund.getApprovalStatus())) {
            throw new IllegalStateException("Refund is not pending approval. Current: " + refund.getApprovalStatus());
        }

        refund.setApprovedBy(actor.userId());

        if (command.approved()) {
            refund.setApprovalStatus("APPROVED");
            refund.setRefundStatus("COMPLETED");

            SettlementRecordEntity settlement = settlementRecordRepository.findByIdForUpdate(refund.getSettlementId())
                    .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + refund.getSettlementId()));
            applyRefundToSettlement(settlement, refund.getRefundAmountCents());
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
