package com.developer.pos.v2.promotion.application.dto;

public record PromotionRuleSummaryDto(
        Long id,
        String ruleCode,
        String ruleName,
        String ruleType,
        long thresholdAmountCents,
        long discountAmountCents,
        int priority
) {
}
