package com.developer.pos.v2.rbac.application.dto;

public record RbacUserDto(
    Long id,
    String userCode,
    String username,
    String displayName,
    Long merchantId,
    String userStatus,
    boolean mustChangePassword
) {}
