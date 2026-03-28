package com.developer.pos.v2.rbac.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.rbac.application.dto.RbacLoginResponse;
import com.developer.pos.v2.rbac.application.dto.RbacStoreAccessDto;
import com.developer.pos.v2.rbac.application.dto.RbacUserDto;
import com.developer.pos.v2.rbac.application.dto.ResolvedPermissions;
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.CustomRoleEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.UserEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.UserRoleEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.UserStoreAccessEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaCustomRoleRepository;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaUserRepository;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaUserRoleRepository;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaUserStoreAccessRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreRepository;
import com.developer.pos.auth.security.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RbacAuthService implements UseCase {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    private final JpaUserRepository userRepository;
    private final JpaUserRoleRepository userRoleRepository;
    private final JpaUserStoreAccessRepository userStoreAccessRepository;
    private final JpaCustomRoleRepository customRoleRepository;
    private final JpaStoreRepository storeRepository;
    private final JwtProvider jwtProvider;
    private final PermissionCacheService permissionCacheService;
    private final PasswordEncoder passwordEncoder;

    public RbacAuthService(
            JpaUserRepository userRepository,
            JpaUserRoleRepository userRoleRepository,
            JpaUserStoreAccessRepository userStoreAccessRepository,
            JpaCustomRoleRepository customRoleRepository,
            JpaStoreRepository storeRepository,
            JwtProvider jwtProvider,
            PermissionCacheService permissionCacheService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userStoreAccessRepository = userStoreAccessRepository;
        this.customRoleRepository = customRoleRepository;
        this.storeRepository = storeRepository;
        this.jwtProvider = jwtProvider;
        this.permissionCacheService = permissionCacheService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RbacLoginResponse login(String username, String password) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        validateUserNotDisabledOrLocked(user);
        verifyPassword(user, password);

        // Success: reset failed login state
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        return buildLoginResponse(user);
    }

    @Transactional
    public RbacLoginResponse pinLogin(Long storeId, String userCode, String pin) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        UserEntity user = userRepository.findByUserCodeAndMerchantId(userCode, store.getMerchantId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        // Verify user has access to the requested store
        boolean hasAccess = userStoreAccessRepository.findByUserIdAndStoreId(user.getId(), storeId).isPresent();
        if (!hasAccess) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        validateUserNotDisabledOrLocked(user);

        if (user.getPinHash() == null || !passwordEncoder.matches(pin, user.getPinHash())) {
            handleFailedLogin(user);
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Success: reset failed login state
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        return buildLoginResponse(user, storeId);
    }

    @Transactional
    public synchronized RbacLoginResponse bootstrapFirstAdmin(String username, String password, String displayName) {
        // Check if any SUPER_ADMIN user_role exists
        CustomRoleEntity superAdminRole = customRoleRepository.findByRoleCode("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role not found in custom_roles"));

        boolean adminExists = userRoleRepository.findAll().stream()
                .anyMatch(ur -> ur.getRoleId().equals(superAdminRole.getId()));
        if (adminExists) {
            throw new IllegalStateException("Admin already exists");
        }

        // Create new user
        UserEntity user = new UserEntity();
        user.setUserCode("ADMIN-1");
        user.setMerchantId(0L);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName(displayName);
        user.setMustChangePassword(false);
        user.setUserStatus("ACTIVE");
        user.setFailedLoginCount(0);
        userRepository.save(user);

        // Create user_role entry
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(user.getId());
        userRole.setRoleId(superAdminRole.getId());
        userRole.setAssignedAt(OffsetDateTime.now());
        userRoleRepository.save(userRole);

        return buildLoginResponse(user);
    }

    // --- Private helpers ---

    private void validateUserNotDisabledOrLocked(UserEntity user) {
        String status = user.getUserStatus();
        if ("DISABLED".equals(status)) {
            throw new IllegalStateException("Account is disabled");
        }
        if ("LOCKED".equals(status)) {
            throw new IllegalStateException("Account is locked");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new IllegalStateException("Account locked until " + user.getLockedUntil());
        }
    }

    private void verifyPassword(UserEntity user, String password) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new IllegalArgumentException("Invalid credentials");
        }
    }

    private void handleFailedLogin(UserEntity user) {
        int failedCount = user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount();
        failedCount++;
        user.setFailedLoginCount(failedCount);
        if (failedCount >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(OffsetDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
        }
        userRepository.save(user);
    }

    private RbacLoginResponse buildLoginResponse(UserEntity user) {
        return buildLoginResponse(user, null);
    }

    private RbacLoginResponse buildLoginResponse(UserEntity user, Long requestedStoreId) {
        ResolvedPermissions resolved = permissionCacheService.resolve(user.getId());

        // Use requested storeId if provided (PIN login), otherwise null (password login)
        Long storeId = requestedStoreId;

        String token = jwtProvider.generateToken(
                user.getId(),
                user.getUsername(),
                user.getUserCode(),
                resolved.primaryRoleCode(),
                user.getMerchantId(),
                storeId
        );

        RbacUserDto userDto = new RbacUserDto(
                user.getId(),
                user.getUserCode(),
                user.getUsername(),
                user.getDisplayName(),
                user.getMerchantId(),
                user.getUserStatus(),
                Boolean.TRUE.equals(user.getMustChangePassword())
        );

        List<UserStoreAccessEntity> storeAccessEntities = userStoreAccessRepository.findByUserId(user.getId());
        List<RbacStoreAccessDto> accessibleStores = storeAccessEntities.stream()
                .map(sa -> new RbacStoreAccessDto(sa.getStoreId(), sa.getAccessLevel()))
                .collect(Collectors.toList());

        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(user.getId());
        List<Long> roleIds = userRoles.stream().map(UserRoleEntity::getRoleId).toList();
        List<String> roles = roleIds.isEmpty()
                ? List.of()
                : customRoleRepository.findAllById(roleIds).stream()
                        .map(CustomRoleEntity::getRoleCode)
                        .collect(Collectors.toList());

        return new RbacLoginResponse(token, userDto, resolved.permissions(), accessibleStores, roles);
    }
}
