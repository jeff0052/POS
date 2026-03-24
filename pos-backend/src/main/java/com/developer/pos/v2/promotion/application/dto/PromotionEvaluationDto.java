package com.developer.pos.v2.promotion.application.dto;

import java.util.List;

public record PromotionEvaluationDto(
        String activeOrderId,
        String matchedRuleCode,
        String matchedRuleName,
        long originalAmountCents,
        long memberDiscountCents,
        long promotionDiscountCents,
        long payableAmountCents,
        List<GiftItemDto> giftItems
) {
    public record GiftItemDto(
            Long skuId,
            String skuName,
            int quantity
    ) {
    }
}
