package com.developer.pos.v2.member.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateMemberRequest(
        @NotBlank String name,
        @NotBlank String phone,
        @NotBlank String tierCode,
        @NotBlank @Pattern(regexp = "ACTIVE|INACTIVE") String memberStatus
) {
}
