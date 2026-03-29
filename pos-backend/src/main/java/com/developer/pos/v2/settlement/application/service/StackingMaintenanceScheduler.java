package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberCouponRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@Component
public class StackingMaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(StackingMaintenanceScheduler.class);
    private static final int COUPON_LOCK_EXPIRY_MINUTES = 10;

    private final CouponLockingService couponLockingService;
    private final PaymentStackingService stackingService;
    private final JpaMemberCouponRepository couponRepo;
    private final JpaSettlementRecordRepository settlementRepo;

    public StackingMaintenanceScheduler(CouponLockingService couponLockingService,
                                        PaymentStackingService stackingService,
                                        JpaMemberCouponRepository couponRepo,
                                        JpaSettlementRecordRepository settlementRepo) {
        this.couponLockingService = couponLockingService;
        this.stackingService = stackingService;
        this.couponRepo = couponRepo;
        this.settlementRepo = settlementRepo;
    }

    /**
     * Reclaims expired coupon locks. For each expired lock:
     * - If the coupon is linked to a stacking session with a PENDING settlement:
     *   call releaseStacking() to release ALL holds (points, cash, coupon) atomically.
     * - Otherwise (orphan lock with no session or no PENDING settlement):
     *   directly release only the coupon lock.
     */
    @Scheduled(fixedDelay = 60_000)
    public void reclaimExpiredCouponLocks() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(COUPON_LOCK_EXPIRY_MINUTES);
        var expired = couponRepo.findAllByCouponStatusAndLockedAtBefore("LOCKED", cutoff);
        if (expired.isEmpty()) return;

        // Group by lockedBySession (null session = orphan lock)
        var bySession = expired.stream()
                .collect(Collectors.groupingBy(c ->
                        c.getLockedBySession() != null ? c.getLockedBySession() : -c.getId()));

        bySession.forEach((sessionKey, coupons) -> {
            var coupon = coupons.get(0);
            Long sessionId = coupon.getLockedBySession();
            try {
                if (sessionId != null) {
                    // Look for a PENDING settlement linked to this stacking session
                    var settlementOpt = settlementRepo.findByStackingSessionIdAndFinalStatus(sessionId, "PENDING");
                    if (settlementOpt.isPresent()) {
                        var settlement = settlementOpt.get();
                        // releaseStacking handles: hold release, balance unfreeze, coupon release, status → CANCELLED
                        stackingService.releaseStacking(settlement.getStoreId(), settlement.getId(), "COUPON_LOCK_TIMEOUT");
                        log.info("Released stacking settlement {} due to expired coupon lock on session {}",
                                settlement.getId(), sessionId);
                        return; // coupon released inside releaseStacking; skip direct coupon release below
                    }
                }
                // Orphan lock: no session or no PENDING settlement — release coupon directly
                couponLockingService.releaseCoupon(coupon.getId(), sessionId);
                log.info("Released orphan coupon lock: couponId={}, sessionId={}", coupon.getId(), sessionId);
            } catch (Exception e) {
                log.error("CRITICAL: reclaimExpiredCouponLocks failed for couponId={}, sessionId={}: {}",
                        coupon.getId(), sessionId, e.getMessage());
            }
        });
    }

    @Scheduled(fixedDelay = 60_000)
    public void reclaimPendingSettlements() {
        stackingService.reclaimPendingSettlements();
    }
}
