package com.developer.pos.auth.controller;

import com.developer.pos.auth.dto.AuthUserDto;
import com.developer.pos.auth.dto.LoginRequest;
import com.developer.pos.auth.dto.LoginResponse;
import com.developer.pos.auth.service.AuthService;
import com.developer.pos.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping({"/api/v1/auth/login", "/api/v2/auth/login"})
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping({"/api/v1/auth/logout", "/api/v2/auth/logout"})
    public ApiResponse<Map<String, Boolean>> logout() {
        return ApiResponse.success(Map.of("success", true));
    }

    @org.springframework.beans.factory.annotation.Value("${auth.bootstrap.secret:}")
    private String bootstrapSecret;

    /**
     * Bootstrap: create the first admin user if no users exist.
     * Requires X-Bootstrap-Secret header matching auth.bootstrap.secret env var.
     * Only works when auth_users table is empty.
     */
    @PostMapping({"/api/v1/auth/bootstrap", "/api/v2/auth/bootstrap"})
    public ApiResponse<Map<String, String>> bootstrap(
            @RequestHeader(value = "X-Bootstrap-Secret", required = false) String providedSecret,
            @RequestBody Map<String, String> request
    ) {
        // Gate: bootstrap requires a secret to prevent public race condition
        if (bootstrapSecret == null || bootstrapSecret.isBlank()) {
            throw new IllegalStateException("Bootstrap is disabled. Set AUTH_BOOTSTRAP_SECRET to enable.");
        }
        if (providedSecret == null || !providedSecret.equals(bootstrapSecret)) {
            throw new SecurityException("Invalid bootstrap secret");
        }

        String username = request.get("username");
        String password = request.get("password");
        String displayName = request.getOrDefault("displayName", "Platform Admin");
        if (username == null || password == null || username.isBlank() || password.length() < 8) {
            throw new IllegalArgumentException("Username required, password must be at least 8 characters");
        }
        authService.bootstrapFirstAdmin(username, password, displayName);
        return ApiResponse.success(Map.of("message", "Admin user created. Please login."));
    }

    @GetMapping({"/api/v1/auth/me", "/api/v2/auth/me"})
    public ApiResponse<AuthUserDto> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalStateException("Not authenticated");
        }
        Long userId = Long.parseLong(auth.getPrincipal().toString());
        return ApiResponse.success(authService.getUser(userId));
    }
}
