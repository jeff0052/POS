package com.developer.pos.v2.catalog.interfaces.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpsertModifierGroupRequest(
        @NotBlank String groupCode,
        @NotBlank String groupName,
        @NotBlank String selectionType,
        @NotNull Boolean required,
        int minSelect,
        int maxSelect,
        int sortOrder,
        @Valid List<OptionItem> options
) {
    public record OptionItem(
            Long optionId,
            @NotBlank String optionCode,
            @NotBlank String optionName,
            long priceAdjustmentCents,
            boolean defaultOption,
            int sortOrder
    ) {
    }
}
