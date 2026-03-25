package com.developer.pos.v2.order.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MerchantAdminOrderDto(
        String orderId,
        String orderNo,
        Long storeId,
        Long tableId,
        String tableCode,
        String orderType,
        String orderStatus,
        String paymentMethod,
        OffsetDateTime createdAt,
        String memberName,
        String memberTier,
        long originalAmountCents,
        long memberDiscountCents,
        long promotionDiscountCents,
        long payableAmountCents,
        List<String> giftItems,
        List<MerchantAdminOrderItemDto> items
) {
}
