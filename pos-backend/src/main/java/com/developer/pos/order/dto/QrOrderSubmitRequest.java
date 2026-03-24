package com.developer.pos.order.dto;

import java.util.List;

public record QrOrderSubmitRequest(
    String storeCode,
    String storeName,
    String tableCode,
    Boolean memberBound,
    String memberName,
    String memberTier,
    String memberPhone,
    List<QrOrderItemRequest> items
) {
}
