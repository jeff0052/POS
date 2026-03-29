package com.developer.pos.v2.catalog.application.dto;

public record BuffetBillDto(
    int guestCount, int childCount,
    long headFeeCents, long surchargeTotal, long extraTotal,
    long overtimeMinutes, long overtimeFeeCents, long grandTotal,
    String packageName, long packagePriceCents, Long childPriceCents,
    int durationMinutes, int overtimeGraceMinutes, int maxOvertimeMinutes
) {}
