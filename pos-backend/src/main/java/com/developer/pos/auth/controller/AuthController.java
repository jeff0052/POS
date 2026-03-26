package com.developer.pos.auth.controller;

import com.developer.pos.auth.dto.AuthUserDto;
import com.developer.pos.auth.dto.LoginRequest;
import com.developer.pos.auth.dto.LoginResponse;
import com.developer.pos.auth.entity.AuthUserEntity;
import com.developer.pos.auth.repository.AuthUserRepository;
import com.developer.pos.auth.service.AuthService;
import com.developer.pos.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
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
    public ApiResponse<AuthUserDto> me(@RequestParam(required = false) String username) {
        if (username == null || username.isBlank()) {
            return ApiResponse.success(new AuthUserDto(1L, "admin", "Default Admin", "ADMIN", null, null));
        }
        AuthUserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        return ApiResponse.success(new AuthUserDto(
                user.getId(), user.getUsername(), user.getDisplayName(),
                user.getRole(), user.getMerchantId(), user.getStoreId()
        ));
    }
}
