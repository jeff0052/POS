package com.developer.pos.v2.report.application.dto;

public record V2DailySummaryDto(
        long totalRevenueCents,
        long orderCount,
        long refundAmountCents,
        long cashAmountCents,
        long sdkPayAmountCents
) {
}
