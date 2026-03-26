package com.developer.pos.v2.catalog.application.dto;

public record AdminCatalogModifierOptionDto(
        String code,
        String label,
        long priceDeltaCents,
        boolean defaultSelected,
        String kitchenLabel
) {
}
