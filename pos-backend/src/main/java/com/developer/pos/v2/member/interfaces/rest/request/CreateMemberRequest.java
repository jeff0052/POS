package com.developer.pos.v2.member.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMemberRequest(
        @NotNull Long merchantId,
        @NotBlank String name,
        @NotBlank String phone,
        String tierCode
) {
}
