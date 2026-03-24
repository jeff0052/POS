package com.developer.pos.v2.order.application.dto;

import com.developer.pos.v2.order.domain.source.OrderSource;
import com.developer.pos.v2.order.domain.status.ActiveOrderStatus;

import java.util.List;

public record ActiveTableOrderDto(
        String activeOrderId,
        String orderNo,
        Long storeId,
        Long tableId,
        String tableCode,
        OrderSource orderSource,
        ActiveOrderStatus status,
        Long memberId,
        List<ActiveTableOrderItemDto> items,
        PricingDto pricing
) {
    public record ActiveTableOrderItemDto(
            Long skuId,
            String skuCode,
            String skuName,
            int quantity,
            long unitPriceCents,
            String remark,
            long lineTotalCents
    ) {
    }

    public record PricingDto(
            long originalAmountCents,
            long memberDiscountCents,
            long promotionDiscountCents,
            long payableAmountCents
    ) {
    }
}
