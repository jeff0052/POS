package com.developer.pos.v2.catalog.application.dto;

public record AdminCatalogProductDto(
        Long id,
        Long categoryId,
        String name,
        String barcode,
        long priceCents,
        int stockQty,
        String status,
        String categoryName,
        java.util.List<AdminCatalogSkuDto> skus,
        java.util.List<AdminCatalogAttributeGroupDto> attributeGroups,
        java.util.List<AdminCatalogModifierGroupDto> modifierGroups,
        java.util.List<AdminCatalogComboSlotDto> comboSlots
) {
}
