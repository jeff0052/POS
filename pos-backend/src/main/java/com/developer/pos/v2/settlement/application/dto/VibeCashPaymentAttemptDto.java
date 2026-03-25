package com.developer.pos.v2.settlement.application.dto;

public record VibeCashPaymentAttemptDto(
        String paymentAttemptId,
        String provider,
        String paymentMethod,
        String paymentScheme,
        String attemptStatus,
        String providerStatus,
        String providerPaymentId,
        String checkoutUrl,
        long settlementAmountCents,
        String currencyCode
) {
}
