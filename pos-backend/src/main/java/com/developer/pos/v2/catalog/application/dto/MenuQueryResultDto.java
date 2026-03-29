package com.developer.pos.v2.catalog.application.dto;

import java.util.List;

public record MenuQueryResultDto(
        List<MenuCategoryDto> categories
) {
    public record MenuCategoryDto(
            Long categoryId,
            String categoryName,
            List<MenuProductDto> products
    ) {
    }

    public record MenuProductDto(
            Long productId,
            String productName,
            String imageUrl,
            List<MenuSkuDto> skus
    ) {
    }

    public record MenuSkuDto(
            Long skuId,
            String skuCode,
            String skuName,
            long priceCents,
            String imageUrl,
            List<MenuModifierGroupDto> modifierGroups
    ) {
    }

    public record MenuModifierGroupDto(
            Long groupId,
            String groupName,
            String selectionType,
            boolean required,
            int minSelect,
            int maxSelect,
            List<MenuModifierOptionDto> options
    ) {
    }

    public record MenuModifierOptionDto(
            Long optionId,
            String optionName,
            long priceAdjustmentCents
    ) {
    }
}
