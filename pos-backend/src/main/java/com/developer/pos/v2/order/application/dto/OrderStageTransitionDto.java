package com.developer.pos.v2.order.application.dto;

import java.util.List;

public record OrderStageTransitionDto(
        String activeOrderId,
        String status,
        List<Long> kitchenTicketIds
) {
    /** Backward-compat constructor for callers that don't have ticket IDs */
    public OrderStageTransitionDto(String activeOrderId, String status) {
        this(activeOrderId, status, List.of());
    }
}
