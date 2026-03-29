package com.developer.pos.v2.settlement.application.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StackingMaintenanceScheduler {

    private final CouponLockingService couponLockingService;
    private final PaymentStackingService stackingService;

    public StackingMaintenanceScheduler(CouponLockingService couponLockingService,
                                        PaymentStackingService stackingService) {
        this.couponLockingService = couponLockingService;
        this.stackingService = stackingService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void reclaimExpiredCouponLocks() {
        couponLockingService.reclaimExpiredLocks();
    }

    @Scheduled(fixedDelay = 60_000)
    public void reclaimPendingSettlements() {
        stackingService.reclaimPendingSettlements();
    }
}
