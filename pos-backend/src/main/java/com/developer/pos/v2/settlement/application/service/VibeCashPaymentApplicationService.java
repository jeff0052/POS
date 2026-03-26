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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.codec.digest.HmacUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VibeCashPaymentApplicationService implements UseCase {

    private static final String PROVIDER = "VIBECASH";
    private static final String PAYMENT_METHOD = "QR";

    private final JpaTableSessionRepository tableSessionRepository;
    private final JpaPaymentAttemptRepository paymentAttemptRepository;
    private final CashierSettlementApplicationService cashierSettlementApplicationService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiUrl;
    private final String secret;
    private final String webhookSecret;
    private final String currencyCode;

    public VibeCashPaymentApplicationService(
            JpaTableSessionRepository tableSessionRepository,
            JpaPaymentAttemptRepository paymentAttemptRepository,
            CashierSettlementApplicationService cashierSettlementApplicationService,
            ObjectMapper objectMapper,
            @Value("${vibecash.api-url}") String apiUrl,
            @Value("${vibecash.secret}") String secret,
            @Value("${vibecash.webhook-secret:}") String webhookSecret,
            @Value("${vibecash.currency}") String currencyCode
    ) {
        this.tableSessionRepository = tableSessionRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.cashierSettlementApplicationService = cashierSettlementApplicationService;
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

    @Transactional(readOnly = true)
    public VibeCashPaymentAttemptDto getAttempt(Long storeId, Long tableId, String paymentAttemptId) {
        PaymentAttemptEntity paymentAttempt = paymentAttemptRepository.findByPaymentAttemptId(paymentAttemptId)
                .filter(item -> item.getStoreId().equals(storeId) && item.getTableId().equals(tableId))
                .orElseThrow(() -> new IllegalArgumentException("Payment attempt not found."));
        return toDto(paymentAttempt);
    }

    @Transactional
    public VibeCashWebhookResultDto handleWebhook(String signature, JsonNode payload) {
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            String payloadText = payload.toString();
            String expectedSignature = new HmacUtils("HmacSHA256", webhookSecret).hmacHex(payloadText);
            if (signature == null || !java.security.MessageDigest.isEqual(
                    expectedSignature.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    signature.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                throw new SecurityException("Invalid webhook signature");
            }
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
