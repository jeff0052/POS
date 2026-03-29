package com.developer.pos.v2.rbac.application.dto;

public record PermissionDto(
    Long id,
    String permissionCode,
    String permissionName,
    String description,
    String riskLevel
) {}
