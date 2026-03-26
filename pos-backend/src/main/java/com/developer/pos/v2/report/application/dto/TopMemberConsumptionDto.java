package com.developer.pos.v2.report.application.dto;

public record TopMemberConsumptionDto(
        Long memberId,
        String memberName,
        String tierCode,
        long orderCount,
        long totalSalesCents,
        long totalRechargeCents,
        long memberDiscountCents
) {
}
