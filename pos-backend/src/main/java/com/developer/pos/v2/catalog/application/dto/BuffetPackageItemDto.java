package com.developer.pos.v2.catalog.application.dto;

public record BuffetPackageItemDto(
    Long itemId, Long skuId, String skuCode, String skuName,
    String inclusionType, long surchargeCents,
    Integer maxQtyPerPerson, int sortOrder
) {}
