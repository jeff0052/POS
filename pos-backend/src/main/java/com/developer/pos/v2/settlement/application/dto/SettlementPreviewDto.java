package com.developer.pos.v2.settlement.application.dto;

import com.developer.pos.v2.order.domain.status.ActiveOrderStatus;

import java.util.List;

public record SettlementPreviewDto(
        String activeOrderId,
        ActiveOrderStatus status,
        MemberPreviewDto member,
        PricingPreviewDto pricing,
        List<GiftItemDto> giftItems
) {
    public record MemberPreviewDto(
            Long id,
            String name,
            String tier
    ) {
    }

    public record PricingPreviewDto(
            long originalAmountCents,
            long memberDiscountCents,
            long promotionDiscountCents,
            long payableAmountCents
    ) {
    }

    public record GiftItemDto(
            String skuName,
            int quantity
    ) {
    }
}
