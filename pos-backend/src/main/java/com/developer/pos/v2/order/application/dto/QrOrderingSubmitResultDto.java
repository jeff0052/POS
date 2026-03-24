package com.developer.pos.v2.order.application.dto;

public record QrOrderingSubmitResultDto(
        String activeOrderId,
        String orderNo,
        String tableCode,
        String status,
        long payableAmountCents,
        int totalItemCount
) {
}
