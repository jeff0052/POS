package com.developer.pos.auth.dto;

public record AuthUserDto(
    Long id,
    String username,
    String displayName,
    String role,
    Long merchantId,
    Long storeId
) {
}
