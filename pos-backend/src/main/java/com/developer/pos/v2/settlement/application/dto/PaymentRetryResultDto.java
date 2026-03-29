package com.developer.pos.v2.settlement.application.dto;

public record PaymentRetryResultDto(
    String newAttemptId,
    String checkoutUrl
) {}
