package com.developer.pos.v2.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2StoreSkuAvailabilityEntity")
@Table(name = "store_sku_availability")
public class StoreSkuAvailabilityEntity {

    @Id
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "is_available", nullable = false)
    private boolean available;

    protected StoreSkuAvailabilityEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public boolean isAvailable() {
        return available;
    }
}
