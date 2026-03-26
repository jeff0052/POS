package com.developer.pos.v2.staff.application.command;

public record UpdateStaffCommand(
        String staffId,
        String staffName,
        String roleCode,
        String phone,
        String staffStatus
) {
}
