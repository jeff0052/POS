package com.developer.pos.v2.settlement.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "refund_records")
public class RefundRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "refund_no", nullable = false, unique = true)
    private String refundNo;

    @Column(name = "settlement_id", nullable = false)
    private Long settlementId;

    @Column(name = "settlement_no", nullable = false)
    private String settlementNo;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "refund_amount_cents", nullable = false)
    private long refundAmountCents;

    @Column(name = "refund_type", nullable = false)
    private String refundType;

    @Column(name = "refund_reason")
    private String refundReason;

    @Column(name = "refund_status", nullable = false)
    private String refundStatus;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "operated_by")
    private Long operatedBy;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public String getRefundNo() { return refundNo; }
    public void setRefundNo(String refundNo) { this.refundNo = refundNo; }
    public Long getSettlementId() { return settlementId; }
    public void setSettlementId(Long settlementId) { this.settlementId = settlementId; }
    public String getSettlementNo() { return settlementNo; }
    public void setSettlementNo(String settlementNo) { this.settlementNo = settlementNo; }
    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public long getRefundAmountCents() { return refundAmountCents; }
    public void setRefundAmountCents(long refundAmountCents) { this.refundAmountCents = refundAmountCents; }
    public String getRefundType() { return refundType; }
    public void setRefundType(String refundType) { this.refundType = refundType; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public Long getOperatedBy() { return operatedBy; }
    public void setOperatedBy(Long operatedBy) { this.operatedBy = operatedBy; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
