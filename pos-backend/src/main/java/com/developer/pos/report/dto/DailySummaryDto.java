package com.developer.pos.report.dto;

public record DailySummaryDto(
    String date,
    Long totalRevenueCents,
    Integer orderCount,
    Long refundAmountCents,
    Long cashAmountCents,
    Long sdkPayAmountCents
) {
}
