package com.developer.pos.v2.member.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

public record CreateMemberRequest(
        @NotBlank String name,
        @NotBlank String phone,
        String tierCode
) {
}
