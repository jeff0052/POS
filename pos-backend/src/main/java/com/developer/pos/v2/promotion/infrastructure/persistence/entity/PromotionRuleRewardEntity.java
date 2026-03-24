package com.developer.pos.v2.promotion.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2PromotionRuleRewardEntity")
@Table(name = "promotion_rule_rewards")
public class PromotionRuleRewardEntity {

    @Id
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "reward_type", nullable = false)
    private String rewardType;

    @Column(name = "discount_amount_cents")
    private Long discountAmountCents;

    @Column(name = "gift_sku_id")
    private Long giftSkuId;

    @Column(name = "gift_quantity")
    private Integer giftQuantity;

    protected PromotionRuleRewardEntity() {
    }

    public Long getRuleId() {
        return ruleId;
    }

    public String getRewardType() {
        return rewardType;
    }

    public Long getDiscountAmountCents() {
        return discountAmountCents;
    }

    public Long getGiftSkuId() {
        return giftSkuId;
    }

    public Integer getGiftQuantity() {
        return giftQuantity;
    }
}
