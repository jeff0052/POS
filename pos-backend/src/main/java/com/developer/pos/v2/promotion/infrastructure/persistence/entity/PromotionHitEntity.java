package com.developer.pos.v2.promotion.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2PromotionHitEntity")
@Table(name = "promotion_hits")
public class PromotionHitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "active_order_id", nullable = false)
    private Long activeOrderDbId;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "rule_code", nullable = false)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(name = "discount_amount_cents", nullable = false)
    private long discountAmountCents;

    @Column(name = "gift_snapshot_json")
    private String giftSnapshotJson;

    public void setActiveOrderDbId(Long activeOrderDbId) {
        this.activeOrderDbId = activeOrderDbId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public void setDiscountAmountCents(long discountAmountCents) {
        this.discountAmountCents = discountAmountCents;
    }

    public void setGiftSnapshotJson(String giftSnapshotJson) {
        this.giftSnapshotJson = giftSnapshotJson;
    }
}
