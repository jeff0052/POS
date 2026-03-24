package com.developer.pos.order.dto;

public record QrOrderItemRequest(
    Long productId,
    String productName,
    Integer quantity,
    Long unitPriceCents,
    Long memberPriceCents
) {
}
