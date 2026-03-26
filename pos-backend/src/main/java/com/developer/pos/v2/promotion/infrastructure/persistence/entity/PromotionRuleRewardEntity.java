package com.developer.pos.v2.promotion.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2PromotionRuleRewardEntity")
@Table(name = "promotion_rule_rewards")
public class PromotionRuleRewardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "reward_type", nullable = false)
    private String rewardType;

    @Column(name = "discount_amount_cents")
    private Long discountAmountCents;

    @Column(name = "discount_percent")
    private Integer discountPercent;

    @Column(name = "gift_sku_id")
    private Long giftSkuId;

    @Column(name = "gift_quantity")
    private Integer giftQuantity;

    public PromotionRuleRewardEntity() {
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRewardType() {
        return rewardType;
    }

    public void setRewardType(String rewardType) {
        this.rewardType = rewardType;
    }

    public Long getDiscountAmountCents() {
        return discountAmountCents;
    }

    public void setDiscountAmountCents(Long discountAmountCents) {
        this.discountAmountCents = discountAmountCents;
    }

    public Integer getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(Integer discountPercent) {
        this.discountPercent = discountPercent;
    }

    public Long getGiftSkuId() {
        return giftSkuId;
    }

    public void setGiftSkuId(Long giftSkuId) {
        this.giftSkuId = giftSkuId;
    }

    public Integer getGiftQuantity() {
        return giftQuantity;
    }

    public void setGiftQuantity(Integer giftQuantity) {
        this.giftQuantity = giftQuantity;
    }
}
