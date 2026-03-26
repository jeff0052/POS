package com.developer.pos.v2.shift.application.command;

public record CloseShiftCommand(String shiftId, long closingCashCents, String notes) {}
