package com.developer.pos.v2.catalog.application.dto;

public record AdminCatalogProductDto(
        Long id,
        String name,
        String barcode,
        long priceCents,
        int stockQty,
        String status,
        String categoryName
) {
}
