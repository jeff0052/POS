package com.developer.pos.v2.member.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

public record MemberPointsAdjustmentRequest(
        String changeType,
        long pointsDelta,
        @NotBlank String source,
        @NotBlank String operatorName
) {
}
