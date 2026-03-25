package com.developer.pos.v2.catalog.interfaces.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpsertCatalogProductRequest(
        @NotBlank String storeCode,
        @NotNull Long categoryId,
        String productCode,
        @NotBlank String name,
        @NotBlank String status,
        @Valid @NotEmpty List<UpsertCatalogSkuItemRequest> skus
) {
    public record UpsertCatalogSkuItemRequest(
            Long skuId,
            String skuCode,
            @NotBlank String name,
            @Min(0) long priceCents,
            @NotBlank String status,
            @NotNull Boolean available
    ) {
    }
}
