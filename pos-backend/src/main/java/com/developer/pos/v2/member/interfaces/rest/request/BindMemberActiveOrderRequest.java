package com.developer.pos.v2.member.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

public record BindMemberActiveOrderRequest(
        @NotBlank String activeOrderId
) {
}
