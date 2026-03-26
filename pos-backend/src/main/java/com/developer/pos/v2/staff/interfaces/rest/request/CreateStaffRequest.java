package com.developer.pos.v2.staff.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStaffRequest(
        @NotNull Long merchantId,
        @NotNull Long storeId,
        @NotBlank String staffName,
        @NotBlank String staffCode,
        @NotBlank String pin,
        String roleCode,
        String phone
) {
}
