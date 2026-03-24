package com.developer.pos.v2.settlement.application.command;

public record CollectCashierSettlementCommand(
        String activeOrderId,
        Long cashierId,
        String paymentMethod,
        long collectedAmountCents
) {
}
