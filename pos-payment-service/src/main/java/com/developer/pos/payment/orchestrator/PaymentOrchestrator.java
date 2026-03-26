package com.developer.pos.payment.orchestrator;

import com.developer.pos.payment.core.PaymentIntentEntity;
import com.developer.pos.payment.core.PaymentProviderAdapter;
import com.developer.pos.payment.core.PaymentProviderAdapter.CreatePaymentResult;
import com.developer.pos.payment.core.PaymentStatus;
import com.developer.pos.payment.persistence.repository.PaymentIntentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(PaymentOrchestrator.class);

    private final PaymentIntentRepository intentRepository;
    private final List<PaymentProviderAdapter> adapters;
    private final CallbackNotifier callbackNotifier;

    public PaymentOrchestrator(
            PaymentIntentRepository intentRepository,
            List<PaymentProviderAdapter> adapters,
            CallbackNotifier callbackNotifier
    ) {
        this.intentRepository = intentRepository;
        this.adapters = adapters;
        this.callbackNotifier = callbackNotifier;
    }

    @Transactional
    public PaymentIntentEntity createIntent(CreateIntentRequest request) {
        PaymentProviderAdapter adapter = findAdapter(request.paymentMethod(), request.paymentScheme());

        PaymentIntentEntity intent = new PaymentIntentEntity();
        intent.setIntentId("PI-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        intent.setMerchantId(request.merchantId());
        intent.setStoreId(request.storeId());
        intent.setTableId(request.tableId());
        intent.setSessionRef(request.sessionRef());
        intent.setAmountCents(request.amountCents());
        intent.setCurrency(request.currency() != null ? request.currency() : "SGD");
        intent.setPaymentMethod(request.paymentMethod());
        intent.setPaymentScheme(request.paymentScheme());
        intent.setProviderCode(adapter.providerCode());
        intent.setCallbackUrl(request.callbackUrl());
        intent.setMetadataJson(request.metadataJson());
        intent.setStatus(PaymentStatus.CREATED);

        intentRepository.save(intent);

        // Call provider
        CreatePaymentResult result = adapter.createPayment(intent);

        if (result.success()) {
            intent.setProviderTransactionId(result.providerTransactionId());
            intent.setProviderStatus(result.providerStatus());
            intent.setCheckoutUrl(result.checkoutUrl());

            // Cash and similar sync providers succeed immediately
            if (result.checkoutUrl() == null || result.checkoutUrl().isBlank()) {
                intent.setStatus(PaymentStatus.SUCCEEDED);
                intent.setCompletedAt(OffsetDateTime.now());
            } else {
                intent.setStatus(PaymentStatus.PENDING);
            }
        } else {
            intent.setStatus(PaymentStatus.FAILED);
            intent.setErrorCode(result.errorCode());
            intent.setErrorMessage(result.errorMessage());
            intent.setCompletedAt(OffsetDateTime.now());
        }

        intentRepository.save(intent);

        // Notify POS if payment already succeeded (cash)
        if (intent.getStatus() == PaymentStatus.SUCCEEDED) {
            callbackNotifier.notifyPaymentResult(intent);
        }

        return intent;
    }

    @Transactional
    public PaymentIntentEntity handleProviderSuccess(String intentId, String providerTransactionId) {
        PaymentIntentEntity intent = intentRepository.findByIntentIdForUpdate(intentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment intent not found: " + intentId));

        if (intent.getStatus() == PaymentStatus.SUCCEEDED) {
            log.info("Intent {} already succeeded, skipping duplicate", intentId);
            return intent;
        }

        if (intent.getStatus() != PaymentStatus.PENDING && intent.getStatus() != PaymentStatus.CREATED) {
            throw new IllegalStateException("Cannot mark as succeeded from status: " + intent.getStatus());
        }

        intent.setStatus(PaymentStatus.SUCCEEDED);
        intent.setProviderTransactionId(providerTransactionId);
        intent.setProviderStatus("completed");
        intent.setCompletedAt(OffsetDateTime.now());
        intentRepository.save(intent);

        callbackNotifier.notifyPaymentResult(intent);
        return intent;
    }

    @Transactional
    public PaymentIntentEntity handleProviderFailure(String intentId, String errorCode, String errorMessage) {
        PaymentIntentEntity intent = intentRepository.findByIntentIdForUpdate(intentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment intent not found: " + intentId));

        if (intent.getStatus() == PaymentStatus.SUCCEEDED) {
            log.warn("Intent {} already succeeded, ignoring failure event", intentId);
            return intent;
        }

        intent.setStatus(PaymentStatus.FAILED);
        intent.setErrorCode(errorCode);
        intent.setErrorMessage(errorMessage);
        intent.setCompletedAt(OffsetDateTime.now());
        intentRepository.save(intent);

        return intent;
    }

    public PaymentIntentEntity getIntent(String intentId) {
        return intentRepository.findByIntentId(intentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment intent not found: " + intentId));
    }

    @Transactional
    public PaymentIntentEntity cancelIntent(String intentId) {
        PaymentIntentEntity intent = intentRepository.findByIntentIdForUpdate(intentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment intent not found: " + intentId));

        if (intent.getStatus() != PaymentStatus.CREATED && intent.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel intent in status: " + intent.getStatus());
        }

        intent.setStatus(PaymentStatus.CANCELLED);
        intent.setCompletedAt(OffsetDateTime.now());
        intentRepository.save(intent);

        return intent;
    }

    private PaymentProviderAdapter findAdapter(String paymentMethod, String paymentScheme) {
        return adapters.stream()
                .filter(a -> a.supports(paymentMethod, paymentScheme))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment provider supports method=" + paymentMethod + " scheme=" + paymentScheme));
    }

    public record CreateIntentRequest(
            Long merchantId,
            Long storeId,
            Long tableId,
            String sessionRef,
            long amountCents,
            String currency,
            String paymentMethod,
            String paymentScheme,
            String callbackUrl,
            String metadataJson
    ) {}
}
