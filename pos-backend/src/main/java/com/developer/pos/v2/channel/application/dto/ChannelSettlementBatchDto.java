package com.developer.pos.v2.channel.application.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ChannelSettlementBatchDto(
        Long id,
        String batchNo,
        Long channelId,
        Long storeId,
        LocalDate periodStart,
        LocalDate periodEnd,
        int totalOrders,
        long totalOrderAmountCents,
        long totalCommissionCents,
        long adjustmentCents,
        String adjustmentReason,
        long finalSettlementCents,
        String batchStatus,
        OffsetDateTime confirmedAt,
        Long confirmedBy,
        OffsetDateTime paidAt,
        Long paidBy,
        String paymentRef,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
