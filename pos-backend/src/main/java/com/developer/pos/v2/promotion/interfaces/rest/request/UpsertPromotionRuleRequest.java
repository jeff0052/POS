package com.developer.pos.v2.promotion.interfaces.rest.request;

import java.time.OffsetDateTime;

public class UpsertPromotionRuleRequest {

    private Long merchantId;
    private Long storeId;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private String ruleStatus;
    private Integer priority;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private String conditionType;
    private Long thresholdAmountCents;
    private String rewardType;
    private Long discountAmountCents;
    private Long giftSkuId;
    private Integer giftQuantity;

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
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

    public String getRuleStatus() {
        return ruleStatus;
    }

    public void setRuleStatus(String ruleStatus) {
        this.ruleStatus = ruleStatus;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(OffsetDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(OffsetDateTime endsAt) {
        this.endsAt = endsAt;
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
