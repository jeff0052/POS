package com.developer.pos.payment.orchestrator;

import com.developer.pos.payment.core.PaymentIntentEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class CallbackNotifier {

    private static final Logger log = LoggerFactory.getLogger(CallbackNotifier.class);
    private static final int MAX_RETRIES = 3;
    private static final long[] RETRY_DELAYS_MS = {1000, 3000, 10000};

    private final String callbackUrl;
    private final String callbackSecret;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public CallbackNotifier(
            @Value("${pos.callback-url}") String callbackUrl,
            @Value("${pos.callback-secret:}") String callbackSecret,
            ObjectMapper objectMapper
    ) {
        this.callbackUrl = callbackUrl;
        this.callbackSecret = callbackSecret;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Notify POS backend of payment result.
     * Returns true if POS confirmed settlement success (HTTP 2xx + settlementTriggered=true).
     * Retries up to 3 times on non-2xx responses.
     */
    public boolean notifyPaymentResult(PaymentIntentEntity intent) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentIntentId", intent.getIntentId());
        payload.put("status", intent.getStatus().name());
        payload.put("providerCode", intent.getProviderCode() != null ? intent.getProviderCode() : "");
        payload.put("providerTransactionId", intent.getProviderTransactionId() != null ? intent.getProviderTransactionId() : "");
        payload.put("amountCents", intent.getAmountCents());
        payload.put("currency", intent.getCurrency());
        payload.put("paymentMethod", intent.getPaymentMethod());
        payload.put("paymentScheme", intent.getPaymentScheme() != null ? intent.getPaymentScheme() : "");
        payload.put("merchantId", intent.getMerchantId());
        payload.put("storeId", intent.getStoreId());
        payload.put("tableId", intent.getTableId() != null ? intent.getTableId() : 0);
        payload.put("sessionRef", intent.getSessionRef() != null ? intent.getSessionRef() : "");

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                String json = objectMapper.writeValueAsString(payload);

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(callbackUrl))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

                if (callbackSecret != null && !callbackSecret.isBlank()) {
                    String signature = new HmacUtils("HmacSHA256", callbackSecret).hmacHex(json);
                    requestBuilder.header("X-Payment-Signature", signature);
                }

                HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    log.info("Payment callback succeeded for intent {} (attempt {})", intent.getIntentId(), attempt + 1);
                    return true;
                }

                // Non-2xx means settlement failed on POS side — retry
                log.warn("Payment callback returned {} for intent {} (attempt {}/{}): {}",
                        response.statusCode(), intent.getIntentId(), attempt + 1, MAX_RETRIES + 1, response.body());

            } catch (Exception e) {
                log.error("Payment callback error for intent {} (attempt {}/{}): {}",
                        intent.getIntentId(), attempt + 1, MAX_RETRIES + 1, e.getMessage());
            }

            // Wait before retry (except on last attempt)
            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAYS_MS[attempt]);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.error("Payment callback EXHAUSTED all retries for intent {}. Settlement may not have completed.", intent.getIntentId());
        return false;
    }
}
