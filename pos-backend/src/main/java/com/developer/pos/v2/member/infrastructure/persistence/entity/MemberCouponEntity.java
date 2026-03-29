package com.developer.pos.v2.member.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity(name = "V2MemberCouponEntity")
@Table(name = "member_coupons")
public class MemberCouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_no", nullable = false)
    private String couponNo;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "coupon_status", nullable = false)
    private String couponStatus;

    @Column(name = "lock_version", nullable = false)
    private int lockVersion;

    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    @Column(name = "locked_by_session")
    private Long lockedBySession;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "used_order_id")
    private String usedOrderId;

    @Column(name = "used_store_id")
    private Long usedStoreId;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private OffsetDateTime validUntil;

    public Long getId() { return id; }
    public String getCouponNo() { return couponNo; }
    public void setCouponNo(String couponNo) { this.couponNo = couponNo; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getCouponStatus() { return couponStatus; }
    public void setCouponStatus(String couponStatus) { this.couponStatus = couponStatus; }
    public int getLockVersion() { return lockVersion; }
    public void setLockVersion(int lockVersion) { this.lockVersion = lockVersion; }
    public OffsetDateTime getLockedAt() { return lockedAt; }
    public void setLockedAt(OffsetDateTime lockedAt) { this.lockedAt = lockedAt; }
    public Long getLockedBySession() { return lockedBySession; }
    public void setLockedBySession(Long lockedBySession) { this.lockedBySession = lockedBySession; }
    public OffsetDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(OffsetDateTime usedAt) { this.usedAt = usedAt; }
    public String getUsedOrderId() { return usedOrderId; }
    public void setUsedOrderId(String usedOrderId) { this.usedOrderId = usedOrderId; }
    public Long getUsedStoreId() { return usedStoreId; }
    public void setUsedStoreId(Long usedStoreId) { this.usedStoreId = usedStoreId; }
    public OffsetDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(OffsetDateTime validFrom) { this.validFrom = validFrom; }
    public OffsetDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(OffsetDateTime validUntil) { this.validUntil = validUntil; }
}
