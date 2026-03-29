package com.developer.pos.v2.settlement.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "settlement_payment_holds")
public class SettlementPaymentHoldEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hold_no", nullable = false)
    private String holdNo;

    @Column(name = "settlement_record_id")
    private Long settlementRecordId;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "table_session_id")
    private Long tableSessionId;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "hold_type", nullable = false)
    private String holdType;

    @Column(name = "hold_amount_cents", nullable = false)
    private long holdAmountCents;

    @Column(name = "points_held")
    private Long pointsHeld;

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "hold_ref")
    private String holdRef;

    @Column(name = "hold_status", nullable = false)
    private String holdStatus;

    @Column(name = "payment_attempt_id")
    private Long paymentAttemptId;

    @Column(name = "held_at", nullable = false)
    private OffsetDateTime heldAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(name = "release_reason")
    private String releaseReason;

    public Long getId() { return id; }
    public String getHoldNo() { return holdNo; }
    public void setHoldNo(String holdNo) { this.holdNo = holdNo; }
    public Long getSettlementRecordId() { return settlementRecordId; }
    public void setSettlementRecordId(Long settlementRecordId) { this.settlementRecordId = settlementRecordId; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getTableSessionId() { return tableSessionId; }
    public void setTableSessionId(Long tableSessionId) { this.tableSessionId = tableSessionId; }
    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
    public String getHoldType() { return holdType; }
    public void setHoldType(String holdType) { this.holdType = holdType; }
    public long getHoldAmountCents() { return holdAmountCents; }
    public void setHoldAmountCents(long holdAmountCents) { this.holdAmountCents = holdAmountCents; }
    public Long getPointsHeld() { return pointsHeld; }
    public void setPointsHeld(Long pointsHeld) { this.pointsHeld = pointsHeld; }
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public String getHoldRef() { return holdRef; }
    public void setHoldRef(String holdRef) { this.holdRef = holdRef; }
    public String getHoldStatus() { return holdStatus; }
    public void setHoldStatus(String holdStatus) { this.holdStatus = holdStatus; }
    public Long getPaymentAttemptId() { return paymentAttemptId; }
    public void setPaymentAttemptId(Long paymentAttemptId) { this.paymentAttemptId = paymentAttemptId; }
    public OffsetDateTime getHeldAt() { return heldAt; }
    public void setHeldAt(OffsetDateTime heldAt) { this.heldAt = heldAt; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(OffsetDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public OffsetDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(OffsetDateTime releasedAt) { this.releasedAt = releasedAt; }
    public String getReleaseReason() { return releaseReason; }
    public void setReleaseReason(String releaseReason) { this.releaseReason = releaseReason; }
}
