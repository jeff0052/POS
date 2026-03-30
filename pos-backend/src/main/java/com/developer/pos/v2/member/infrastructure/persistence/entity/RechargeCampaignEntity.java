package com.developer.pos.v2.member.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity(name = "V2RechargeCampaignEntity")
@Table(name = "recharge_campaigns")
public class RechargeCampaignEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "campaign_code", nullable = false)
    private String campaignCode;

    @Column(name = "campaign_name", nullable = false)
    private String campaignName;

    @Column(name = "recharge_amount_cents", nullable = false)
    private long rechargeAmountCents;

    @Column(name = "bonus_amount_cents", nullable = false)
    private long bonusAmountCents;

    @Column(name = "bonus_points", nullable = false)
    private long bonusPoints;

    @Column(name = "bonus_coupon_template_id")
    private Long bonusCouponTemplateId;

    @Column(name = "min_tier_level", nullable = false)
    private int minTierLevel;

    @Column(name = "max_per_member", nullable = false)
    private int maxPerMember;

    @Column(name = "total_quota", nullable = false)
    private int totalQuota;

    @Column(name = "used_quota", nullable = false)
    private int usedQuota;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "campaign_status", nullable = false)
    private String campaignStatus;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public RechargeCampaignEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getCampaignCode() {
        return campaignCode;
    }

    public void setCampaignCode(String campaignCode) {
        this.campaignCode = campaignCode;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public long getRechargeAmountCents() {
        return rechargeAmountCents;
    }

    public void setRechargeAmountCents(long rechargeAmountCents) {
        this.rechargeAmountCents = rechargeAmountCents;
    }

    public long getBonusAmountCents() {
        return bonusAmountCents;
    }

    public void setBonusAmountCents(long bonusAmountCents) {
        this.bonusAmountCents = bonusAmountCents;
    }

    public long getBonusPoints() {
        return bonusPoints;
    }

    public void setBonusPoints(long bonusPoints) {
        this.bonusPoints = bonusPoints;
    }

    public Long getBonusCouponTemplateId() {
        return bonusCouponTemplateId;
    }

    public void setBonusCouponTemplateId(Long bonusCouponTemplateId) {
        this.bonusCouponTemplateId = bonusCouponTemplateId;
    }

    public int getMinTierLevel() {
        return minTierLevel;
    }

    public void setMinTierLevel(int minTierLevel) {
        this.minTierLevel = minTierLevel;
    }

    public int getMaxPerMember() {
        return maxPerMember;
    }

    public void setMaxPerMember(int maxPerMember) {
        this.maxPerMember = maxPerMember;
    }

    public int getTotalQuota() {
        return totalQuota;
    }

    public void setTotalQuota(int totalQuota) {
        this.totalQuota = totalQuota;
    }

    public int getUsedQuota() {
        return usedQuota;
    }

    public void setUsedQuota(int usedQuota) {
        this.usedQuota = usedQuota;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(LocalDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public String getCampaignStatus() {
        return campaignStatus;
    }

    public void setCampaignStatus(String campaignStatus) {
        this.campaignStatus = campaignStatus;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
