package com.developer.pos.v2.staff.interfaces.rest.request;

import jakarta.validation.constraints.Min;

public record CloseShiftRequest(
        @Min(0) long closingCashCents,
        String closingNote
) {
}
