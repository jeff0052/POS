package com.developer.pos.v2.catalog.domain.model;

public record SkuRef(
        Long skuId,
        String skuCode,
        String skuName
) {
}
