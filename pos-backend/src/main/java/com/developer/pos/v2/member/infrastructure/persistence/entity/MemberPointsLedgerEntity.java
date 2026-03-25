package com.developer.pos.v2.member.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity(name = "V2MemberPointsLedgerEntity")
@Table(name = "member_points_ledger")
public class MemberPointsLedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ledger_no", nullable = false)
    private String ledgerNo;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "change_type", nullable = false)
    private String changeType;

    @Column(name = "points_delta", nullable = false)
    private long pointsDelta;

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_ref")
    private String sourceRef;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getLedgerNo() {
        return ledgerNo;
    }

    public void setLedgerNo(String ledgerNo) {
        this.ledgerNo = ledgerNo;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public long getPointsDelta() {
        return pointsDelta;
    }

    public void setPointsDelta(long pointsDelta) {
        this.pointsDelta = pointsDelta;
    }

    public long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
