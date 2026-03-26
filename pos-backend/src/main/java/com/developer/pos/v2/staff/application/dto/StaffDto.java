package com.developer.pos.v2.staff.application.dto;

import java.util.List;

public record StaffDto(
    String staffId, Long merchantId, Long storeId, String staffName, String staffCode,
    String roleCode, String roleName, String staffStatus, String phone, List<String> permissions
) {}
