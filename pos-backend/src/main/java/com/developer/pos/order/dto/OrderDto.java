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
    List<OrderItemDto> items
) {
}
