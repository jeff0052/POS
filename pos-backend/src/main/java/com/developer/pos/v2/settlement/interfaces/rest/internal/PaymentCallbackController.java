package com.developer.pos.v2.settlement.interfaces.rest.internal;

import com.developer.pos.v2.settlement.application.command.CollectCashierSettlementCommand;
import com.developer.pos.v2.settlement.application.service.CashierSettlementApplicationService;
import com.developer.pos.v2.settlement.application.service.RefundApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/internal")
public class PaymentCallbackController {

    private static final Logger log = LoggerFactory.getLogger(PaymentCallbackController.class);

    @Value("${pos.callback-secret:}")
    private String callbackSecret;

    private final CashierSettlementApplicationService settlementService;
    private final RefundApplicationService refundService;

    public PaymentCallbackController(CashierSettlementApplicationService settlementService,
                                     RefundApplicationService refundService) {
        this.settlementService = settlementService;
        this.refundService = refundService;
    }

    @PostMapping("/payment-callback")
    public ResponseEntity<Map<String, Object>> handlePaymentCallback(
            @RequestHeader(value = "X-Payment-Signature", required = false) String signature,
            @RequestBody JsonNode payload
    ) {
        // Secure-by-default: reject if no callback secret is configured
        if (callbackSecret == null || callbackSecret.isBlank()) {
            log.error("Payment callback rejected: pos.callback-secret is not configured");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Callback secret not configured", "settlementTriggered", false));
        }

        String payloadText = payload.toString();
        String expectedSignature = new HmacUtils("HmacSHA256", callbackSecret).hmacHex(payloadText);
        if (signature == null || !MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        )) {
            log.error("Invalid payment callback signature");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid signature", "settlementTriggered", false));
        }

        String status = payload.path("status").asText("");
        String paymentIntentId = payload.path("paymentIntentId").asText("");
        long amountCents = payload.path("amountCents").asLong(0);
        long storeId = payload.path("storeId").asLong(0);
        long tableId = payload.path("tableId").asLong(0);
        String sessionRef = payload.path("sessionRef").asText(null);
        String paymentMethod = payload.path("paymentMethod").asText("UNKNOWN");

        log.info("Payment callback: intent={}, status={}, amount={}, store={}, table={}",
                paymentIntentId, status, amountCents, storeId, tableId);

        if (!"SUCCEEDED".equals(status)) {
            return ResponseEntity.ok(Map.of(
                    "acknowledged", true,
                    "settlementTriggered", false,
                    "paymentIntentId", paymentIntentId
            ));
        }

        // Trigger settlement — return non-2xx on failure so payment service knows to retry
        try {
            settlementService.collectForTable(
                    storeId, tableId,
                    new CollectCashierSettlementCommand(sessionRef, 0L, paymentMethod, amountCents)
            );

            return ResponseEntity.ok(Map.of(
                    "acknowledged", true,
                    "settlementTriggered", true,
                    "paymentIntentId", paymentIntentId
            ));
        } catch (Exception e) {
            log.error("Settlement FAILED for callback intent={}: {}", paymentIntentId, e.getMessage());
            // Return 500 so payment service knows settlement didn't happen
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "acknowledged", true,
                            "settlementTriggered", false,
                            "error", e.getMessage() != null ? e.getMessage() : "Settlement failed",
                            "paymentIntentId", paymentIntentId
                    ));
        }
    }

    /**
     * External refund completion callback — called by the payment service after
     * the external provider confirms the refund has actually succeeded.
     * Books the deferred external portion into settlement accounting.
     */
    @PostMapping("/refund-callback")
    public ResponseEntity<Map<String, Object>> handleRefundCallback(
            @RequestHeader(value = "X-Payment-Signature", required = false) String signature,
            @RequestBody JsonNode payload
    ) {
        // Secure-by-default: reject if no callback secret is configured
        if (callbackSecret == null || callbackSecret.isBlank()) {
            log.error("Refund callback rejected: pos.callback-secret is not configured");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Callback secret not configured", "refundCompleted", false));
        }

        String payloadText = payload.toString();
        String expectedSignature = new HmacUtils("HmacSHA256", callbackSecret).hmacHex(payloadText);
        if (signature == null || !MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        )) {
            log.error("Invalid refund callback signature");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid signature", "refundCompleted", false));
        }

        String refundNo = payload.path("refundNo").asText("");
        String status = payload.path("status").asText("");

        log.info("Refund callback: refundNo={}, status={}", refundNo, status);

        if (!"SUCCEEDED".equals(status)) {
            return ResponseEntity.ok(Map.of(
                    "acknowledged", true,
                    "refundCompleted", false,
                    "refundNo", refundNo
            ));
        }

        try {
            refundService.completeExternalRefund(refundNo);
            return ResponseEntity.ok(Map.of(
                    "acknowledged", true,
                    "refundCompleted", true,
                    "refundNo", refundNo
            ));
        } catch (Exception e) {
            log.error("Refund completion FAILED for refundNo={}: {}", refundNo, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "acknowledged", true,
                            "refundCompleted", false,
                            "error", e.getMessage() != null ? e.getMessage() : "Refund completion failed",
                            "refundNo", refundNo
                    ));
        }
    }
}
