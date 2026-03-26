package com.developer.pos.payment.core;

public interface PaymentProviderAdapter {

    String providerCode();

    CreatePaymentResult createPayment(PaymentIntentEntity intent);

    QueryPaymentResult queryPayment(String providerTransactionId);

    RefundResult refund(String providerTransactionId, long amountCents, String reason);

    boolean supports(String paymentMethod, String paymentScheme);

    record CreatePaymentResult(
            boolean success,
            String providerTransactionId,
            String checkoutUrl,
            String providerStatus,
            String errorCode,
            String errorMessage
    ) {}

    record QueryPaymentResult(
            String providerTransactionId,
            String providerStatus,
            long confirmedAmountCents
    ) {}

    record RefundResult(
            boolean success,
            String providerRefundId,
            String errorCode,
            String errorMessage
    ) {}
}
