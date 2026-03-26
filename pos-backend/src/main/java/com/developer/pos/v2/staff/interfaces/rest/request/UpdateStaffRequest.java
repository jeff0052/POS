package com.developer.pos.v2.staff.interfaces.rest.request;

public record UpdateStaffRequest(
        String staffName,
        String roleCode,
        String phone,
        String staffStatus
) {
}
