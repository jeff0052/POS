package com.developer.pos.v2.rbac.application.service;

import com.developer.pos.v2.rbac.application.dto.ResolvedPermissions;
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.CustomRoleEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.CustomRolePermissionEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.UserRoleEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.entity.UserStoreAccessEntity;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaCustomRolePermissionRepository;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaCustomRoleRepository;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaUserRoleRepository;
import com.developer.pos.v2.rbac.infrastructure.persistence.repository.JpaUserStoreAccessRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PermissionResolver {

    private static final Map<String, Integer> ROLE_LEVEL_PRIORITY = Map.of(
        "PLATFORM", 3,
        "MERCHANT", 2,
        "STORE", 1
    );

    private final JpaUserRoleRepository userRoleRepository;
    private final JpaCustomRolePermissionRepository rolePermissionRepository;
    private final JpaCustomRoleRepository customRoleRepository;
    private final JpaUserStoreAccessRepository userStoreAccessRepository;

    public PermissionResolver(
            JpaUserRoleRepository userRoleRepository,
            JpaCustomRolePermissionRepository rolePermissionRepository,
            JpaCustomRoleRepository customRoleRepository,
            JpaUserStoreAccessRepository userStoreAccessRepository
    ) {
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.customRoleRepository = customRoleRepository;
        this.userStoreAccessRepository = userStoreAccessRepository;
    }

    public ResolvedPermissions resolve(Long userId) {
        // 1. Find user's roles via user_roles
        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(userId);
        List<Long> roleIds = userRoles.stream()
                .map(UserRoleEntity::getRoleId)
                .toList();

        // 2. Find all permission_codes for those roles
        Set<String> permissions = new HashSet<>();
        if (!roleIds.isEmpty()) {
            List<CustomRolePermissionEntity> rolePermissions =
                    rolePermissionRepository.findByRoleIdIn(roleIds);
            permissions = rolePermissions.stream()
                    .map(CustomRolePermissionEntity::getPermissionCode)
                    .collect(Collectors.toSet());
        }

        // 3. Find accessibleStoreIds via user_store_access
        List<UserStoreAccessEntity> storeAccess = userStoreAccessRepository.findByUserId(userId);
        Set<Long> accessibleStoreIds = storeAccess.stream()
                .map(UserStoreAccessEntity::getStoreId)
                .collect(Collectors.toSet());

        // 4. Load the actual role entities to determine primary role and max refund
        List<CustomRoleEntity> roles = roleIds.isEmpty()
                ? List.of()
                : customRoleRepository.findAllById(roleIds);

        // 5. Determine primaryRoleCode (highest role_level: PLATFORM > MERCHANT > STORE)
        String primaryRoleCode = roles.stream()
                .max(Comparator.comparingInt(r -> ROLE_LEVEL_PRIORITY.getOrDefault(r.getRoleLevel(), 0)))
                .map(CustomRoleEntity::getRoleCode)
                .orElse(null);

        // 6. Get maxRefundCents from highest role
        Long maxRefundCents = roles.stream()
                .map(CustomRoleEntity::getMaxRefundCents)
                .filter(v -> v != null)
                .max(Long::compareTo)
                .orElse(0L);

        return new ResolvedPermissions(permissions, accessibleStoreIds, primaryRoleCode, maxRefundCents);
    }
}
