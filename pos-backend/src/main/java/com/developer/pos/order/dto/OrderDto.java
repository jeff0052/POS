package com.developer.pos.order.dto;

import java.util.List;

public record OrderDto(
    Long id,
    String orderNo,
    Long paidAmountCents,
    String orderStatus,
    String paymentMethod,
    String createdAt,
    String cashier,
    String printStatus,
    List<OrderItemDto> items,
    String tableCode,
    String orderType,
    String memberName,
    String memberTier,
    Long originalAmountCents,
    Long memberDiscountCents,
    Long promotionDiscountCents,
    Long payableAmountCents,
    List<String> giftItems
) {
}
