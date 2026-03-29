package com.developer.pos.v2.catalog.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public record UpsertMenuTimeSlotRequest(
        @NotBlank String slotCode,
        @NotBlank String slotName,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        List<String> applicableDays,
        List<String> diningModes,
        boolean active,
        int priority,
        List<Long> productIds
) {
}
