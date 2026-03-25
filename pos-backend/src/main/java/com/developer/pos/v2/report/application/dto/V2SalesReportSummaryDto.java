package com.developer.pos.v2.report.application.dto;

public record V2SalesReportSummaryDto(
        long totalSalesCents,
        long totalDiscountCents,
        long memberSalesCents,
        long rechargeSalesCents,
        double tableTurnoverRate,
        long pendingGtoBatches
) {
}
