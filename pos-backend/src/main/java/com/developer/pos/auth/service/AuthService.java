package com.developer.pos.auth.service;

import com.developer.pos.auth.dto.AuthUserDto;
import com.developer.pos.auth.dto.LoginRequest;
import com.developer.pos.auth.dto.LoginResponse;
import com.developer.pos.auth.entity.AuthUserEntity;
import com.developer.pos.auth.repository.AuthUserRepository;
import com.developer.pos.auth.security.JwtProvider;
import com.developer.pos.v2.rbac.application.dto.RbacLoginResponse;
import com.developer.pos.v2.rbac.application.service.RbacAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthUserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RbacAuthService rbacAuthService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AuthUserRepository userRepository, JwtProvider jwtProvider, RbacAuthService rbacAuthService) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.rbacAuthService = rbacAuthService;
    }

    public LoginResponse login(LoginRequest request) {
        // Try RBAC (users table) first
        try {
            RbacLoginResponse rbacResponse = rbacAuthService.login(request.username(), request.password());
            return new LoginResponse(
                    rbacResponse.token(),
                    new AuthUserDto(
                            rbacResponse.user().id(),
                            rbacResponse.user().username(),
                            rbacResponse.user().displayName(),
                            rbacResponse.roles().isEmpty() ? "CASHIER" : rbacResponse.roles().get(0),
                            null
                    ),
                    rbacResponse.permissions(),
                    rbacResponse.roles()
            );
        } catch (Exception e) {
            log.debug("RBAC login failed for {}, falling back to legacy auth_users: {}", request.username(), e.getMessage());
        }

        // Fallback to legacy auth_users
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

        return new LoginResponse(token, toDto(user), java.util.Set.of(), java.util.List.of(user.getRole()));
    }

    public AuthUserDto getUser(Long userId) {
        AuthUserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toDto(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public synchronized void bootstrapFirstAdmin(String username, String password, String displayName) {
        // Try RBAC bootstrap first
        try {
            rbacAuthService.bootstrapFirstAdmin(username, password, displayName);
            return;
        } catch (Exception e) {
            log.debug("RBAC bootstrap failed, falling back to legacy: {}", e.getMessage());
        }

        // Legacy fallback
        long count = userRepository.count();
        if (count > 0) {
            throw new IllegalStateException("Bootstrap not allowed: users already exist");
        }
        AuthUserEntity admin = new AuthUserEntity();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setDisplayName(displayName);
        admin.setRole("PLATFORM_ADMIN");
        admin.setStatus("ACTIVE");
        userRepository.save(admin);
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
