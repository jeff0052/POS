package com.developer.pos.v2.catalog.application.dto;

import java.util.List;

public record QrMenuDto(
        Long storeId,
        String storeCode,
        String storeName,
        List<CategoryDto> categories
) {
    public record CategoryDto(
            Long categoryId,
            String categoryCode,
            String categoryName,
            List<MenuItemDto> items
    ) {
    }

    public record MenuItemDto(
            Long productId,
            String productCode,
            String productName,
            Long skuId,
            String skuCode,
            String skuName,
            long unitPriceCents
    ) {
    }
}
