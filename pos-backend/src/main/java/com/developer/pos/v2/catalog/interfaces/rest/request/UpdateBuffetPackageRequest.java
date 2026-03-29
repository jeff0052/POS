package com.developer.pos.v2.catalog.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateBuffetPackageRequest(
    @NotBlank String packageCode, @NotBlank String packageName, String description,
    @NotNull Long priceCents, Long childPriceCents, Integer childAgeMax,
    int durationMinutes, int warningBeforeMinutes,
    long overtimeFeePerMinuteCents, int overtimeGraceMinutes, int maxOvertimeMinutes,
    List<String> applicableTimeSlots, List<String> applicableDays,
    int sortOrder, Long imageId
) {}
