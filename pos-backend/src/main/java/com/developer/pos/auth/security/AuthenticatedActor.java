package com.developer.pos.auth.security;

import java.util.Set;

public record AuthenticatedActor(
        Long userId,
        String username,
        String userCode,
        String role,
        Long merchantId,
        Long storeId,
        Set<Long> accessibleStoreIds,
        Set<String> permissions
) {
    /** Backward-compatible constructor for legacy JWT tokens without userCode/permissions */
    public AuthenticatedActor(Long userId, String username, String role, Long merchantId, Long storeId) {
        this(userId, username, null, role, merchantId, storeId, storeId != null ? Set.of(storeId) : Set.of(), Set.of());
    }

    public boolean hasPermission(String permissionCode) {
        return permissions != null && permissions.contains(permissionCode);
    }

    public boolean hasStoreAccess(Long targetStoreId) {
        return accessibleStoreIds != null && accessibleStoreIds.contains(targetStoreId);
    }
}
