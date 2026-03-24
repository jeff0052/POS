package com.developer.pos.v2.order.application.dto;

public record OrderStageTransitionDto(
        String activeOrderId,
        String status
) {
}
