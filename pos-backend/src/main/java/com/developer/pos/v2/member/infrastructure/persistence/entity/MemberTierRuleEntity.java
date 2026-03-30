package com.developer.pos.v2.member.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "V2MemberTierRuleEntity")
@Table(name = "member_tier_rules")
public class MemberTierRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "tier_code", nullable = false)
    private String tierCode;

    @Column(name = "tier_name", nullable = false)
    private String tierName;

    @Column(name = "tier_level", nullable = false)
    private int tierLevel;

    @Column(name = "upgrade_type", nullable = false)
    private String upgradeType;

    @Column(name = "upgrade_threshold_cents", nullable = false)
    private long upgradeThresholdCents;

    @Column(name = "downgrade_enabled", nullable = false)
    private boolean downgradeEnabled;

    @Column(name = "downgrade_period_months", nullable = false)
    private int downgradePeriodMonths;

    @Column(name = "downgrade_threshold_cents")
    private Long downgradeThresholdCents;

    @Column(name = "points_multiplier", nullable = false)
    private BigDecimal pointsMultiplier;

    @Column(name = "discount_percent", nullable = false)
    private int discountPercent;

    @Column(name = "birthday_bonus_points", nullable = false)
    private long birthdayBonusPoints;

    @Column(name = "free_delivery", nullable = false)
    private boolean freeDelivery;

    @Column(name = "tier_icon")
    private String tierIcon;

    @Column(name = "tier_color")
    private String tierColor;

    @Column(name = "tier_description")
    private String tierDescription;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public MemberTierRuleEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public String getTierCode() {
        return tierCode;
    }

    public void setTierCode(String tierCode) {
        this.tierCode = tierCode;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public int getTierLevel() {
        return tierLevel;
    }

    public void setTierLevel(int tierLevel) {
        this.tierLevel = tierLevel;
    }

    public String getUpgradeType() {
        return upgradeType;
    }

    public void setUpgradeType(String upgradeType) {
        this.upgradeType = upgradeType;
    }

    public long getUpgradeThresholdCents() {
        return upgradeThresholdCents;
    }

    public void setUpgradeThresholdCents(long upgradeThresholdCents) {
        this.upgradeThresholdCents = upgradeThresholdCents;
    }

    public boolean isDowngradeEnabled() {
        return downgradeEnabled;
    }

    public void setDowngradeEnabled(boolean downgradeEnabled) {
        this.downgradeEnabled = downgradeEnabled;
    }

    public int getDowngradePeriodMonths() {
        return downgradePeriodMonths;
    }

    public void setDowngradePeriodMonths(int downgradePeriodMonths) {
        this.downgradePeriodMonths = downgradePeriodMonths;
    }

    public Long getDowngradeThresholdCents() {
        return downgradeThresholdCents;
    }

    public void setDowngradeThresholdCents(Long downgradeThresholdCents) {
        this.downgradeThresholdCents = downgradeThresholdCents;
    }

    public BigDecimal getPointsMultiplier() {
        return pointsMultiplier;
    }

    public void setPointsMultiplier(BigDecimal pointsMultiplier) {
        this.pointsMultiplier = pointsMultiplier;
    }

    public int getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public long getBirthdayBonusPoints() {
        return birthdayBonusPoints;
    }

    public void setBirthdayBonusPoints(long birthdayBonusPoints) {
        this.birthdayBonusPoints = birthdayBonusPoints;
    }

    public boolean isFreeDelivery() {
        return freeDelivery;
    }

    public void setFreeDelivery(boolean freeDelivery) {
        this.freeDelivery = freeDelivery;
    }

    public String getTierIcon() {
        return tierIcon;
    }

    public void setTierIcon(String tierIcon) {
        this.tierIcon = tierIcon;
    }

    public String getTierColor() {
        return tierColor;
    }

    public void setTierColor(String tierColor) {
        this.tierColor = tierColor;
    }

    public String getTierDescription() {
        return tierDescription;
    }

    public void setTierDescription(String tierDescription) {
        this.tierDescription = tierDescription;
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
