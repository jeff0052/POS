package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaCouponTemplateRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberCouponRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.settlement.application.dto.StackingCollectResultDto;
import com.developer.pos.v2.settlement.application.dto.StackingPreviewDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementPaymentHoldEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.SettlementRecordEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaPaymentAttemptRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementPaymentHoldRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class PaymentStackingService {

    private static final Logger log = LoggerFactory.getLogger(PaymentStackingService.class);
    private static final Set<String> VALID_EXTERNAL_SCHEMES = Set.of("WECHAT_QR", "ALIPAY_QR", "PAYNOW_QR");

    public record StackingChoices(
        boolean usePoints,
        Long couponId,
        Integer couponLockVersion,
        boolean useCashBalance,
        String externalPaymentMethod
    ) {}

    private final JpaStoreTableRepository storeTableRepo;
    private final JpaTableSessionRepository sessionRepo;
    private final JpaSubmittedOrderRepository submittedOrderRepo;
    private final JpaSettlementRecordRepository settlementRepo;
    private final JpaSettlementPaymentHoldRepository holdRepo;
    private final JpaPaymentAttemptRepository attemptRepo;
    private final JpaMemberAccountRepository memberAccountRepo;
    private final JpaMemberCouponRepository couponRepo;
    private final JpaCouponTemplateRepository couponTemplateRepo;
    private final CouponLockingService couponLockingService;
    private final TableSettlementFinalizer finalizer;

    public PaymentStackingService(
            JpaStoreTableRepository storeTableRepo,
            JpaTableSessionRepository sessionRepo,
            JpaSubmittedOrderRepository submittedOrderRepo,
            JpaSettlementRecordRepository settlementRepo,
            JpaSettlementPaymentHoldRepository holdRepo,
            JpaPaymentAttemptRepository attemptRepo,
            JpaMemberAccountRepository memberAccountRepo,
            JpaMemberCouponRepository couponRepo,
            JpaCouponTemplateRepository couponTemplateRepo,
            CouponLockingService couponLockingService,
            TableSettlementFinalizer finalizer) {
        this.storeTableRepo = storeTableRepo;
        this.sessionRepo = sessionRepo;
        this.submittedOrderRepo = submittedOrderRepo;
        this.settlementRepo = settlementRepo;
        this.holdRepo = holdRepo;
        this.attemptRepo = attemptRepo;
        this.memberAccountRepo = memberAccountRepo;
        this.couponRepo = couponRepo;
        this.couponTemplateRepo = couponTemplateRepo;
        this.couponLockingService = couponLockingService;
        this.finalizer = finalizer;
    }

    @Transactional(readOnly = true)
    public StackingPreviewDto previewStacking(Long storeId, Long tableId) {
        var table = storeTableRepo.findByStoreIdAndId(storeId, tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        if (table.getMergedIntoTableId() != null) {
            throw new IllegalStateException("Merged child table: settlement must go through master table");
        }
        TableSessionEntity masterSession = sessionRepo.findActiveByTableId(tableId)
                .orElseThrow(() -> new IllegalStateException("No active session for table: " + tableId));

        List<Long> sessionChainIds = buildSessionChain(masterSession);
        List<SubmittedOrderEntity> orders = submittedOrderRepo.findAllByTableSessionIdIn(sessionChainIds);
        // payableAmountCents is the correct field name in SubmittedOrderEntity
        long total = orders.stream().mapToLong(SubmittedOrderEntity::getPayableAmountCents).sum();

        Long memberId = extractSingleMemberId(orders);
        long remaining = total;

        Long pointsDeductCents = null;
        Long pointsToDeduct = null;
        Long couponDiscountCents = null;
        List<StackingPreviewDto.AvailableCouponItem> availableCoupons = Collections.emptyList();
        Long cashBalanceDeductCents = null;

        if (memberId != null) {
            var account = memberAccountRepo.findByMemberId(memberId).orElse(null);
            if (account != null) {
                long availPoints = account.getPointsBalance() - account.getFrozenPoints();
                if (availPoints > 0) {
                    pointsToDeduct = Math.min(availPoints, remaining);
                    pointsDeductCents = pointsToDeduct;
                    remaining -= pointsDeductCents;
                }
                var coupons = couponRepo.findAllByMemberIdAndCouponStatus(memberId, "AVAILABLE");
                OffsetDateTime previewNow = OffsetDateTime.now();
                long finalRemaining = remaining;
                availableCoupons = coupons.stream()
                        // Mirror collectStacking validity window exactly: validFrom <= now < validUntil
                        .filter(c -> !previewNow.isBefore(c.getValidFrom()) && c.getValidUntil().isAfter(previewNow))
                        .map(c -> {
                            long discount = calculateCouponDiscount(c.getTemplateId(), finalRemaining);
                            return new StackingPreviewDto.AvailableCouponItem(
                                    c.getId(), c.getCouponNo(), discount, c.getLockVersion());
                        })
                        // Exclude coupons whose effective discount is 0 (minSpendCents not met, etc.)
                        .filter(item -> item.discountCents() > 0)
                        .toList();
                long availCash = account.getCashBalanceCents() - account.getFrozenCashCents();
                if (availCash > 0 && remaining > 0) {
                    cashBalanceDeductCents = Math.min(availCash, remaining);
                    remaining -= cashBalanceDeductCents;
                }
            }
        }

        return new StackingPreviewDto(total, pointsDeductCents, pointsToDeduct,
                couponDiscountCents, availableCoupons, cashBalanceDeductCents,
                remaining, "ALIPAY_QR");
    }

    @Transactional
    public StackingCollectResultDto collectStacking(Long storeId, Long tableId, StackingChoices choices) {
        // Validate external payment scheme before any DB writes — prevents stranding a
        // committed settlement/holds with an un-buildable provider link
        if (choices.externalPaymentMethod() != null
                && !VALID_EXTERNAL_SCHEMES.contains(choices.externalPaymentMethod())) {
            throw new IllegalArgumentException("Unsupported external payment scheme: "
                    + choices.externalPaymentMethod() + ". Valid values: " + VALID_EXTERNAL_SCHEMES);
        }

        var table = storeTableRepo.findByStoreIdAndId(storeId, tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));
        if (table.getMergedIntoTableId() != null) {
            throw new IllegalStateException("Merged child table: settlement must go through master table");
        }

        TableSessionEntity masterSession = sessionRepo.findActiveByTableIdForUpdate(tableId)
                .orElseThrow(() -> new IllegalStateException("No active session for table: " + tableId));

        var existing = settlementRepo.findByStackingSessionIdAndFinalStatusForUpdate(masterSession.getId(), "PENDING");
        if (existing.isPresent()) {
            var s = existing.get();
            var allAttempts = attemptRepo.findBySettlementRecordIdOrderByCreatedAtDesc(s.getId());
            // Guard: if any attempt is already SUCCEEDED, payment has been captured.
            // Creating a new checkout link would risk a duplicate charge. Reject and let the
            // settlement-timeout scheduler (or explicit confirm-stacking) finalise the settlement.
            boolean alreadyPaid = allAttempts.stream()
                    .anyMatch(a -> "SUCCEEDED".equals(a.getAttemptStatus()));
            if (alreadyPaid) {
                throw new IllegalStateException(
                        "Settlement " + s.getSettlementNo()
                        + " already has a SUCCEEDED payment; confirmation is pending."
                        + " Call confirm-stacking or wait for the scheduler.");
            }
            var holds = holdRepo.findAllBySettlementRecordIdAndHoldStatus(s.getId(), "HELD");
            // For idempotent response: re-surface existing PENDING_CUSTOMER checkout URL if present
            String existingUrl = allAttempts.stream()
                    .filter(a -> "PENDING_CUSTOMER".equals(a.getAttemptStatus()))
                    .map(a -> a.getProviderCheckoutUrl())
                    .findFirst()
                    .orElse(null);
            return new StackingCollectResultDto(s.getId(), s.getSettlementNo(),
                    holds.stream().map(SettlementPaymentHoldEntity::getId).toList(),
                    existingUrl, s.getExternalPaymentCents());
        }

        List<Long> sessionChainIds = buildSessionChain(masterSession);
        List<SubmittedOrderEntity> orders = submittedOrderRepo.findAllByTableSessionIdIn(sessionChainIds);
        long total = orders.stream().mapToLong(SubmittedOrderEntity::getPayableAmountCents).sum();
        Long memberId = extractSingleMemberId(orders);

        SettlementRecordEntity settlement = new SettlementRecordEntity();
        settlement.setSettlementNo("STK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        settlement.setActiveOrderId("STACK-" + UUID.randomUUID());
        settlement.setStackingSessionId(masterSession.getId());
        settlement.setFinalStatus("PENDING");
        settlement.setStoreId(storeId);
        settlement.setMerchantId(masterSession.getMerchantId());
        settlement.setTableId(tableId);
        settlement.setPayableAmountCents(total);
        settlement.setCollectedAmountCents(0L);
        settlement.setRefundedAmountCents(0L);
        settlement.setPaymentMethod(choices.externalPaymentMethod() != null ? choices.externalPaymentMethod() : "MIXED");
        try { settlement.setCashierId(AuthContext.current().userId()); }
        catch (Exception e) {
            log.warn("Could not resolve cashier ID from AuthContext: {}", e.getMessage());
            settlement.setCashierId(null);
        }
        settlement = settlementRepo.save(settlement);

        long remaining = total;
        List<Long> holdIds = new ArrayList<>();
        int stepOrder = 0;

        if (choices.usePoints() && memberId != null) {
            var account = memberAccountRepo.findByMemberId(memberId).orElse(null);
            if (account != null) {
                long availPoints = account.getPointsBalance() - account.getFrozenPoints();
                long toFreeze = Math.min(availPoints, remaining);
                if (toFreeze > 0) {
                    int affected = memberAccountRepo.freezePoints(memberId, toFreeze);
                    if (affected == 0) throw new IllegalStateException("Insufficient points balance");
                    var hold = buildHold(settlement.getId(), masterSession.getId(), storeId, memberId,
                            "POINTS", toFreeze, ++stepOrder);
                    hold.setPointsHeld(toFreeze);
                    hold = holdRepo.save(hold);
                    holdIds.add(hold.getId());
                    settlement.setPointsDeductCents(toFreeze);
                    settlement.setPointsDeducted(toFreeze);
                    remaining -= toFreeze;
                }
            }
        }

        if (choices.couponId() != null && choices.couponLockVersion() != null && memberId != null) {
            // Validate before locking: ownership, then discount value
            var coupon = couponRepo.findById(choices.couponId())
                    .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + choices.couponId()));
            if (!coupon.getMemberId().equals(memberId)) {
                throw new IllegalArgumentException(
                        "Coupon " + choices.couponId() + " does not belong to member " + memberId);
            }
            OffsetDateTime now = OffsetDateTime.now();
            if (now.isBefore(coupon.getValidFrom())) {
                throw new IllegalArgumentException(
                        "Coupon " + choices.couponId() + " is not yet valid");
            }
            if (!now.isBefore(coupon.getValidUntil())) {
                throw new IllegalArgumentException(
                        "Coupon " + choices.couponId() + " has expired");
            }
            long couponDiscount = calculateCouponDiscount(coupon.getTemplateId(), remaining);
            if (couponDiscount == 0L) {
                throw new IllegalArgumentException(
                        "Coupon " + choices.couponId() + " does not apply: zero effective discount (minimum spend not met or invalid template)");
            }
            // Lock only after validation passes
            couponLockingService.lockCoupon(choices.couponId(), choices.couponLockVersion(), masterSession.getId());
            var hold = buildHold(settlement.getId(), masterSession.getId(), storeId, memberId,
                    "COUPON", couponDiscount, ++stepOrder);
            hold.setCouponId(choices.couponId());
            hold = holdRepo.save(hold);
            holdIds.add(hold.getId());
            settlement.setCouponId(choices.couponId());
            settlement.setCouponDiscountCents(couponDiscount);
            remaining -= couponDiscount;
        }

        if (choices.useCashBalance() && memberId != null && remaining > 0) {
            var account = memberAccountRepo.findByMemberId(memberId).orElse(null);
            if (account != null) {
                long availCash = account.getCashBalanceCents() - account.getFrozenCashCents();
                long toFreeze = Math.min(availCash, remaining);
                if (toFreeze > 0) {
                    int affected = memberAccountRepo.freezeCash(memberId, toFreeze);
                    if (affected == 0) throw new IllegalStateException("Insufficient cash balance");
                    var hold = buildHold(settlement.getId(), masterSession.getId(), storeId, memberId,
                            "CASH_BALANCE", toFreeze, ++stepOrder);
                    hold = holdRepo.save(hold);
                    holdIds.add(hold.getId());
                    settlement.setCashBalanceDeductCents(toFreeze);
                    remaining -= toFreeze;
                }
            }
        }

        settlement.setExternalPaymentCents(remaining);
        settlementRepo.save(settlement);

        if (remaining > 0) {
            var extHold = buildHold(settlement.getId(), masterSession.getId(), storeId, memberId,
                    "EXTERNAL", remaining, ++stepOrder);
            extHold = holdRepo.save(extHold);
            holdIds.add(extHold.getId());
        }

        if (remaining == 0) {
            confirmStacking(storeId, tableId, settlement.getId());
        }
        // VibeCash call intentionally excluded — controller calls it after this transaction commits

        return new StackingCollectResultDto(settlement.getId(), settlement.getSettlementNo(), holdIds, null, remaining);
    }

    private List<Long> buildSessionChain(TableSessionEntity masterSession) {
        List<Long> ids = new ArrayList<>();
        ids.add(masterSession.getId());
        List<TableSessionEntity> merged = sessionRepo.findAllByMergedIntoSessionId(masterSession.getId());
        merged.forEach(s -> ids.add(s.getId()));
        return ids;
    }

    private Long extractSingleMemberId(List<SubmittedOrderEntity> orders) {
        var memberIds = orders.stream()
                .map(SubmittedOrderEntity::getMemberId)
                .filter(Objects::nonNull)
                .distinct().toList();
        if (memberIds.size() > 1) throw new IllegalStateException("Mixed member IDs in session chain: " + memberIds);
        return memberIds.isEmpty() ? null : memberIds.get(0);
    }

    private SettlementPaymentHoldEntity buildHold(Long settlementId, Long sessionId,
            Long storeId, Long memberId, String holdType, long amountCents, int stepOrder) {
        var h = new SettlementPaymentHoldEntity();
        h.setHoldNo("H-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        h.setSettlementRecordId(settlementId);
        h.setTableSessionId(sessionId);
        h.setStoreId(storeId);
        h.setMemberId(memberId);
        h.setHoldType(holdType);
        h.setHoldAmountCents(amountCents);
        h.setHoldStatus("HELD");
        h.setStepOrder(stepOrder);
        h.setHeldAt(OffsetDateTime.now());
        return h;
    }

    @Transactional
    public void confirmStacking(Long storeId, Long tableId, Long settlementId) {
        SettlementRecordEntity settlement = settlementRepo.findByIdForUpdate(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + settlementId));
        if (!storeId.equals(settlement.getStoreId()) || !tableId.equals(settlement.getTableId())) {
            throw new IllegalArgumentException("Settlement " + settlementId
                    + " does not belong to store " + storeId + " / table " + tableId);
        }

        if ("SETTLED".equals(settlement.getFinalStatus())) return; // 幂等
        if ("CANCELLED".equals(settlement.getFinalStatus())) {
            throw new IllegalStateException("Cannot confirm a CANCELLED settlement: " + settlementId);
        }

        // If there are EXTERNAL holds, verify payment attempt SUCCEEDED
        var holds = holdRepo.findHeldBySettlementForUpdate(settlementId);
        boolean hasExternal = holds.stream().anyMatch(h -> "EXTERNAL".equals(h.getHoldType()));
        if (hasExternal) {
            var attempts = attemptRepo.findBySettlementRecordIdOrderByCreatedAtDesc(settlementId);
            boolean hasSucceeded = attempts.stream().anyMatch(a -> "SUCCEEDED".equals(a.getAttemptStatus()));
            if (!hasSucceeded) {
                throw new IllegalStateException("External payment not yet SUCCEEDED for settlement: " + settlementId);
            }
            attempts.stream()
                    .filter(a -> "SUCCEEDED".equals(a.getAttemptStatus()))
                    .forEach(a -> {
                        a.setAttemptStatus("SETTLED");
                        attemptRepo.save(a);
                    });
        }

        // Transition holds HELD → CONFIRMED
        holds.forEach(h -> {
            h.setHoldStatus("CONFIRMED");
            h.setConfirmedAt(OffsetDateTime.now());
        });
        holdRepo.saveAll(holds);

        // Deduct balances
        Long memberId = holds.stream().map(h -> h.getMemberId()).filter(Objects::nonNull).findFirst().orElse(null);
        if (memberId != null) {
            long pointsDeduct = settlement.getPointsDeducted();
            if (pointsDeduct > 0) memberAccountRepo.deductPoints(memberId, pointsDeduct);
            long cashDeduct = settlement.getCashBalanceDeductCents();
            if (cashDeduct > 0) memberAccountRepo.deductCash(memberId, cashDeduct);
        }

        // Confirm coupon — runs regardless of memberId (coupon may be non-member)
        if (settlement.getCouponId() != null) {
            Long sessionId = holds.stream()
                    .filter(h -> "COUPON".equals(h.getHoldType()))
                    .map(h -> h.getTableSessionId()).findFirst().orElse(null);
            couponLockingService.confirmCoupon(settlement.getCouponId(), sessionId,
                    settlement.getActiveOrderId(), settlement.getStoreId());
        }

        // Settlement → SETTLED
        settlement.setFinalStatus("SETTLED");
        settlementRepo.save(settlement);

        // Finalize table (close session, mark table PENDING_CLEAN, etc.)
        List<Long> sessionChainIds = buildSessionChainFromSettlement(settlement);
        finalizer.finalize(sessionChainIds);
    }

    @Transactional
    public void releaseStacking(Long storeId, Long tableId, Long settlementId, String reason) {
        SettlementRecordEntity settlement = settlementRepo.findByIdForUpdate(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + settlementId));
        if (!storeId.equals(settlement.getStoreId()) || !tableId.equals(settlement.getTableId())) {
            throw new IllegalArgumentException("Settlement " + settlementId
                    + " does not belong to store " + storeId + " / table " + tableId);
        }

        if ("CANCELLED".equals(settlement.getFinalStatus())) return; // 幂等
        if ("SETTLED".equals(settlement.getFinalStatus())) {
            throw new IllegalStateException("Cannot release a SETTLED settlement: " + settlementId);
        }

        // Lock holds early to prevent concurrent mutation during attempt checks
        var holds = holdRepo.findHeldBySettlementForUpdate(settlementId);

        // Check live attempts — block release if already charged
        var attempts = attemptRepo.findBySettlementRecordIdOrderByCreatedAtDesc(settlementId);
        for (var attempt : attempts) {
            if ("SUCCEEDED".equals(attempt.getAttemptStatus())) {
                throw new IllegalStateException("Cannot release: payment already charged for settlement " + settlementId);
            }
            if ("PENDING_CUSTOMER".equals(attempt.getAttemptStatus())) {
                int affected = attemptRepo.markReplacedCas(attempt.getPaymentAttemptId(), "PENDING_CUSTOMER");
                if (affected == 0) {
                    throw new IllegalStateException("Cannot release: payment race condition for settlement " + settlementId);
                }
            }
        }

        // Transition holds HELD → RELEASED
        holds.forEach(h -> {
            h.setHoldStatus("RELEASED");
            h.setReleasedAt(OffsetDateTime.now());
            h.setReleaseReason(reason);
        });
        holdRepo.saveAll(holds);

        // Unfreeze balances
        Long memberId = holds.stream().map(h -> h.getMemberId()).filter(Objects::nonNull).findFirst().orElse(null);
        if (memberId != null) {
            long pointsDeduct = settlement.getPointsDeducted();
            if (pointsDeduct > 0) memberAccountRepo.unfreezePoints(memberId, pointsDeduct);
            long cashDeduct = settlement.getCashBalanceDeductCents();
            if (cashDeduct > 0) memberAccountRepo.unfreezeCash(memberId, cashDeduct);
        }

        // Release coupon — runs regardless of memberId (coupon may be non-member)
        if (settlement.getCouponId() != null) {
            Long sessionId = holds.stream()
                    .filter(h -> "COUPON".equals(h.getHoldType()))
                    .map(h -> h.getTableSessionId()).findFirst().orElse(null);
            couponLockingService.releaseCoupon(settlement.getCouponId(), sessionId);
        }

        settlement.setFinalStatus("CANCELLED");
        settlementRepo.save(settlement);
    }

    private List<Long> buildSessionChainFromSettlement(SettlementRecordEntity settlement) {
        if (settlement.getStackingSessionId() == null) return Collections.emptyList();
        TableSessionEntity master = sessionRepo.findById(settlement.getStackingSessionId()).orElse(null);
        if (master == null) return Collections.emptyList();
        return buildSessionChain(master);
    }

    private long calculateCouponDiscount(Long templateId, long baseAmountCents) {
        if (templateId == null || baseAmountCents <= 0) return 0L;
        var template = couponTemplateRepo.findById(templateId).orElse(null);
        if (template == null) return 0L;
        // Enforce minimum spend threshold before applying any discount
        if (template.getMinSpendCents() != null && baseAmountCents < template.getMinSpendCents()) {
            return 0L;
        }
        return switch (template.getCouponType()) {
            case "FIXED" -> {
                long d = template.getDiscountAmountCents() != null ? template.getDiscountAmountCents() : 0L;
                yield Math.min(d, baseAmountCents);
            }
            case "PERCENT" -> {
                if (template.getDiscountPercent() == null) yield 0L;
                long d = baseAmountCents * template.getDiscountPercent() / 100L;
                if (template.getMaxDiscountCents() != null) {
                    d = Math.min(d, template.getMaxDiscountCents());
                }
                yield Math.min(d, baseAmountCents);
            }
            default -> 0L;
        };
    }

    // NO @Transactional here - each confirmStacking/releaseStacking runs its own transaction
    public void reclaimPendingSettlements() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(30);
        List<SettlementRecordEntity> pending = settlementRepo.findPendingOlderThan(cutoff);
        for (SettlementRecordEntity settlement : pending) {
            try {
                var attempts = attemptRepo.findBySettlementRecordIdOrderByCreatedAtDesc(settlement.getId());
                if (attempts.isEmpty()) {
                    releaseStacking(settlement.getStoreId(), settlement.getTableId(), settlement.getId(), "SETTLEMENT_TIMEOUT");
                    continue;
                }
                var latest = attempts.get(0);
                switch (latest.getAttemptStatus()) {
                    case "SUCCEEDED" -> confirmStacking(settlement.getStoreId(), settlement.getTableId(), settlement.getId()); // compensate lost webhook
                    case "PENDING_CUSTOMER" -> {
                        if (latest.getCreatedAt().isBefore(cutoff)) {
                            releaseStacking(settlement.getStoreId(), settlement.getTableId(), settlement.getId(), "ATTEMPT_EXPIRED_NO_WEBHOOK");
                        }
                        // else: young attempt, skip
                    }
                    default -> releaseStacking(settlement.getStoreId(), settlement.getTableId(), settlement.getId(), "SETTLEMENT_TIMEOUT");
                }
            } catch (Exception e) {  // broadened from IllegalStateException to catch JPA/persistence exceptions too
                log.error("CRITICAL: reclaimPendingSettlements failed for settlement {}: {}",
                        settlement.getId(), e.getMessage());
            }
        }
    }
}
