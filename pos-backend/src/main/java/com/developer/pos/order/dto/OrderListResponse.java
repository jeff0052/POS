package com.developer.pos.order.dto;

import java.util.List;

public record OrderListResponse(
    List<OrderDto> list,
    Integer total
) {
}
