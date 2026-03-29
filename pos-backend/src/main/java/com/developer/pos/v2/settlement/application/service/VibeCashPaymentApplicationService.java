package com.developer.pos.v2.settlement.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.settlement.application.command.CollectCashierSettlementCommand;
import com.developer.pos.v2.settlement.application.dto.SettlementPreviewDto;
import com.developer.pos.v2.settlement.application.dto.VibeCashPaymentAttemptDto;
import com.developer.pos.v2.settlement.application.dto.VibeCashWebhookResultDto;
import com.developer.pos.v2.settlement.infrastructure.persistence.entity.PaymentAttemptEntity;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaPaymentAttemptRepository;
import com.developer.pos.v2.settlement.infrastructure.persistence.repository.JpaSettlementRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VibeCashPaymentApplicationService implements UseCase {

    private static final Logger log = LoggerFactory.getLogger(VibeCashPaymentApplicationService.class);
    private static final String PROVIDER = "VIBECASH";
    private static final String PAYMENT_METHOD = "QR";

    private final JpaTableSessionRepository tableSessionRepository;
    private final JpaPaymentAttemptRepository paymentAttemptRepository;
    private final JpaSettlementRecordRepository settlementRecordRepository;
    private final CashierSettlementApplicationService cashierSettlementApplicationService;
    private final PaymentStackingService paymentStackingService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiUrl;
    private final String secret;
    private final String webhookSecret;
    private final String currencyCode;

    public VibeCashPaymentApplicationService(
            JpaTableSessionRepository tableSessionRepository,
            JpaPaymentAttemptRepository paymentAttemptRepository,
            JpaSettlementRecordRepository settlementRecordRepository,
            CashierSettlementApplicationService cashierSettlementApplicationService,
            @Lazy PaymentStackingService paymentStackingService,
            ObjectMapper objectMapper,
            @Value("${vibecash.api-url}") String apiUrl,
            @Value("${vibecash.secret}") String secret,
            @Value("${vibecash.webhook-secret:}") String webhookSecret,
            @Value("${vibecash.currency}") String currencyCode
    ) {
        this.tableSessionRepository = tableSessionRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.settlementRecordRepository = settlementRecordRepository;
        this.cashierSettlementApplicationService = cashierSettlementApplicationService;
        this.paymentStackingService = paymentStackingService;
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.secret = secret;
        this.webhookSecret = webhookSecret;
        this.currencyCode = currencyCode;
    }

    @Transactional
    public VibeCashPaymentAttemptDto startPayment(Long storeId, Long tableId, String paymentScheme) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("VibeCash secret is not configured.");
        }

        cashierSettlementApplicationService.moveTableToPaymentPending(storeId, tableId);
        SettlementPreviewDto preview = cashierSettlementApplicationService.getTableSettlementPreview(storeId, tableId);
        TableSessionEntity session = tableSessionRepository
                .findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(storeId, tableId, "OPEN")
                .orElseThrow(() -> new IllegalArgumentException("Open table session not found."));

        PaymentAttemptEntity paymentAttempt = new PaymentAttemptEntity();
        paymentAttempt.setPaymentAttemptId("PAT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18));
        paymentAttempt.setProvider(PROVIDER);
        paymentAttempt.setPaymentMethod(PAYMENT_METHOD);
        paymentAttempt.setPaymentScheme(paymentScheme);
        paymentAttempt.setStoreId(storeId);
        paymentAttempt.setTableId(tableId);
        paymentAttempt.setTableSessionId(session.getId());
        paymentAttempt.setSessionRef(session.getSessionId());
        paymentAttempt.setSettlementAmountCents(preview.pricing().payableAmountCents());
        paymentAttempt.setCurrencyCode(currencyCode);
        paymentAttempt.setAttemptStatus("PENDING_CUSTOMER");
        paymentAttempt.setProviderStatus("CREATING_LINK");
        paymentAttempt.setSettlementRecordId(null); // legacy path: no stacking record
        paymentAttempt.setCreatedAt(OffsetDateTime.now());
        paymentAttempt.setUpdatedAt(OffsetDateTime.now());
        paymentAttemptRepository.saveAndFlush(paymentAttempt);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", preview.pricing().payableAmountCents());
        payload.put("currency", currencyCode);
        payload.put("name", "Table " + tableId + " payment");
        payload.put("description", "POS cashier payment for session " + session.getSessionId());
        payload.put("paymentMethodTypes", List.of(toGatewayScheme(paymentScheme)));
        payload.put("metadata", Map.of(
                "paymentAttemptId", paymentAttempt.getPaymentAttemptId(),
                "tableSessionId", session.getSessionId(),
                "storeId", String.valueOf(storeId),
                "tableId", String.valueOf(tableId)
        ));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl.replaceAll("/$", "") + "/v1/payment_links"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secret)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                paymentAttempt.setAttemptStatus("FAILED");
                paymentAttempt.setProviderStatus("HTTP_" + response.statusCode());
                paymentAttempt.setLastWebhookPayloadJson(response.body());
                paymentAttempt.setUpdatedAt(OffsetDateTime.now());
                paymentAttemptRepository.saveAndFlush(paymentAttempt);
                throw new IllegalStateException("VibeCash payment link creation failed: " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            paymentAttempt.setProviderPaymentId(textOrNull(data, "id", "paymentLinkId"));
            paymentAttempt.setProviderCheckoutUrl(textOrNull(data, "url", "checkoutUrl"));
            paymentAttempt.setProviderStatus(textOrDefault(data, "status", "open"));
            paymentAttempt.setUpdatedAt(OffsetDateTime.now());
            paymentAttemptRepository.saveAndFlush(paymentAttempt);
            return toDto(paymentAttempt);
        } catch (IOException | InterruptedException exception) {
            paymentAttempt.setAttemptStatus("FAILED");
            paymentAttempt.setProviderStatus("REQUEST_FAILED");
            paymentAttempt.setLastWebhookPayloadJson(exception.getMessage());
            paymentAttempt.setUpdatedAt(OffsetDateTime.now());
            paymentAttemptRepository.saveAndFlush(paymentAttempt);
            throw new IllegalStateException("Failed to create VibeCash payment link.", exception);
        }
    }

    /**
     * Creates a VibeCash payment attempt for the stacking flow.
     * Unlike startPayment(), this method:
     *  - does NOT call moveTableToPaymentPending (stacking manages its own lifecycle)
     *  - sets settlementRecordId on the attempt (signals stacking path to webhook handler)
     *  - uses settlement.externalPaymentCents as the charged amount
     */
    @Transactional
    public VibeCashPaymentAttemptDto startStackingPayment(
            Long storeId, Long tableId, Long settlementId, String paymentScheme) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("VibeCash secret is not configured.");
        }

        var settlement = settlementRecordRepository.findByIdForUpdate(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + settlementId));
        if (!"PENDING".equals(settlement.getFinalStatus())) {
            throw new IllegalStateException("Settlement is not PENDING: " + settlement.getFinalStatus());
        }

        TableSessionEntity session = tableSessionRepository.findById(settlement.getStackingSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Stacking session not found: " + settlement.getStackingSessionId()));

        long amountCents = settlement.getExternalPaymentCents();

        PaymentAttemptEntity attempt = new PaymentAttemptEntity();
        attempt.setPaymentAttemptId("PAT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase());
        attempt.setProvider(PROVIDER);
        attempt.setPaymentMethod(PAYMENT_METHOD);
        attempt.setPaymentScheme(paymentScheme);
        attempt.setStoreId(storeId);
        attempt.setTableId(tableId);
        attempt.setTableSessionId(session.getId());
        attempt.setSessionRef(session.getSessionId());
        attempt.setSettlementAmountCents(amountCents);
        attempt.setCurrencyCode(currencyCode);
        attempt.setAttemptStatus("PENDING_CUSTOMER");
        attempt.setProviderStatus("CREATING_LINK");
        attempt.setSettlementRecordId(settlementId);  // stacking path marker
        attempt.setCreatedAt(OffsetDateTime.now());
        attempt.setUpdatedAt(OffsetDateTime.now());
        paymentAttemptRepository.saveAndFlush(attempt);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", amountCents);
        payload.put("currency", currencyCode);
        payload.put("name", "Table " + tableId + " payment");
        payload.put("description", "Stacking payment for settlement " + settlement.getSettlementNo());
        payload.put("paymentMethodTypes", List.of(toGatewayScheme(paymentScheme)));
        payload.put("metadata", Map.of(
                "paymentAttemptId", attempt.getPaymentAttemptId(),
                "tableSessionId", session.getSessionId(),
                "storeId", String.valueOf(storeId),
                "tableId", String.valueOf(tableId),
                "settlementId", String.valueOf(settlementId)
        ));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl.replaceAll("/$", "") + "/v1/payment_links"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secret)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                attempt.setAttemptStatus("FAILED");
                attempt.setProviderStatus("HTTP_" + response.statusCode());
                attempt.setLastWebhookPayloadJson(response.body());
                attempt.setUpdatedAt(OffsetDateTime.now());
                paymentAttemptRepository.saveAndFlush(attempt);
                throw new IllegalStateException("VibeCash stacking payment link creation failed: " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            attempt.setProviderPaymentId(textOrNull(data, "id", "paymentLinkId"));
            attempt.setProviderCheckoutUrl(textOrNull(data, "url", "checkoutUrl"));
            attempt.setProviderStatus(textOrDefault(data, "status", "open"));
            attempt.setUpdatedAt(OffsetDateTime.now());
            paymentAttemptRepository.saveAndFlush(attempt);
            return toDto(attempt);
        } catch (IOException | InterruptedException exception) {
            attempt.setAttemptStatus("FAILED");
            attempt.setProviderStatus("REQUEST_FAILED");
            attempt.setLastWebhookPayloadJson(exception.getMessage());
            attempt.setUpdatedAt(OffsetDateTime.now());
            paymentAttemptRepository.saveAndFlush(attempt);
            throw new IllegalStateException("Failed to create VibeCash stacking payment link.", exception);
        }
    }

    @Transactional(readOnly = true)
    public VibeCashPaymentAttemptDto getAttempt(Long storeId, Long tableId, String paymentAttemptId) {
        PaymentAttemptEntity paymentAttempt = paymentAttemptRepository.findByPaymentAttemptId(paymentAttemptId)
                .filter(item -> item.getStoreId().equals(storeId) && item.getTableId().equals(tableId))
                .orElseThrow(() -> new IllegalArgumentException("Payment attempt not found."));
        return toDto(paymentAttempt);
    }

    @Transactional
    public VibeCashWebhookResultDto handleWebhook(String signature, JsonNode payload) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new SecurityException("Webhook secret is not configured. Cannot verify webhook signature.");
        }
        String expectedSignature = new HmacUtils("HmacSHA256", webhookSecret).hmacHex(payload.toString());
        if (signature == null || !MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        )) {
            throw new SecurityException("Invalid webhook signature");
        }

        String eventType = payload.path("type").asText(null);
        JsonNode objectNode = payload.path("data").path("object");
        if (objectNode.isMissingNode() || objectNode.isNull()) {
            objectNode = payload.path("data");
        }

        String paymentAttemptId = objectNode.path("metadata").path("paymentAttemptId").asText(null);
        String providerPaymentId = textOrNull(objectNode, "id", "paymentLinkId");
        PaymentAttemptEntity paymentAttempt = null;
        if (paymentAttemptId != null && !paymentAttemptId.isBlank()) {
            paymentAttempt = paymentAttemptRepository.findByPaymentAttemptId(paymentAttemptId).orElse(null);
        }
        if (paymentAttempt == null && providerPaymentId != null && !providerPaymentId.isBlank()) {
            paymentAttempt = paymentAttemptRepository.findByProviderAndProviderPaymentId(PROVIDER, providerPaymentId).orElse(null);
        }
        if (paymentAttempt == null) {
            throw new IllegalArgumentException("Payment attempt not found for webhook.");
        }

        // Stacking path: attempt is linked to a SettlementRecord
        if (paymentAttempt.getSettlementRecordId() != null) {
            if ("REPLACED".equals(paymentAttempt.getAttemptStatus())) {
                log.info("Webhook for REPLACED attempt {}, ignoring", paymentAttempt.getPaymentAttemptId());
                return new VibeCashWebhookResultDto(eventType, paymentAttempt.getPaymentAttemptId(),
                        paymentAttempt.getAttemptStatus(), false);
            }
            paymentAttempt.setWebhookEventType(eventType);
            paymentAttempt.setLastWebhookPayloadJson(payload.toPrettyString());
            paymentAttempt.setProviderStatus(textOrDefault(objectNode, "status", eventType));
            paymentAttempt.setUpdatedAt(OffsetDateTime.now());
            if ("payment.succeeded".equals(eventType)) {
                paymentAttempt.setAttemptStatus("SUCCEEDED");
                paymentAttemptRepository.save(paymentAttempt);
                paymentStackingService.confirmStacking(paymentAttempt.getSettlementRecordId());
            } else if ("payment.failed".equals(eventType)) {
                paymentAttempt.setAttemptStatus("FAILED");
                paymentAttemptRepository.save(paymentAttempt);
                log.info("Payment failed for settlement {}, keeping PENDING (switch-method window open)",
                        paymentAttempt.getSettlementRecordId());
            } else if ("checkout.session.expired".equals(eventType)) {
                paymentAttempt.setAttemptStatus("EXPIRED");
                paymentAttemptRepository.save(paymentAttempt);
                paymentStackingService.releaseStacking(paymentAttempt.getSettlementRecordId(), "CHECKOUT_EXPIRED");
            }
            return new VibeCashWebhookResultDto(eventType, paymentAttempt.getPaymentAttemptId(),
                    paymentAttempt.getAttemptStatus(), false);
        } else {
        // Legacy path: no stacking record
        paymentAttempt.setWebhookEventType(eventType);
        paymentAttempt.setLastWebhookPayloadJson(payload.toPrettyString());
        paymentAttempt.setProviderStatus(textOrDefault(objectNode, "status", eventType));
        paymentAttempt.setUpdatedAt(OffsetDateTime.now());

        boolean settlementTriggered = false;
        if ("payment.succeeded".equals(eventType)) {
            if (!"SUCCEEDED".equals(paymentAttempt.getAttemptStatus()) && !"SETTLED".equals(paymentAttempt.getAttemptStatus())) {
                paymentAttempt.setAttemptStatus("SUCCEEDED");
                paymentAttempt.setCompletedAt(OffsetDateTime.now());
                cashierSettlementApplicationService.collectForTable(
                        paymentAttempt.getStoreId(),
                        paymentAttempt.getTableId(),
                        new CollectCashierSettlementCommand(
                                paymentAttempt.getSessionRef(),
                                0L,
                                paymentAttempt.getPaymentScheme(),
                                paymentAttempt.getSettlementAmountCents()
                        )
                );
                paymentAttempt.setAttemptStatus("SETTLED");
                settlementTriggered = true;
            }
        } else if ("payment.failed".equals(eventType)) {
            paymentAttempt.setAttemptStatus("FAILED");
            paymentAttempt.setCompletedAt(OffsetDateTime.now());
        } else if ("checkout.session.expired".equals(eventType)) {
            paymentAttempt.setAttemptStatus("EXPIRED");
            paymentAttempt.setCompletedAt(OffsetDateTime.now());
        }

        paymentAttemptRepository.saveAndFlush(paymentAttempt);
        return new VibeCashWebhookResultDto(
                eventType,
                paymentAttempt.getPaymentAttemptId(),
                paymentAttempt.getAttemptStatus(),
                settlementTriggered
        );
        }
    }

    private VibeCashPaymentAttemptDto toDto(PaymentAttemptEntity paymentAttempt) {
        return new VibeCashPaymentAttemptDto(
                paymentAttempt.getPaymentAttemptId(),
                paymentAttempt.getProvider(),
                paymentAttempt.getPaymentMethod(),
                paymentAttempt.getPaymentScheme(),
                paymentAttempt.getAttemptStatus(),
                paymentAttempt.getProviderStatus(),
                paymentAttempt.getProviderPaymentId(),
                paymentAttempt.getProviderCheckoutUrl(),
                paymentAttempt.getSettlementAmountCents(),
                paymentAttempt.getCurrencyCode()
        );
    }

    private String toGatewayScheme(String paymentScheme) {
        return switch (paymentScheme) {
            case "WECHAT_QR" -> "wechat";
            case "ALIPAY_QR" -> "alipay";
            case "PAYNOW_QR" -> "paynow";
            default -> throw new IllegalArgumentException("Unsupported VibeCash payment scheme: " + paymentScheme);
        };
    }

    private String textOrNull(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.path(fieldName);
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                String value = valueNode.asText();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private String textOrDefault(JsonNode node, String fieldName, String fallback) {
        JsonNode valueNode = node.path(fieldName);
        if (!valueNode.isMissingNode() && !valueNode.isNull()) {
            String value = valueNode.asText();
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return fallback;
    }
}
