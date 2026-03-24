package com.developer.pos.v2.promotion.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2PromotionRuleConditionEntity")
@Table(name = "promotion_rule_conditions")
public class PromotionRuleConditionEntity {

    @Id
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "condition_type", nullable = false)
    private String conditionType;

    @Column(name = "threshold_amount_cents")
    private Long thresholdAmountCents;

    protected PromotionRuleConditionEntity() {
    }

    public Long getRuleId() {
        return ruleId;
    }

    public String getConditionType() {
        return conditionType;
    }

    public Long getThresholdAmountCents() {
        return thresholdAmountCents;
    }
}
