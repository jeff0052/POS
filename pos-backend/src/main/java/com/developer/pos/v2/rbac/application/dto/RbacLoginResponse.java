package com.developer.pos.v2.rbac.application.dto;

import java.util.List;
import java.util.Set;

public record RbacLoginResponse(
    String token,
    RbacUserDto user,
    Set<String> permissions,
    List<RbacStoreAccessDto> accessibleStores,
    List<String> roles
) {}
