package com.developer.pos.v2.catalog.interfaces.rest.request;

import jakarta.validation.constraints.NotNull;

public record UpsertSkuPriceOverrideRequest(
        @NotNull Long skuId,
        Long storeId,
        String priceContext,
        String priceContextRef,
        @NotNull Long overridePriceCents,
        boolean active
) {
}
