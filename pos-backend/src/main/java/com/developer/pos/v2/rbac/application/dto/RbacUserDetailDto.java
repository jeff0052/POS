package com.developer.pos.v2.rbac.application.dto;

import java.util.List;

public record RbacUserDetailDto(
    Long id,
    String userCode,
    String username,
    String displayName,
    Long merchantId,
    String phone,
    String email,
    String userStatus,
    boolean mustChangePassword,
    List<String> roles,
    List<RbacStoreAccessDto> storeAccess
) {}
