package com.developer.pos.v2.catalog.application.dto;

import java.util.List;

public record AdminCatalogComboSlotDto(
        String code,
        String name,
        Integer minSelect,
        Integer maxSelect,
        List<String> allowedSkuCodes
) {
}
