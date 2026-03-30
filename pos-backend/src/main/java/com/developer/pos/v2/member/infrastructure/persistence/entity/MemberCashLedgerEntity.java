package com.developer.pos.v2.member.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity(name = "V2MemberCashLedgerEntity")
@Table(name = "member_cash_ledger")
public class MemberCashLedgerEntity {

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

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "balance_after_cents", nullable = false)
    private long balanceAfterCents;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "source_ref")
    private String sourceRef;

    @Column(name = "operator_type", nullable = false)
    private String operatorType;

    @Column(name = "operator_id")
    private String operatorId;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public MemberCashLedgerEntity() {
    }

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

    public long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    public long getBalanceAfterCents() {
        return balanceAfterCents;
    }

    public void setBalanceAfterCents(long balanceAfterCents) {
        this.balanceAfterCents = balanceAfterCents;
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

    public String getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
