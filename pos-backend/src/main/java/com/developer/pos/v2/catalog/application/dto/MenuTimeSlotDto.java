package com.developer.pos.v2.catalog.application.dto;

import java.time.LocalTime;
import java.util.List;

public record MenuTimeSlotDto(
        Long id,
        String slotCode,
        String slotName,
        LocalTime startTime,
        LocalTime endTime,
        List<String> applicableDays,
        List<String> diningModes,
        boolean active,
        int priority,
        List<Long> productIds
) {
}
