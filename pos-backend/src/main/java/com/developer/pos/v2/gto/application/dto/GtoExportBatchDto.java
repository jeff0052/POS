package com.developer.pos.v2.gto.application.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GtoExportBatchDto(
        String batchId,
        Long merchantId,
        Long storeId,
        LocalDate exportDate,
        String batchStatus,
        long totalSalesCents,
        long totalTaxCents,
        int totalTransactionCount,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        List<GtoExportItemDto> items
) {

    public record GtoExportItemDto(
            String paymentMethod,
            String paymentScheme,
            int saleCount,
            long saleTotalCents,
            int refundCount,
            long refundTotalCents,
            long netTotalCents,
            long taxCents
    ) {
    }
}
