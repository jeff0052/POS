package com.developer.pos.v2.settlement.interfaces.rest.internal;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.settlement.application.command.CollectCashierSettlementCommand;
import com.developer.pos.v2.settlement.application.service.CashierSettlementApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Internal endpoint for the Payment microservice to notify POS backend
 * when a payment succeeds. This triggers the settlement/close-table flow.
 */
@RestController
@RequestMapping("/api/v2/internal")
public class PaymentCallbackController {

    private static final Logger log = LoggerFactory.getLogger(PaymentCallbackController.class);

    @Value("${pos.callback-secret:}")
    private String callbackSecret;

    private final CashierSettlementApplicationService settlementService;

    public PaymentCallbackController(CashierSettlementApplicationService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/payment-callback")
    public ApiResponse<Map<String, Object>> handlePaymentCallback(
            @RequestHeader(value = "X-Payment-Signature", required = false) String signature,
            @RequestBody JsonNode payload
    ) {
        // Verify HMAC signature if secret is configured
        if (callbackSecret != null && !callbackSecret.isBlank()) {
            String payloadText = payload.toString();
            String expectedSignature = new HmacUtils("HmacSHA256", callbackSecret).hmacHex(payloadText);
            if (signature == null || !MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            )) {
                log.error("Invalid payment callback signature");
                throw new SecurityException("Invalid payment callback signature");
            }
        }

        String status = payload.path("status").asText("");
        String paymentIntentId = payload.path("paymentIntentId").asText("");
        long amountCents = payload.path("amountCents").asLong(0);
        long storeId = payload.path("storeId").asLong(0);
        long tableId = payload.path("tableId").asLong(0);
        String sessionRef = payload.path("sessionRef").asText(null);
        String paymentMethod = payload.path("paymentMethod").asText("UNKNOWN");

        log.info("Payment callback received: intent={}, status={}, amount={}, store={}, table={}",
                paymentIntentId, status, amountCents, storeId, tableId);

        if (!"SUCCEEDED".equals(status)) {
            log.info("Payment status is {}, no settlement triggered", status);
            return ApiResponse.success(Map.of(
                    "acknowledged", true,
                    "settlementTriggered", false,
                    "paymentIntentId", paymentIntentId
            ));
        }

        // Trigger settlement
        try {
            settlementService.collectForTable(
                    storeId,
                    tableId,
                    new CollectCashierSettlementCommand(sessionRef, 0L, paymentMethod, amountCents)
            );

            return ApiResponse.success(Map.of(
                    "acknowledged", true,
                    "settlementTriggered", true,
                    "paymentIntentId", paymentIntentId
            ));
        } catch (Exception e) {
            log.error("Settlement failed for payment callback intent={}: {}", paymentIntentId, e.getMessage());
            return ApiResponse.success(Map.of(
                    "acknowledged", true,
                    "settlementTriggered", false,
                    "error", e.getMessage(),
                    "paymentIntentId", paymentIntentId
            ));
        }
    }
}
