package com.developer.pos.order.dto;

public record OrderItemDto(
    String productName,
    Integer quantity,
    Long lineAmountCents
) {
}
