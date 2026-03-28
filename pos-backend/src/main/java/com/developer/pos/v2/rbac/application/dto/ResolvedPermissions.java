package com.developer.pos.v2.rbac.application.dto;

import java.util.Set;

public record ResolvedPermissions(
    Set<String> permissions,
    Set<Long> accessibleStoreIds,
    String primaryRoleCode,
    Long maxRefundCents
) {}
