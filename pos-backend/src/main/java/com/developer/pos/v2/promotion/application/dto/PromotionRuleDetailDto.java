package com.developer.pos.v2.promotion.application.dto;

import java.time.OffsetDateTime;

public record PromotionRuleDetailDto(
        Long id,
        Long merchantId,
        Long storeId,
        String ruleCode,
        String ruleName,
        String ruleType,
        String ruleStatus,
        int priority,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String conditionType,
        Long thresholdAmountCents,
        String rewardType,
        Long discountAmountCents,
        Long giftSkuId,
        Integer giftQuantity
) {
}
