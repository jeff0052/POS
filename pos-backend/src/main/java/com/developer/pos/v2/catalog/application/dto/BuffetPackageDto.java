package com.developer.pos.v2.catalog.application.dto;

import java.util.List;

public record BuffetPackageDto(
    Long packageId, String packageCode, String packageName, String description,
    long priceCents, Long childPriceCents, Integer childAgeMax,
    int durationMinutes, int warningBeforeMinutes,
    long overtimeFeePerMinuteCents, int overtimeGraceMinutes, int maxOvertimeMinutes,
    String packageStatus, List<String> applicableTimeSlots, List<String> applicableDays,
    int sortOrder, String imageUrl
) {}
