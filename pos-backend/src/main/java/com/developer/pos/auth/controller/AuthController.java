package com.developer.pos.auth.controller;

import com.developer.pos.auth.dto.AuthUserDto;
import com.developer.pos.auth.dto.LoginRequest;
import com.developer.pos.auth.dto.LoginResponse;
import com.developer.pos.auth.entity.AuthUserEntity;
import com.developer.pos.auth.repository.AuthUserRepository;
import com.developer.pos.auth.service.AuthService;
import com.developer.pos.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthUserRepository userRepository;

    public AuthController(AuthService authService, AuthUserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Map<String, Boolean>> logout() {
        return ApiResponse.success(Map.of("success", true));
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserDto> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("Not authenticated.");
        }
        String userId = auth.getPrincipal().toString();
        AuthUserEntity user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        return ApiResponse.success(new AuthUserDto(
                user.getId(), user.getUsername(), user.getDisplayName(),
                user.getRole(), user.getMerchantId(), user.getStoreId()
        ));
    }
}
