package com.developer.pos.v2.shift.application.command;

public record OpenShiftCommand(Long merchantId, Long storeId, String cashierStaffId, String cashierName, long openingCashCents) {}
