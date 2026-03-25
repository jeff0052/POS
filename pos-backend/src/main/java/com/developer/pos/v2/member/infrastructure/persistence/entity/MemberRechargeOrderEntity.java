package com.developer.pos.v2.member.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity(name = "V2MemberRechargeOrderEntity")
@Table(name = "member_recharge_orders")
public class MemberRechargeOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recharge_no", nullable = false)
    private String rechargeNo;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "bonus_amount_cents", nullable = false)
    private long bonusAmountCents;

    @Column(name = "final_status", nullable = false)
    private String finalStatus;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getRechargeNo() {
        return rechargeNo;
    }

    public void setRechargeNo(String rechargeNo) {
        this.rechargeNo = rechargeNo;
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

    public long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    public long getBonusAmountCents() {
        return bonusAmountCents;
    }

    public void setBonusAmountCents(long bonusAmountCents) {
        this.bonusAmountCents = bonusAmountCents;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
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
