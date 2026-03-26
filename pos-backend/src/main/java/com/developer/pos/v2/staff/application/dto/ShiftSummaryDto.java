package com.developer.pos.v2.staff.application.dto;

import java.time.OffsetDateTime;

public record ShiftSummaryDto(
        String shiftId,
        Long storeId,
        Long cashierId,
        String cashierName,
        String shiftStatus,
        long openingFloatCents,
        Long closingCashCents,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt,
        int settlementCount,
        long payableAmountCents,
        long collectedAmountCents
) {
}
