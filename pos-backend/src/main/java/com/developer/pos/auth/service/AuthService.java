package com.developer.pos.auth.service;

import com.developer.pos.auth.dto.AuthUserDto;
import com.developer.pos.auth.dto.LoginRequest;
import com.developer.pos.auth.dto.LoginResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public LoginResponse login(LoginRequest request) {
        return new LoginResponse(
            "mock-jwt-token",
            new AuthUserDto(1L, request.username(), "Store Admin", "ADMIN", 1001L)
        );
    }
}
