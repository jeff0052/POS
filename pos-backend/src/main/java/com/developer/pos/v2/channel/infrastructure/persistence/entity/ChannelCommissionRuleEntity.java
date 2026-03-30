package com.developer.pos.v2.channel.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity(name = "V2ChannelCommissionRuleEntity")
@Table(name = "channel_commission_rules")
public class ChannelCommissionRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Column(name = "rule_code", nullable = false)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "commission_type", nullable = false)
    private String commissionType;

    @Column(name = "commission_rate_percent", precision = 7, scale = 4)
    private BigDecimal commissionRatePercent;

    @Column(name = "commission_fixed_cents")
    private Long commissionFixedCents;

    @Column(name = "tier_rules_json", columnDefinition = "JSON")
    private String tierRulesJson;

    @Column(name = "calculation_base")
    private String calculationBase;

    @Column(name = "applicable_stores", columnDefinition = "JSON")
    private String applicableStores;

    @Column(name = "applicable_categories", columnDefinition = "JSON")
    private String applicableCategories;

    @Column(name = "applicable_dining_modes", columnDefinition = "JSON")
    private String applicableDiningModes;

    @Column(name = "min_commission_cents")
    private Long minCommissionCents;

    @Column(name = "max_commission_cents")
    private Long maxCommissionCents;

    @Column(name = "starts_at")
    private OffsetDateTime startsAt;

    @Column(name = "ends_at")
    private OffsetDateTime endsAt;

    @Column(name = "rule_status", nullable = false)
    private String ruleStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public ChannelCommissionRuleEntity() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getCommissionType() { return commissionType; }
    public void setCommissionType(String commissionType) { this.commissionType = commissionType; }
    public BigDecimal getCommissionRatePercent() { return commissionRatePercent; }
    public void setCommissionRatePercent(BigDecimal commissionRatePercent) { this.commissionRatePercent = commissionRatePercent; }
    public Long getCommissionFixedCents() { return commissionFixedCents; }
    public void setCommissionFixedCents(Long commissionFixedCents) { this.commissionFixedCents = commissionFixedCents; }
    public String getTierRulesJson() { return tierRulesJson; }
    public void setTierRulesJson(String tierRulesJson) { this.tierRulesJson = tierRulesJson; }
    public String getCalculationBase() { return calculationBase; }
    public void setCalculationBase(String calculationBase) { this.calculationBase = calculationBase; }
    public String getApplicableStores() { return applicableStores; }
    public void setApplicableStores(String applicableStores) { this.applicableStores = applicableStores; }
    public String getApplicableCategories() { return applicableCategories; }
    public void setApplicableCategories(String applicableCategories) { this.applicableCategories = applicableCategories; }
    public String getApplicableDiningModes() { return applicableDiningModes; }
    public void setApplicableDiningModes(String applicableDiningModes) { this.applicableDiningModes = applicableDiningModes; }
    public Long getMinCommissionCents() { return minCommissionCents; }
    public void setMinCommissionCents(Long minCommissionCents) { this.minCommissionCents = minCommissionCents; }
    public Long getMaxCommissionCents() { return maxCommissionCents; }
    public void setMaxCommissionCents(Long maxCommissionCents) { this.maxCommissionCents = maxCommissionCents; }
    public OffsetDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(OffsetDateTime startsAt) { this.startsAt = startsAt; }
    public OffsetDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(OffsetDateTime endsAt) { this.endsAt = endsAt; }
    public String getRuleStatus() { return ruleStatus; }
    public void setRuleStatus(String ruleStatus) { this.ruleStatus = ruleStatus; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
