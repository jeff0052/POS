package com.developer.pos.auth.service;

import com.developer.pos.auth.dto.AuthUserDto;
import com.developer.pos.auth.dto.LoginRequest;
import com.developer.pos.auth.dto.LoginResponse;
import com.developer.pos.common.security.JwtTokenProvider;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        // TODO: Replace with real user lookup + password verification
        AuthUserDto user = new AuthUserDto(1L, request.username(), "Store Admin", "ADMIN", 1001L);
        String token = jwtTokenProvider.generateToken(user.id(), user.username(), user.role(), user.storeId());
        return new LoginResponse(token, user);
    }
}
