package com.developer.pos.v2.settlement.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

public record StartVibeCashPaymentRequest(
        @NotBlank String paymentScheme
) {
}
