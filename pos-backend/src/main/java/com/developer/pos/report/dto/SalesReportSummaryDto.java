package com.developer.pos.report.dto;

public record SalesReportSummaryDto(
    Long totalSalesCents,
    Long totalDiscountCents,
    Long memberSalesCents,
    Long rechargeSalesCents,
    Double tableTurnoverRate,
    Integer pendingGtoBatches
) {
}
