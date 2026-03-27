package com.developer.pos.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthContext {
    public static AuthenticatedActor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedActor)) {
            throw new IllegalStateException("No authenticated actor in security context");
        }
        return (AuthenticatedActor) auth.getPrincipal();
    }
}
