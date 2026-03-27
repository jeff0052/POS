package com.developer.pos.v2.catalog.application.dto;

public record AdminCatalogSkuDto(
        Long id,
        Long productId,
        String name,
        String barcode,
        long priceCents,
        String status,
        boolean available,
        String imageId,
        String imageUrl
) {
}
