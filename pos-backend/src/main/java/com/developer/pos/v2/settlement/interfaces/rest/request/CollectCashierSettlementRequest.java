package com.developer.pos.v2.settlement.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CollectCashierSettlementRequest(
        @NotNull Long cashierId,
        @NotBlank String paymentMethod,
        @Positive long collectedAmountCents
) {
}
