package com.developer.pos.v2.rbac.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.rbac.application.dto.CustomRoleDto;
import com.developer.pos.v2.rbac.application.dto.PermissionDto;
import com.developer.pos.v2.rbac.application.dto.PermissionGroupDto;
import com.developer.pos.v2.rbac.application.dto.RbacStoreAccessDto;
import com.developer.pos.v2.rbac.application.dto.RbacUserDetailDto;
import com.developer.pos.v2.rbac.application.dto.RbacUserDto;
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.CustomRoleEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.CustomRolePermissionEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.PermissionEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.UserEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.UserRoleEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.UserStoreAccessEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaCustomRolePermissionRepository;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaCustomRoleRepository;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaPermissionRepository;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaUserRepository;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaUserRoleRepository;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaUserStoreAccessRepository;
import com.developer.pos.v2.rbac.interfaces.rest.request.CreateCustomRoleRequest;
import com.developer.pos.v2.rbac.interfaces.rest.request.CreateUserRequest;
import com.developer.pos.v2.rbac.interfaces.rest.request.SetStoreAccessRequest;
import com.developer.pos.v2.rbac.interfaces.rest.request.UpdateCustomRoleRequest;
import com.developer.pos.v2.rbac.interfaces.rest.request.UpdateUserRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RbacManagementService implements UseCase {

    private final JpaUserRepository userRepository;
    private final JpaPermissionRepository permissionRepository;
    private final JpaCustomRoleRepository customRoleRepository;
    private final JpaCustomRolePermissionRepository customRolePermissionRepository;
    private final JpaUserRoleRepository userRoleRepository;
    private final JpaUserStoreAccessRepository userStoreAccessRepository;
    private final PermissionCacheService permissionCacheService;
    private final PasswordEncoder passwordEncoder;

    public RbacManagementService(
            JpaUserRepository userRepository,
            JpaPermissionRepository permissionRepository,
            JpaCustomRoleRepository customRoleRepository,
            JpaCustomRolePermissionRepository customRolePermissionRepository,
            JpaUserRoleRepository userRoleRepository,
            JpaUserStoreAccessRepository userStoreAccessRepository,
            PermissionCacheService permissionCacheService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.customRoleRepository = customRoleRepository;
        this.customRolePermissionRepository = customRolePermissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.userStoreAccessRepository = userStoreAccessRepository;
        this.permissionCacheService = permissionCacheService;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== User CRUD ====================

    @Transactional
    public RbacUserDto createUser(CreateUserRequest request) {
        UserEntity user = new UserEntity();
        user.setUserCode(generateUserCode());
        user.setMerchantId(request.getMerchantId());
        user.setDisplayName(request.getDisplayName());
        user.setUsername(request.getUsername());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setUserStatus("ACTIVE");
        user.setMustChangePassword(true);
        user.setFailedLoginCount(0);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getPin() != null && !request.getPin().isBlank()) {
            user.setPinHash(passwordEncoder.encode(request.getPin()));
        }

        userRepository.save(user);

        // Assign roles
        if (request.getRoleIds() != null) {
            for (Long roleId : request.getRoleIds()) {
                UserRoleEntity userRole = new UserRoleEntity();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                userRole.setAssignedAt(OffsetDateTime.now());
                userRoleRepository.save(userRole);
            }
        }

        // Grant store access
        if (request.getStoreIds() != null) {
            for (Long storeId : request.getStoreIds()) {
                UserStoreAccessEntity access = new UserStoreAccessEntity();
                access.setUserId(user.getId());
                access.setStoreId(storeId);
                access.setAccessLevel("FULL");
                access.setGrantedAt(OffsetDateTime.now());
                userStoreAccessRepository.save(access);
            }
        }

        return toUserDto(user);
    }

    @Transactional
    public RbacUserDto updateUser(Long userId, UpdateUserRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getUserStatus() != null) {
            user.setUserStatus(request.getUserStatus());
        }

        userRepository.save(user);
        permissionCacheService.evict(userId);

        return toUserDto(user);
    }

    @Transactional
    public void deactivateUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setUserStatus("DISABLED");
        userRepository.save(user);
        permissionCacheService.evict(userId);
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);
        permissionCacheService.evict(userId);
    }

    @Transactional
    public void setPin(Long userId, String newPin) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setPinHash(passwordEncoder.encode(newPin));
        userRepository.save(user);
        permissionCacheService.evict(userId);
    }

    // ==================== Role Management ====================

    public List<PermissionGroupDto> listPermissions() {
        List<PermissionEntity> all = permissionRepository.findAll();

        Map<String, List<PermissionDto>> grouped = new LinkedHashMap<>();
        for (PermissionEntity p : all) {
            grouped.computeIfAbsent(p.getPermissionGroup(), k -> new ArrayList<>())
                    .add(new PermissionDto(
                            p.getId(),
                            p.getPermissionCode(),
                            p.getPermissionName(),
                            p.getDescription(),
                            p.getRiskLevel()
                    ));
        }

        return grouped.entrySet().stream()
                .map(e -> new PermissionGroupDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public List<CustomRoleDto> listRoles(Long merchantId) {
        List<CustomRoleEntity> roles = customRoleRepository.findByMerchantIdOrMerchantIdIsNull(merchantId);
        return roles.stream().map(this::toRoleDto).collect(Collectors.toList());
    }

    @Transactional
    public CustomRoleDto createCustomRole(CreateCustomRoleRequest request) {
        CustomRoleEntity role = new CustomRoleEntity();
        role.setMerchantId(request.getMerchantId());
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setRoleDescription(request.getRoleDescription());
        role.setRoleLevel(request.getRoleLevel());
        role.setMaxRefundCents(request.getMaxRefundCents());
        role.setIsSystem(false);
        role.setIsEditable(true);
        role.setCreatedAt(OffsetDateTime.now());
        role.setUpdatedAt(OffsetDateTime.now());

        customRoleRepository.save(role);

        // Create permission mappings
        if (request.getPermissionCodes() != null) {
            for (String code : request.getPermissionCodes()) {
                CustomRolePermissionEntity crp = new CustomRolePermissionEntity();
                crp.setRoleId(role.getId());
                crp.setPermissionCode(code);
                customRolePermissionRepository.save(crp);
            }
        }

        return toRoleDto(role);
    }

    @Transactional
    public CustomRoleDto updateCustomRole(Long roleId, UpdateCustomRoleRequest request) {
        CustomRoleEntity role = customRoleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        if (Boolean.FALSE.equals(role.getIsEditable())) {
            throw new IllegalStateException("Role is not editable: " + roleId);
        }

        if (request.getRoleName() != null) {
            role.setRoleName(request.getRoleName());
        }
        if (request.getRoleDescription() != null) {
            role.setRoleDescription(request.getRoleDescription());
        }
        if (request.getMaxRefundCents() != null) {
            role.setMaxRefundCents(request.getMaxRefundCents());
        }
        role.setUpdatedAt(OffsetDateTime.now());
        customRoleRepository.save(role);

        // Replace permissions if provided
        if (request.getPermissionCodes() != null) {
            customRolePermissionRepository.deleteByRoleId(roleId);
            for (String code : request.getPermissionCodes()) {
                CustomRolePermissionEntity crp = new CustomRolePermissionEntity();
                crp.setRoleId(roleId);
                crp.setPermissionCode(code);
                customRolePermissionRepository.save(crp);
            }
        }

        return toRoleDto(role);
    }

    @Transactional
    public void deleteCustomRole(Long roleId) {
        CustomRoleEntity role = customRoleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new IllegalStateException("Cannot delete system role: " + roleId);
        }

        customRolePermissionRepository.deleteByRoleId(roleId);
        customRoleRepository.delete(role);
    }

    // ==================== User-Role Assignment ====================

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Remove existing roles
        List<UserRoleEntity> existing = userRoleRepository.findByUserId(userId);
        for (UserRoleEntity ur : existing) {
            userRoleRepository.deleteByUserIdAndRoleId(userId, ur.getRoleId());
        }

        // Assign new roles
        for (Long roleId : roleIds) {
            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setAssignedAt(OffsetDateTime.now());
            userRoleRepository.save(userRole);
        }

        permissionCacheService.evict(userId);
    }

    @Transactional
    public void setStoreAccess(Long userId, List<SetStoreAccessRequest.Entry> entries) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Remove existing store access
        List<UserStoreAccessEntity> existing = userStoreAccessRepository.findByUserId(userId);
        for (UserStoreAccessEntity sa : existing) {
            userStoreAccessRepository.deleteByUserIdAndStoreId(userId, sa.getStoreId());
        }

        // Grant new store access
        for (SetStoreAccessRequest.Entry entry : entries) {
            UserStoreAccessEntity access = new UserStoreAccessEntity();
            access.setUserId(userId);
            access.setStoreId(entry.getStoreId());
            access.setAccessLevel(entry.getAccessLevel());
            access.setGrantedAt(OffsetDateTime.now());
            userStoreAccessRepository.save(access);
        }

        permissionCacheService.evict(userId);
    }

    public List<RbacUserDetailDto> listUsers(Long merchantId) {
        List<UserEntity> users = userRepository.findByMerchantId(merchantId);
        List<RbacUserDetailDto> result = new ArrayList<>();

        for (UserEntity user : users) {
            List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(user.getId());
            List<Long> roleIds = userRoles.stream().map(UserRoleEntity::getRoleId).toList();
            List<String> roleCodes = roleIds.isEmpty()
                    ? List.of()
                    : customRoleRepository.findAllById(roleIds).stream()
                            .map(CustomRoleEntity::getRoleCode)
                            .collect(Collectors.toList());

            List<UserStoreAccessEntity> storeAccessEntities = userStoreAccessRepository.findByUserId(user.getId());
            List<RbacStoreAccessDto> storeAccess = storeAccessEntities.stream()
                    .map(sa -> new RbacStoreAccessDto(sa.getStoreId(), sa.getAccessLevel()))
                    .collect(Collectors.toList());

            result.add(new RbacUserDetailDto(
                    user.getId(),
                    user.getUserCode(),
                    user.getUsername(),
                    user.getDisplayName(),
                    user.getMerchantId(),
                    user.getPhone(),
                    user.getEmail(),
                    user.getUserStatus(),
                    Boolean.TRUE.equals(user.getMustChangePassword()),
                    roleCodes,
                    storeAccess
            ));
        }

        return result;
    }

    // ==================== Private Helpers ====================

    private String generateUserCode() {
        return "U-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private CustomRoleDto toRoleDto(CustomRoleEntity role) {
        List<String> permCodes = customRolePermissionRepository.findByRoleId(role.getId())
                .stream().map(CustomRolePermissionEntity::getPermissionCode).collect(Collectors.toList());
        return new CustomRoleDto(
                role.getId(),
                role.getMerchantId(),
                role.getRoleCode(),
                role.getRoleName(),
                role.getRoleDescription(),
                Boolean.TRUE.equals(role.getIsSystem()),
                Boolean.TRUE.equals(role.getIsEditable()),
                role.getRoleLevel(),
                role.getMaxRefundCents(),
                permCodes
        );
    }

    private RbacUserDto toUserDto(UserEntity user) {
        return new RbacUserDto(
                user.getId(),
                user.getUserCode(),
                user.getUsername(),
                user.getDisplayName(),
                user.getMerchantId(),
                user.getUserStatus(),
                Boolean.TRUE.equals(user.getMustChangePassword())
        );
    }
}
