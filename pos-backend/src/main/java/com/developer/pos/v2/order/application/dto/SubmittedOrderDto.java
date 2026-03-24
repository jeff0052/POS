package com.developer.pos.v2.order.application.dto;

import java.util.List;

public record SubmittedOrderDto(
        String submittedOrderId,
        String orderNo,
        String sourceOrderType,
        String fulfillmentStatus,
        String settlementStatus,
        Long memberId,
        PricingDto pricing,
        List<ItemDto> items
) {
    public record PricingDto(
            long originalAmountCents,
            long memberDiscountCents,
            long promotionDiscountCents,
            long payableAmountCents
    ) {
    }

    public record ItemDto(
            Long skuId,
            String skuCode,
            String skuName,
            int quantity,
            long unitPriceCents,
            String remark,
            long lineTotalCents
    ) {
    }
}
