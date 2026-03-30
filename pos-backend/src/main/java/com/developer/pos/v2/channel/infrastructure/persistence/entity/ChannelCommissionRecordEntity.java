package com.developer.pos.v2.channel.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity(name = "V2ChannelCommissionRecordEntity")
@Table(name = "channel_commission_records")
public class ChannelCommissionRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "commission_no", nullable = false)
    private String commissionNo;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "submitted_order_id", nullable = false)
    private Long submittedOrderId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "order_amount_cents", nullable = false)
    private long orderAmountCents;

    @Column(name = "calculation_base_cents", nullable = false)
    private long calculationBaseCents;

    @Column(name = "commission_type", nullable = false)
    private String commissionType;

    @Column(name = "commission_rate_percent", precision = 7, scale = 4)
    private BigDecimal commissionRatePercent;

    @Column(name = "commission_fixed_cents")
    private Long commissionFixedCents;

    @Column(name = "commission_amount_cents", nullable = false)
    private long commissionAmountCents;

    @Column(name = "commission_status", nullable = false)
    private String commissionStatus;

    @Column(name = "settlement_batch_id")
    private Long settlementBatchId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public ChannelCommissionRecordEntity() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCommissionNo() { return commissionNo; }
    public void setCommissionNo(String commissionNo) { this.commissionNo = commissionNo; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public Long getSubmittedOrderId() { return submittedOrderId; }
    public void setSubmittedOrderId(Long submittedOrderId) { this.submittedOrderId = submittedOrderId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public long getOrderAmountCents() { return orderAmountCents; }
    public void setOrderAmountCents(long orderAmountCents) { this.orderAmountCents = orderAmountCents; }
    public long getCalculationBaseCents() { return calculationBaseCents; }
    public void setCalculationBaseCents(long calculationBaseCents) { this.calculationBaseCents = calculationBaseCents; }
    public String getCommissionType() { return commissionType; }
    public void setCommissionType(String commissionType) { this.commissionType = commissionType; }
    public BigDecimal getCommissionRatePercent() { return commissionRatePercent; }
    public void setCommissionRatePercent(BigDecimal commissionRatePercent) { this.commissionRatePercent = commissionRatePercent; }
    public Long getCommissionFixedCents() { return commissionFixedCents; }
    public void setCommissionFixedCents(Long commissionFixedCents) { this.commissionFixedCents = commissionFixedCents; }
    public long getCommissionAmountCents() { return commissionAmountCents; }
    public void setCommissionAmountCents(long commissionAmountCents) { this.commissionAmountCents = commissionAmountCents; }
    public String getCommissionStatus() { return commissionStatus; }
    public void setCommissionStatus(String commissionStatus) { this.commissionStatus = commissionStatus; }
    public Long getSettlementBatchId() { return settlementBatchId; }
    public void setSettlementBatchId(Long settlementBatchId) { this.settlementBatchId = settlementBatchId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
