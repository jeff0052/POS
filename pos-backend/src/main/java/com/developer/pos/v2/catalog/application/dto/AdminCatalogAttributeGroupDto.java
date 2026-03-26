package com.developer.pos.v2.catalog.application.dto;

import java.util.List;

public record AdminCatalogAttributeGroupDto(
        String code,
        String name,
        String selectionMode,
        boolean required,
        Integer minSelect,
        Integer maxSelect,
        List<AdminCatalogAttributeValueDto> values
) {
}
