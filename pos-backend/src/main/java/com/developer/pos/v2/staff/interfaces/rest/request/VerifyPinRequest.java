package com.developer.pos.v2.staff.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerifyPinRequest(
        @NotNull Long storeId,
        @NotBlank String staffCode,
        @NotBlank String pin
) {
}
