package com.developer.pos.v2.channel.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity(name = "V2ChannelSettlementBatchEntity")
@Table(name = "channel_settlement_batches")
public class ChannelSettlementBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_no", nullable = false)
    private String batchNo;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_orders", nullable = false)
    private int totalOrders;

    @Column(name = "total_order_amount_cents", nullable = false)
    private long totalOrderAmountCents;

    @Column(name = "total_commission_cents", nullable = false)
    private long totalCommissionCents;

    @Column(name = "adjustment_cents", nullable = false)
    private long adjustmentCents;

    @Column(name = "adjustment_reason")
    private String adjustmentReason;

    @Column(name = "final_settlement_cents", nullable = false)
    private long finalSettlementCents;

    @Column(name = "batch_status", nullable = false)
    private String batchStatus;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "paid_by")
    private Long paidBy;

    @Column(name = "payment_ref")
    private String paymentRef;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public ChannelSettlementBatchEntity() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
    public long getTotalOrderAmountCents() { return totalOrderAmountCents; }
    public void setTotalOrderAmountCents(long totalOrderAmountCents) { this.totalOrderAmountCents = totalOrderAmountCents; }
    public long getTotalCommissionCents() { return totalCommissionCents; }
    public void setTotalCommissionCents(long totalCommissionCents) { this.totalCommissionCents = totalCommissionCents; }
    public long getAdjustmentCents() { return adjustmentCents; }
    public void setAdjustmentCents(long adjustmentCents) { this.adjustmentCents = adjustmentCents; }
    public String getAdjustmentReason() { return adjustmentReason; }
    public void setAdjustmentReason(String adjustmentReason) { this.adjustmentReason = adjustmentReason; }
    public long getFinalSettlementCents() { return finalSettlementCents; }
    public void setFinalSettlementCents(long finalSettlementCents) { this.finalSettlementCents = finalSettlementCents; }
    public String getBatchStatus() { return batchStatus; }
    public void setBatchStatus(String batchStatus) { this.batchStatus = batchStatus; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(OffsetDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public Long getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Long confirmedBy) { this.confirmedBy = confirmedBy; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }
    public Long getPaidBy() { return paidBy; }
    public void setPaidBy(Long paidBy) { this.paidBy = paidBy; }
    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String paymentRef) { this.paymentRef = paymentRef; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
