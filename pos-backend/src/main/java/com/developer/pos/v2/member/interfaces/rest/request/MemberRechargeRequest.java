package com.developer.pos.v2.member.interfaces.rest.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record MemberRechargeRequest(
        @Min(1) long amountCents,
        @Min(0) long bonusAmountCents,
        @NotBlank String operatorName
) {
}
