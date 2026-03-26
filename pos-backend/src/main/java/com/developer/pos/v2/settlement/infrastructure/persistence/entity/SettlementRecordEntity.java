package com.developer.pos.v2.settlement.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "settlement_records")
public class SettlementRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_no", nullable = false)
    private String settlementNo;

    @Column(name = "active_order_id", nullable = false)
    private String activeOrderId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "final_status", nullable = false)
    private String finalStatus;

    @Column(name = "payable_amount_cents", nullable = false)
    private long payableAmountCents;

    @Column(name = "collected_amount_cents", nullable = false)
    private long collectedAmountCents;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getSettlementNo() {
        return settlementNo;
    }

    public void setSettlementNo(String settlementNo) {
        this.settlementNo = settlementNo;
    }

    public String getActiveOrderId() {
        return activeOrderId;
    }

    public void setActiveOrderId(String activeOrderId) {
        this.activeOrderId = activeOrderId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
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

    public Long getCashierId() {
        return cashierId;
    }

    public void setCashierId(Long cashierId) {
        this.cashierId = cashierId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
    }

    public long getPayableAmountCents() {
        return payableAmountCents;
    }

    public void setPayableAmountCents(long payableAmountCents) {
        this.payableAmountCents = payableAmountCents;
    }

    public long getCollectedAmountCents() {
        return collectedAmountCents;
    }

    public void setCollectedAmountCents(long collectedAmountCents) {
        this.collectedAmountCents = collectedAmountCents;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
