package com.developer.pos.v2.promotion.application.dto;

public record PromotionEvaluationDto(
        String activeOrderId,
        String matchedRuleCode,
        String matchedRuleName,
        long originalAmountCents,
        long memberDiscountCents,
        long promotionDiscountCents,
        long payableAmountCents
) {
}
