package com.developer.pos.auth.security;

public record AuthenticatedActor(
        Long userId,
        String username,
        String role,
        Long merchantId,
        Long storeId
) {}
