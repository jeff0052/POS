package com.developer.pos.v2.staff.application.command;

public record CreateStaffCommand(
    Long merchantId, Long storeId, String staffName, String staffCode, String pin, String roleCode, String phone
) {}
