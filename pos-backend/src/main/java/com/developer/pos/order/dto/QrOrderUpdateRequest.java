package com.developer.pos.order.dto;

import java.util.List;

public record QrOrderUpdateRequest(
    String storeCode,
    String tableCode,
    String settlementStatus,
    List<QrOrderItemRequest> items
) {
}
