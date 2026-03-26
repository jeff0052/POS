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

    public void notifyPaymentResult(PaymentIntentEntity intent) {
        try {
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
                log.info("Payment callback sent successfully for intent {} → status {}", intent.getIntentId(), response.statusCode());
            } else {
                log.error("Payment callback failed for intent {} → status {}, body: {}", intent.getIntentId(), response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Payment callback error for intent {}: {}", intent.getIntentId(), e.getMessage(), e);
        }
    }
}
