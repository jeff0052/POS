package com.developer.pos.v2.catalog.interfaces.rest.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertCatalogCategoryRequest(
        @NotBlank String storeCode,
        String categoryCode,
        @NotBlank String name,
        @NotNull Boolean enabled,
        @Min(0) @Max(9999) Integer sortOrder
) {
}
