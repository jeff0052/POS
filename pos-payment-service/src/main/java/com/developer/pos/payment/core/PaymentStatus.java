package com.developer.pos.payment.core;

public enum PaymentStatus {
    CREATED,
    PENDING,
    SUCCEEDED,
    FAILED,
    EXPIRED,
    CANCELLED,
    SETTLEMENT_FAILED,
    REFUND_PENDING,
    REFUNDED
}
