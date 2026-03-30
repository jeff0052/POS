package com.developer.pos.v2.member.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "V2PointsRuleEntity")
@Table(name = "points_rules")
public class PointsRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "rule_code", nullable = false)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "rule_type", nullable = false)
    private String ruleType;

    @Column(name = "points_per_dollar", nullable = false)
    private int pointsPerDollar;

    @Column(name = "bonus_multiplier", nullable = false)
    private BigDecimal bonusMultiplier;

    @Column(name = "fixed_points")
    private Long fixedPoints;

    @Column(name = "min_spend_cents", nullable = false)
    private long minSpendCents;

    @Column(name = "max_points_per_order")
    private Long maxPointsPerOrder;

    @Column(name = "max_points_per_day")
    private Long maxPointsPerDay;

    @Column(name = "applicable_tiers", columnDefinition = "JSON")
    private String applicableTiers;

    @Column(name = "applicable_stores", columnDefinition = "JSON")
    private String applicableStores;

    @Column(name = "applicable_categories", columnDefinition = "JSON")
    private String applicableCategories;

    @Column(name = "applicable_dining_modes", columnDefinition = "JSON")
    private String applicableDiningModes;

    @Column(name = "applicable_days", columnDefinition = "JSON")
    private String applicableDays;

    @Column(name = "applicable_time_slots", columnDefinition = "JSON")
    private String applicableTimeSlots;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "rule_status", nullable = false)
    private String ruleStatus;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PointsRuleEntity() {
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

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(String ruleType) {
        this.ruleType = ruleType;
    }

    public int getPointsPerDollar() {
        return pointsPerDollar;
    }

    public void setPointsPerDollar(int pointsPerDollar) {
        this.pointsPerDollar = pointsPerDollar;
    }

    public BigDecimal getBonusMultiplier() {
        return bonusMultiplier;
    }

    public void setBonusMultiplier(BigDecimal bonusMultiplier) {
        this.bonusMultiplier = bonusMultiplier;
    }

    public Long getFixedPoints() {
        return fixedPoints;
    }

    public void setFixedPoints(Long fixedPoints) {
        this.fixedPoints = fixedPoints;
    }

    public long getMinSpendCents() {
        return minSpendCents;
    }

    public void setMinSpendCents(long minSpendCents) {
        this.minSpendCents = minSpendCents;
    }

    public Long getMaxPointsPerOrder() {
        return maxPointsPerOrder;
    }

    public void setMaxPointsPerOrder(Long maxPointsPerOrder) {
        this.maxPointsPerOrder = maxPointsPerOrder;
    }

    public Long getMaxPointsPerDay() {
        return maxPointsPerDay;
    }

    public void setMaxPointsPerDay(Long maxPointsPerDay) {
        this.maxPointsPerDay = maxPointsPerDay;
    }

    public String getApplicableTiers() {
        return applicableTiers;
    }

    public void setApplicableTiers(String applicableTiers) {
        this.applicableTiers = applicableTiers;
    }

    public String getApplicableStores() {
        return applicableStores;
    }

    public void setApplicableStores(String applicableStores) {
        this.applicableStores = applicableStores;
    }

    public String getApplicableCategories() {
        return applicableCategories;
    }

    public void setApplicableCategories(String applicableCategories) {
        this.applicableCategories = applicableCategories;
    }

    public String getApplicableDiningModes() {
        return applicableDiningModes;
    }

    public void setApplicableDiningModes(String applicableDiningModes) {
        this.applicableDiningModes = applicableDiningModes;
    }

    public String getApplicableDays() {
        return applicableDays;
    }

    public void setApplicableDays(String applicableDays) {
        this.applicableDays = applicableDays;
    }

    public String getApplicableTimeSlots() {
        return applicableTimeSlots;
    }

    public void setApplicableTimeSlots(String applicableTimeSlots) {
        this.applicableTimeSlots = applicableTimeSlots;
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

    public String getRuleStatus() {
        return ruleStatus;
    }

    public void setRuleStatus(String ruleStatus) {
        this.ruleStatus = ruleStatus;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
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
