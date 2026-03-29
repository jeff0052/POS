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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate txTemplate;

    public VibeCashPaymentApplicationService(
            JpaTableSessionRepository tableSessionRepository,
            JpaPaymentAttemptRepository paymentAttemptRepository,
            JpaSettlementRecordRepository settlementRecordRepository,
            CashierSettlementApplicationService cashierSettlementApplicationService,
            @Lazy PaymentStackingService paymentStackingService,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
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
        this.txTemplate = new TransactionTemplate(transactionManager);
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
     *
     * Transaction discipline: Phase 1 (validate + persist attempt) commits BEFORE the HTTP
     * call so the attempt record is durable regardless of HTTP outcome.  Phase 2 (persist
     * provider URL or mark FAILED) runs in its own committed transaction afterwards.
     * No open transaction spans the network call.
     */
    public VibeCashPaymentAttemptDto startStackingPayment(
            Long storeId, Long tableId, Long settlementId, String paymentScheme) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("VibeCash secret is not configured.");
        }

        // Phase 1: validate settlement + persist attempt — commits before HTTP call
        record Phase1(String attemptId, Map<String, Object> payload) {}
        Phase1 p1 = txTemplate.execute(status -> {
            var settlement = settlementRecordRepository.findByIdForUpdate(settlementId)
                    .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + settlementId));
            if (!"PENDING".equals(settlement.getFinalStatus())) {
                throw new IllegalStateException("Settlement is not PENDING: " + settlement.getFinalStatus());
            }
            TableSessionEntity session = tableSessionRepository.findById(settlement.getStackingSessionId())
                    .orElseThrow(() -> new IllegalArgumentException("Stacking session not found: " + settlement.getStackingSessionId()));
            long amountCents = settlement.getExternalPaymentCents();

            String attemptId = "PAT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
            PaymentAttemptEntity attempt = new PaymentAttemptEntity();
            attempt.setPaymentAttemptId(attemptId);
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
                    "paymentAttemptId", attemptId,
                    "tableSessionId", session.getSessionId(),
                    "storeId", String.valueOf(storeId),
                    "tableId", String.valueOf(tableId),
                    "settlementId", String.valueOf(settlementId)
            ));
            return new Phase1(attemptId, payload);
        });

        // HTTP call — outside any transaction; Phase 1 is already committed
        HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl.replaceAll("/$", "") + "/v1/payment_links"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secret)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(p1.payload())))
                    .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            markAttemptFailed(p1.attemptId(), "REQUEST_FAILED", e.getMessage());
            throw new IllegalStateException("Failed to create VibeCash stacking payment link.", e);
        }

        if (response.statusCode() >= 300) {
            markAttemptFailed(p1.attemptId(), "HTTP_" + response.statusCode(), response.body());
            throw new IllegalStateException("VibeCash stacking payment link creation failed: " + response.body());
        }

        // Phase 2: persist provider URL — new committed transaction
        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            String providerPaymentId = textOrNull(data, "id", "paymentLinkId");
            String checkoutUrl = textOrNull(data, "url", "checkoutUrl");
            String providerStatus = textOrDefault(data, "status", "open");
            return txTemplate.execute(txStatus -> {
                var fresh = paymentAttemptRepository.findByPaymentAttemptId(p1.attemptId())
                        .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + p1.attemptId()));
                fresh.setProviderPaymentId(providerPaymentId);
                fresh.setProviderCheckoutUrl(checkoutUrl);
                fresh.setProviderStatus(providerStatus);
                fresh.setUpdatedAt(OffsetDateTime.now());
                paymentAttemptRepository.save(fresh);
                return toDto(fresh);
            });
        } catch (IOException e) {
            markAttemptFailed(p1.attemptId(), "PARSE_ERROR", e.getMessage());
            throw new IllegalStateException("Failed to parse VibeCash stacking payment response.", e);
        }
    }

    /**
     * Creates a VibeCash payment link for an already-saved PaymentAttemptEntity.
     * Used by switchMethod after creating the replacement attempt entity with correct
     * retry tracking metadata (retryCount, parentAttemptId, etc.).
     *
     * Transaction discipline: snapshot the attempt in Phase 1 (committed), make the HTTP
     * call outside any transaction, then persist the provider URL in Phase 2 (committed).
     */
    public VibeCashPaymentAttemptDto createPaymentLinkForSavedAttempt(String paymentAttemptId) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("VibeCash secret is not configured.");
        }

        // Phase 1: snapshot attempt data needed for payload
        record AttemptSnapshot(String attemptId, long amountCents, String paymentScheme,
                               Long tableId, Long storeId, String sessionRef, Long settlementRecordId) {}
        AttemptSnapshot snapshot = txTemplate.execute(status -> {
            PaymentAttemptEntity a = paymentAttemptRepository.findByPaymentAttemptId(paymentAttemptId)
                    .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + paymentAttemptId));
            return new AttemptSnapshot(
                    a.getPaymentAttemptId(),
                    a.getSettlementAmountCents(),
                    a.getPaymentScheme(),
                    a.getTableId(),
                    a.getStoreId(),
                    a.getSessionRef(),
                    a.getSettlementRecordId()
            );
        });

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", snapshot.amountCents());
        payload.put("currency", currencyCode);
        payload.put("name", "Table " + snapshot.tableId() + " payment");
        payload.put("description", "Stacking switch-method attempt " + snapshot.attemptId());
        payload.put("paymentMethodTypes", List.of(toGatewayScheme(snapshot.paymentScheme())));
        payload.put("metadata", Map.of(
                "paymentAttemptId", snapshot.attemptId(),
                "tableSessionId", snapshot.sessionRef() != null ? snapshot.sessionRef() : "",
                "storeId", String.valueOf(snapshot.storeId()),
                "tableId", String.valueOf(snapshot.tableId()),
                "settlementId", String.valueOf(snapshot.settlementRecordId())
        ));

        // HTTP call — outside any transaction
        HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl.replaceAll("/$", "") + "/v1/payment_links"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + secret)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            markAttemptFailed(snapshot.attemptId(), "REQUEST_FAILED", e.getMessage());
            throw new IllegalStateException("Failed to create VibeCash payment link.", e);
        }

        if (response.statusCode() >= 300) {
            markAttemptFailed(snapshot.attemptId(), "HTTP_" + response.statusCode(), response.body());
            throw new IllegalStateException("VibeCash payment link creation failed: " + response.body());
        }

        // Phase 2: persist provider URL — new committed transaction
        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.path("data");
            String providerPaymentId = textOrNull(data, "id", "paymentLinkId");
            String checkoutUrl = textOrNull(data, "url", "checkoutUrl");
            String providerStatus = textOrDefault(data, "status", "open");
            return txTemplate.execute(txStatus -> {
                var fresh = paymentAttemptRepository.findByPaymentAttemptId(snapshot.attemptId())
                        .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + snapshot.attemptId()));
                fresh.setProviderPaymentId(providerPaymentId);
                fresh.setProviderCheckoutUrl(checkoutUrl);
                fresh.setProviderStatus(providerStatus);
                fresh.setUpdatedAt(OffsetDateTime.now());
                paymentAttemptRepository.save(fresh);
                return toDto(fresh);
            });
        } catch (IOException e) {
            markAttemptFailed(snapshot.attemptId(), "PARSE_ERROR", e.getMessage());
            throw new IllegalStateException("Failed to parse VibeCash payment response.", e);
        }
    }

    /**
     * Returns the most recently created non-REPLACED attempt for a stacking settlement.
     * Used by the recovery endpoint when the frontend has lost the newAttemptId.
     *
     * Intentionally returns any status except REPLACED:
     *  - PENDING_CUSTOMER (with URL)   → normal; present checkout URL to customer
     *  - PENDING_CUSTOMER (null URL)   → link creation pending/failed; retry link creation
     *  - FAILED (providerStatus=REQUEST_FAILED/HTTP_xxx/PARSE_ERROR) → link infrastructure
     *                                    failure; retry link creation
     *  - FAILED (payment declined)     → show payment-failed message to customer
     *  - EXPIRED                       → show expired message
     *  - SUCCEEDED                     → settlement already confirmed
     *
     * REPLACED attempts are excluded because they have been superseded; the replacement
     * is always newer in createdAt and will appear first in the result.
     */
    @Transactional(readOnly = true)
    public VibeCashPaymentAttemptDto getActiveStackingAttempt(Long storeId, Long tableId, Long settlementId) {
        return paymentAttemptRepository.findBySettlementRecordIdOrderByCreatedAtDesc(settlementId)
                .stream()
                .filter(a -> a.getStoreId().equals(storeId) && a.getTableId().equals(tableId))
                .filter(a -> !"REPLACED".equals(a.getAttemptStatus()))
                .findFirst()
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No attempt found for settlement " + settlementId));
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
                paymentStackingService.confirmStacking(paymentAttempt.getStoreId(), paymentAttempt.getTableId(), paymentAttempt.getSettlementRecordId());
            } else if ("payment.failed".equals(eventType)) {
                paymentAttempt.setAttemptStatus("FAILED");
                paymentAttemptRepository.save(paymentAttempt);
                log.info("Payment failed for settlement {}, keeping PENDING (switch-method window open)",
                        paymentAttempt.getSettlementRecordId());
            } else if ("checkout.session.expired".equals(eventType)) {
                paymentAttempt.setAttemptStatus("EXPIRED");
                paymentAttemptRepository.save(paymentAttempt);
                paymentStackingService.releaseStacking(paymentAttempt.getStoreId(), paymentAttempt.getTableId(), paymentAttempt.getSettlementRecordId(), "CHECKOUT_EXPIRED");
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

    /**
     * Marks an attempt FAILED in its own committed transaction.
     * Called from error paths that execute outside any open transaction.
     */
    private void markAttemptFailed(String attemptId, String providerStatus, String detail) {
        try {
            txTemplate.execute(txStatus -> {
                paymentAttemptRepository.findByPaymentAttemptId(attemptId).ifPresent(a -> {
                    a.setAttemptStatus("FAILED");
                    a.setProviderStatus(providerStatus);
                    a.setLastWebhookPayloadJson(detail);
                    a.setUpdatedAt(OffsetDateTime.now());
                    paymentAttemptRepository.save(a);
                });
                return null;
            });
        } catch (Exception ex) {
            log.error("Failed to mark attempt {} as FAILED: {}", attemptId, ex.getMessage());
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
