package com.developer.pos.auth.service;

import com.developer.pos.auth.dto.AuthUserDto;
import com.developer.pos.auth.dto.LoginRequest;
import com.developer.pos.auth.dto.LoginResponse;
import com.developer.pos.auth.entity.AuthUserEntity;
import com.developer.pos.auth.repository.AuthUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AuthService {

    private final AuthUserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AuthUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AuthUserEntity user = userRepository.findByUsernameAndUserStatus(request.username(), "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials.");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        AuthUserDto userDto = new AuthUserDto(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getMerchantId(),
                user.getStoreId()
        );

        return new LoginResponse("pending-jwt", userDto);
    }
}
