package com.developer.pos.order.dto;

import java.util.List;

public record QrCurrentOrderResponse(
    String orderNo,
    String queueNo,
    String storeCode,
    String storeName,
    String tableCode,
    String settlementStatus,
    String memberName,
    String memberTier,
    Long originalAmountCents,
    Long memberDiscountCents,
    Long promotionDiscountCents,
    Long payableAmountCents,
    List<QrOrderItemRequest> items
) {
}
