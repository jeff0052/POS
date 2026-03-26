package com.developer.pos.v2.staff.interfaces.rest.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OpenShiftRequest(
        @NotNull Long cashierId,
        @NotBlank String cashierName,
        @Min(0) long openingFloatCents
) {
}
