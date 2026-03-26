package com.developer.pos.v2.report.application.dto;

public record MemberConsumptionOverviewDto(
        long totalMemberSalesCents,
        long totalMemberDiscountCents,
        long memberOrderCount,
        long activeMemberCount,
        long totalRechargeCents,
        long totalBonusCents,
        long rechargeOrderCount,
        long averageRechargeCents
) {
}
