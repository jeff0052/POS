package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberCouponEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberCouponRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponLockingService {

    private final JpaMemberCouponRepository couponRepo;

    public CouponLockingService(JpaMemberCouponRepository couponRepo) {
        this.couponRepo = couponRepo;
    }

    /** CAS 锁券。affected_rows=0 → 已被锁或版本不对 → 抛异常 */
    @Transactional
    public void lockCoupon(Long couponId, int expectedVersion, Long sessionId) {
        int affected = couponRepo.lockCouponCas(couponId, expectedVersion, sessionId);
        if (affected == 0) {
            throw new IllegalStateException("Coupon already locked or version mismatch: couponId=" + couponId);
        }
    }

    /** 释放券。幂等：已 AVAILABLE 或不属于本 session → 无操作 */
    @Transactional
    public void releaseCoupon(Long couponId, Long sessionId) {
        couponRepo.releaseCouponCas(couponId, sessionId);
    }

    @Transactional
    public void reclaimExpiredLocks() {
        var expired = couponRepo.findAllByCouponStatusAndLockedAtBefore(
                "LOCKED", OffsetDateTime.now().minusMinutes(10));
        expired.stream()
                .collect(java.util.stream.Collectors.groupingBy(c ->
                        c.getLockedBySession() != null ? c.getLockedBySession() : -c.getId()))
                .values().forEach(group -> {
                    var coupon = group.get(0);
                    if (coupon.getLockedBySession() == null) {
                        couponRepo.releaseCouponCas(coupon.getId(), null);
                    } else {
                        couponRepo.releaseCouponCas(coupon.getId(), coupon.getLockedBySession());
                    }
                });
    }

    /**
     * 确认券已使用。
     * 幂等：已 USED 且 same order → 无操作。
     * 已 USED 且 different order → 抛冲突异常。
     */
    @Transactional
    public void confirmCoupon(Long couponId, Long sessionId, String usedOrderId, Long usedStoreId) {
        int affected = couponRepo.confirmCouponCas(couponId, sessionId, usedOrderId, usedStoreId);
        if (affected == 0) {
            MemberCouponEntity coupon = couponRepo.findById(couponId)
                    .orElseThrow(() -> new IllegalArgumentException("Coupon not found: " + couponId));
            if ("USED".equals(coupon.getCouponStatus())) {
                if (usedOrderId.equals(coupon.getUsedOrderId())) {
                    return; // 幂等
                }
                throw new IllegalStateException("Coupon already used for different order: couponId=" + couponId
                        + " usedOrderId=" + coupon.getUsedOrderId() + " (conflict with " + usedOrderId + ")");
            }
            throw new IllegalStateException("Coupon confirm failed: couponId=" + couponId);
        }
    }
}
