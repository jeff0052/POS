package com.developer.pos.v2.staff.application.dto;

import java.util.List;

public record StaffPinVerificationResult(
    String staffId, String staffName, String roleCode, List<String> permissions
) {}
