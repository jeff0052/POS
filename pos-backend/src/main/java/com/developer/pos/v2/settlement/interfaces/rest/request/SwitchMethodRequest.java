package com.developer.pos.v2.settlement.interfaces.rest.request;

public record SwitchMethodRequest(
    String paymentAttemptId,
    String newPaymentScheme
) {}
