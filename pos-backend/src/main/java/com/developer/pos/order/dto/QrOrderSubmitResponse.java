package com.developer.pos.order.dto;

public record QrOrderSubmitResponse(
    String orderNo,
    String queueNo,
    String storeCode,
    String storeName,
    String tableCode,
    String orderType,
    String settlementStatus,
    String memberName,
    String memberTier,
    Long originalAmountCents,
    Long memberDiscountCents,
    Long promotionDiscountCents,
    Long payableAmountCents
) {
}
