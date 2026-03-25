package com.developer.pos.v2.report.application.dto;

import java.util.List;

public record OrderStateMonitorDto(
        Long storeId,
        long tableCount,
        long availableTableCount,
        long orderingTableCount,
        long diningTableCount,
        long paymentPendingTableCount,
        long cleaningTableCount,
        long openSessionCount,
        long activeDraftCount,
        long activeSubmittedCount,
        long unsettledSubmittedOrderCount,
        int issueCount,
        List<OrderStateIssueDto> issues
) {
}
