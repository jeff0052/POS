package com.developer.pos.category.dto;

public record CategoryDto(
    Long id,
    Long storeId,
    String name,
    Integer sortOrder,
    String status
) {
}
