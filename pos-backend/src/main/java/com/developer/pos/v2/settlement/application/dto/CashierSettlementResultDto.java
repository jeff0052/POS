package com.developer.pos.v2.settlement.application.dto;

public record CashierSettlementResultDto(
        String activeOrderId,
        String settlementNo,
        String finalStatus,
        long payableAmountCents,
        long collectedAmountCents
) {
}
