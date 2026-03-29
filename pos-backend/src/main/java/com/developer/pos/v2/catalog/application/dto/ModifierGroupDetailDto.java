package com.developer.pos.v2.catalog.application.dto;

import java.util.List;

public record ModifierGroupDetailDto(
        Long id,
        String groupCode,
        String groupName,
        String selectionType,
        boolean required,
        int minSelect,
        int maxSelect,
        int sortOrder,
        List<ModifierOptionDetailDto> options
) {
}
