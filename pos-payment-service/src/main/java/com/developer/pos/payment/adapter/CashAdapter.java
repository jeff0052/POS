package com.developer.pos.payment.adapter;

import com.developer.pos.payment.core.PaymentIntentEntity;
import com.developer.pos.payment.core.PaymentProviderAdapter;
import org.springframework.stereotype.Component;

@Component
public class CashAdapter implements PaymentProviderAdapter {

    @Override
    public String providerCode() {
        return "CASH";
    }

    @Override
    public CreatePaymentResult createPayment(PaymentIntentEntity intent) {
        // Cash requires no external call — immediately succeeds
        return new CreatePaymentResult(
                true,
                "CASH-" + intent.getIntentId(),
                null,
                "completed",
                null,
                null
        );
    }

    @Override
    public QueryPaymentResult queryPayment(String providerTransactionId) {
        return new QueryPaymentResult(providerTransactionId, "completed", 0);
    }

    @Override
    public RefundResult refund(String providerTransactionId, long amountCents, String reason) {
        // Cash refund is manual — just mark as done
        return new RefundResult(true, "CASH-REFUND-" + System.currentTimeMillis(), null, null);
    }

    @Override
    public boolean supports(String paymentMethod, String paymentScheme) {
        return "CASH".equalsIgnoreCase(paymentMethod);
    }
}
