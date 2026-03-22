package com.developer.pos.product.dto;

public record ProductDto(
    Long id,
    Long storeId,
    Long categoryId,
    String name,
    String barcode,
    Long priceCents,
    Integer stockQty,
    String status,
    String categoryName
) {
}
