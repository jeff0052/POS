package com.developer.pos.v2.catalog.application.dto;

public record AdminCatalogCategoryDto(
        Long id,
        String name,
        int sortOrder,
        String status
) {
}
