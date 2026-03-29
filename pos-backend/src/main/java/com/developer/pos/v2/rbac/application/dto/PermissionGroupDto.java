package com.developer.pos.v2.rbac.application.dto;

import java.util.List;

public record PermissionGroupDto(String group, List<PermissionDto> permissions) {}
