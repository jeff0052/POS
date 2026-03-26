package com.developer.pos.v2.staff.application.dto;

import java.time.OffsetDateTime;

public record ShiftClosedDto(
        String shiftId,
        String shiftStatus,
        long closingCashCents,
        String closingNote,
        OffsetDateTime closedAt
) {
}
