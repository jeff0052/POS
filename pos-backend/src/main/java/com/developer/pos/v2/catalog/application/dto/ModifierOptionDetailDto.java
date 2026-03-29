package com.developer.pos.v2.catalog.application.dto;

public record ModifierOptionDetailDto(
        Long id,
        String optionCode,
        String optionName,
        long priceAdjustmentCents,
        boolean defaultOption,
        int sortOrder
) {
}
