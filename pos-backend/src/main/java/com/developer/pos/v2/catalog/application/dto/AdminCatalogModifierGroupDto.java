package com.developer.pos.v2.catalog.application.dto;

import java.util.List;

public record AdminCatalogModifierGroupDto(
        String code,
        String name,
        Integer freeQuantity,
        Integer minSelect,
        Integer maxSelect,
        List<AdminCatalogModifierOptionDto> options
) {
}
