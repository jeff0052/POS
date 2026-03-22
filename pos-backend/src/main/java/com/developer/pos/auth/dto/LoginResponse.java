package com.developer.pos.auth.dto;

public record LoginResponse(
    String token,
    AuthUserDto user
) {
}
