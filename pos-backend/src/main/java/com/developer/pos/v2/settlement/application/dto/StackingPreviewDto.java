package com.developer.pos.v2.settlement.application.dto;

import java.util.List;

public record StackingPreviewDto(
    long totalPayableCents,
    Long pointsDeductCents,
    Long pointsToDeduct,
    Long couponDiscountCents,
    List<AvailableCouponItem> availableCoupons,
    Long cashBalanceDeductCents,
    long externalPaymentCents,
    String suggestedExternalMethod
) {
    public record AvailableCouponItem(Long couponId, String couponNo, long discountCents, int lockVersion) {}
}
