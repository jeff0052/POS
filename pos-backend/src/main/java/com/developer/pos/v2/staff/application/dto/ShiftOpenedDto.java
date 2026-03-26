package com.developer.pos.v2.staff.application.dto;

import java.time.OffsetDateTime;

public record ShiftOpenedDto(
        String shiftId,
        Long storeId,
        Long cashierId,
        String cashierName,
        String shiftStatus,
        long openingFloatCents,
        OffsetDateTime openedAt
) {
}
