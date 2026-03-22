package com.developer.pos.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank String clientType
) {
}
