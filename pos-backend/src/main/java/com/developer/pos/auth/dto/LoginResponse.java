package com.developer.pos.auth.dto;

import java.util.List;
import java.util.Set;

public record LoginResponse(
    String token,
    AuthUserDto user,
    Set<String> permissions,
    List<String> roles
) {
    /** Backward-compatible constructor without permissions */
    public LoginResponse(String token, AuthUserDto user) {
        this(token, user, Set.of(), List.of());
    }
}
