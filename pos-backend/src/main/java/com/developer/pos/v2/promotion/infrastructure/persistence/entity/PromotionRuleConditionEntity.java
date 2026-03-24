package com.developer.pos.v2.promotion.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2PromotionRuleConditionEntity")
@Table(name = "promotion_rule_conditions")
public class PromotionRuleConditionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "condition_type", nullable = false)
    private String conditionType;

    @Column(name = "threshold_amount_cents")
    private Long thresholdAmountCents;

    public PromotionRuleConditionEntity() {
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }

    public Long getThresholdAmountCents() {
        return thresholdAmountCents;
    }

    public void setThresholdAmountCents(Long thresholdAmountCents) {
        this.thresholdAmountCents = thresholdAmountCents;
    }
}
