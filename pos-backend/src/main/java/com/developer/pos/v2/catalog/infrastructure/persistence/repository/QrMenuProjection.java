package com.developer.pos.v2.catalog.infrastructure.persistence.repository;

public interface QrMenuProjection {
    Long getStoreId();

    String getStoreCode();

    String getStoreName();

    Long getCategoryId();

    String getCategoryCode();

    String getCategoryName();

    Integer getCategorySortOrder();

    Long getProductId();

    String getProductCode();

    String getProductName();

    Long getSkuId();

    String getSkuCode();

    String getSkuName();

    Long getUnitPriceCents();
}
