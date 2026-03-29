package com.developer.pos.v2.rbac.application.dto;

import java.util.List;

public record CustomRoleDto(
    Long id,
    Long merchantId,
    String roleCode,
    String roleName,
    String roleDescription,
    boolean isSystem,
    boolean isEditable,
    String roleLevel,
    Long maxRefundCents,
    List<String> permissionCodes
) {}
