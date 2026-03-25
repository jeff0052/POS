package com.developer.pos.v2.report.application.dto;

public record OrderStateIssueDto(
        String issueCode,
        String severity,
        Long tableId,
        String tableCode,
        String tableStatus,
        String activeOrderId,
        String activeOrderStatus,
        String sessionId,
        String sessionStatus,
        int unsettledSubmittedOrderCount,
        String message
) {
}
