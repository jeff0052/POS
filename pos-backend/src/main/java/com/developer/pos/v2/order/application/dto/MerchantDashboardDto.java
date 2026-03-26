package com.developer.pos.v2.order.application.dto;

public record MerchantDashboardDto(
    long todaySalesCents,
    int todayOrderCount,
    int activeTableCount,
    int totalTableCount,
    long todayMemberDiscountCents,
    long todayPromotionDiscountCents
) {}
