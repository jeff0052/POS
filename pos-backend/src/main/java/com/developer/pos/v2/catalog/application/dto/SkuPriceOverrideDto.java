package com.developer.pos.v2.catalog.application.dto;

public record SkuPriceOverrideDto(
        Long id,
        Long skuId,
        Long storeId,
        String priceContext,
        String priceContextRef,
        long overridePriceCents,
        boolean active
) {
}
