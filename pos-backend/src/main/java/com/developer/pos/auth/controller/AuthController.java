package com.developer.pos.auth.controller;

import com.developer.pos.auth.dto.AuthUserDto;
import com.developer.pos.auth.dto.LoginRequest;
import com.developer.pos.auth.dto.LoginResponse;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.auth.service.AuthService;
import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.rbac.application.dto.RbacLoginResponse;
import com.developer.pos.v2.rbac.application.service.RbacAuthService;
import com.developer.pos.v2.rbac.interfaces.rest.request.PinLoginRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AuthController {

    private final AuthService authService;
    private final RbacAuthService rbacAuthService;

    public AuthController(AuthService authService, RbacAuthService rbacAuthService) {
        this.authService = authService;
        this.rbacAuthService = rbacAuthService;
    }

    @PostMapping({"/api/v1/auth/login", "/api/v2/auth/login"})
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/api/v2/auth/pin-login")
    public ApiResponse<RbacLoginResponse> pinLogin(@Valid @RequestBody PinLoginRequest request) {
        return ApiResponse.success(rbacAuthService.pinLogin(request.getStoreId(), request.getUserCode(), request.getPin()));
    }

    @PostMapping({"/api/v1/auth/logout", "/api/v2/auth/logout"})
    public ApiResponse<Map<String, Boolean>> logout() {
        return ApiResponse.success(Map.of("success", true));
    }

    @org.springframework.beans.factory.annotation.Value("${auth.bootstrap.secret:}")
    private String bootstrapSecret;

    @PostMapping({"/api/v1/auth/bootstrap", "/api/v2/auth/bootstrap"})
    public ApiResponse<Map<String, String>> bootstrap(
            @RequestHeader(value = "X-Bootstrap-Secret", required = false) String providedSecret,
            @RequestBody Map<String, String> request
    ) {
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
    public ApiResponse<Map<String, Object>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalStateException("Not authenticated");
        }

        if (auth.getPrincipal() instanceof AuthenticatedActor actor) {
            return ApiResponse.success(Map.of(
                    "userId", actor.userId(),
                    "username", actor.username() != null ? actor.username() : "",
                    "userCode", actor.userCode() != null ? actor.userCode() : "",
                    "role", actor.role() != null ? actor.role() : "",
                    "merchantId", actor.merchantId() != null ? actor.merchantId() : 0,
                    "permissions", actor.permissions(),
                    "accessibleStoreIds", actor.accessibleStoreIds()
            ));
        }

        // Legacy fallback
        Long userId = Long.parseLong(auth.getPrincipal().toString());
        AuthUserDto user = authService.getUser(userId);
        return ApiResponse.success(Map.of(
                "userId", user.id(),
                "username", user.username(),
                "role", user.role()
        ));
    }
}
