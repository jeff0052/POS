package com.developer.pos.auth.service;

import com.developer.pos.auth.dto.AuthUserDto;
import com.developer.pos.auth.dto.LoginRequest;
import com.developer.pos.auth.dto.LoginResponse;
import com.developer.pos.auth.entity.AuthUserEntity;
import com.developer.pos.auth.repository.AuthUserRepository;
import com.developer.pos.auth.security.JwtProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthUserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AuthUserRepository userRepository, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
    }

    public LoginResponse login(LoginRequest request) {
        AuthUserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials."));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("Invalid credentials.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials.");
        }

        String token = jwtProvider.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getMerchantId(),
                user.getStoreId()
        );

        return new LoginResponse(token, toDto(user));
    }

    public AuthUserDto getUser(Long userId) {
        AuthUserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toDto(user);
    }

    private AuthUserDto toDto(AuthUserEntity user) {
        return new AuthUserDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getStoreId()
        );
    }
}
