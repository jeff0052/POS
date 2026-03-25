package com.developer.pos.v2.settlement.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_attempt_id", nullable = false)
    private String paymentAttemptId;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "payment_scheme", nullable = false)
    private String paymentScheme;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "table_session_id", nullable = false)
    private Long tableSessionId;

    @Column(name = "session_ref", nullable = false)
    private String sessionRef;

    @Column(name = "settlement_amount_cents", nullable = false)
    private long settlementAmountCents;

    @Column(name = "currency_code", nullable = false)
    private String currencyCode;

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @Column(name = "provider_checkout_url")
    private String providerCheckoutUrl;

    @Column(name = "provider_status")
    private String providerStatus;

    @Column(name = "attempt_status", nullable = false)
    private String attemptStatus;

    @Column(name = "webhook_event_type")
    private String webhookEventType;

    @Column(name = "last_webhook_payload_json")
    private String lastWebhookPayloadJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public Long getId() {
        return id;
    }

    public String getPaymentAttemptId() {
        return paymentAttemptId;
    }

    public void setPaymentAttemptId(String paymentAttemptId) {
        this.paymentAttemptId = paymentAttemptId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(String paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public Long getTableSessionId() {
        return tableSessionId;
    }

    public void setTableSessionId(Long tableSessionId) {
        this.tableSessionId = tableSessionId;
    }

    public String getSessionRef() {
        return sessionRef;
    }

    public void setSessionRef(String sessionRef) {
        this.sessionRef = sessionRef;
    }

    public long getSettlementAmountCents() {
        return settlementAmountCents;
    }

    public void setSettlementAmountCents(long settlementAmountCents) {
        this.settlementAmountCents = settlementAmountCents;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }

    public String getProviderCheckoutUrl() {
        return providerCheckoutUrl;
    }

    public void setProviderCheckoutUrl(String providerCheckoutUrl) {
        this.providerCheckoutUrl = providerCheckoutUrl;
    }

    public String getProviderStatus() {
        return providerStatus;
    }

    public void setProviderStatus(String providerStatus) {
        this.providerStatus = providerStatus;
    }

    public String getAttemptStatus() {
        return attemptStatus;
    }

    public void setAttemptStatus(String attemptStatus) {
        this.attemptStatus = attemptStatus;
    }

    public String getWebhookEventType() {
        return webhookEventType;
    }

    public void setWebhookEventType(String webhookEventType) {
        this.webhookEventType = webhookEventType;
    }

    public String getLastWebhookPayloadJson() {
        return lastWebhookPayloadJson;
    }

    public void setLastWebhookPayloadJson(String lastWebhookPayloadJson) {
        this.lastWebhookPayloadJson = lastWebhookPayloadJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
