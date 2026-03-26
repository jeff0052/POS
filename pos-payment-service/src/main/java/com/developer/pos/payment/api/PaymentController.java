package com.developer.pos.payment.api;

import com.developer.pos.payment.core.PaymentIntentEntity;
import com.developer.pos.payment.orchestrator.PaymentOrchestrator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentOrchestrator orchestrator;

    public PaymentController(PaymentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/intents")
    public ResponseEntity<PaymentIntentDto> createIntent(@Valid @RequestBody CreateIntentRequest request) {
        PaymentIntentEntity intent = orchestrator.createIntent(new PaymentOrchestrator.CreateIntentRequest(
                request.merchantId(),
                request.storeId(),
                request.tableId(),
                request.sessionRef(),
                request.amountCents(),
                request.currency(),
                request.paymentMethod(),
                request.paymentScheme(),
                request.callbackUrl(),
                request.metadataJson()
        ));

        return ResponseEntity.ok(toDto(intent));
    }

    @GetMapping("/intents/{intentId}")
    public ResponseEntity<PaymentIntentDto> getIntent(@PathVariable String intentId) {
        return ResponseEntity.ok(toDto(orchestrator.getIntent(intentId)));
    }

    @PostMapping("/intents/{intentId}/cancel")
    public ResponseEntity<PaymentIntentDto> cancelIntent(@PathVariable String intentId) {
        return ResponseEntity.ok(toDto(orchestrator.cancelIntent(intentId)));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "pos-payment-service"));
    }

    private PaymentIntentDto toDto(PaymentIntentEntity entity) {
        return new PaymentIntentDto(
                entity.getIntentId(),
                entity.getMerchantId(),
                entity.getStoreId(),
                entity.getTableId(),
                entity.getSessionRef(),
                entity.getAmountCents(),
                entity.getCurrency(),
                entity.getPaymentMethod(),
                entity.getPaymentScheme(),
                entity.getProviderCode(),
                entity.getStatus().name(),
                entity.getProviderTransactionId(),
                entity.getProviderStatus(),
                entity.getCheckoutUrl(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null,
                entity.getCompletedAt() != null ? entity.getCompletedAt().toString() : null
        );
    }

    public record CreateIntentRequest(
            @NotNull Long merchantId,
            @NotNull Long storeId,
            Long tableId,
            String sessionRef,
            @Positive long amountCents,
            String currency,
            @NotBlank String paymentMethod,
            String paymentScheme,
            String callbackUrl,
            String metadataJson
    ) {}

    public record PaymentIntentDto(
            String intentId,
            Long merchantId,
            Long storeId,
            Long tableId,
            String sessionRef,
            long amountCents,
            String currency,
            String paymentMethod,
            String paymentScheme,
            String providerCode,
            String status,
            String providerTransactionId,
            String providerStatus,
            String checkoutUrl,
            String errorCode,
            String errorMessage,
            String createdAt,
            String completedAt
    ) {}
}
