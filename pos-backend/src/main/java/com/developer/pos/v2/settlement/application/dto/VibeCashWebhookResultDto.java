package com.developer.pos.v2.settlement.application.dto;

public record VibeCashWebhookResultDto(
        String eventType,
        String paymentAttemptId,
        String attemptStatus,
        boolean settlementTriggered
) {
}
