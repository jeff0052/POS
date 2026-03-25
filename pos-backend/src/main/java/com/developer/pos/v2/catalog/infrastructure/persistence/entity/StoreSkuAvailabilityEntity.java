package com.developer.pos.v2.catalog.infrastructure.persistence.entity;

import com.developer.pos.v2.common.entity.BaseAuditableEntity;
import com.developer.pos.v2.mcp.ActionContextAuditListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2StoreSkuAvailabilityEntity")
@Table(name = "store_sku_availability")
@EntityListeners(ActionContextAuditListener.class)
public class StoreSkuAvailabilityEntity extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "is_available", nullable = false)
    private boolean available;

    protected StoreSkuAvailabilityEntity() {
    }

    public StoreSkuAvailabilityEntity(Long storeId, Long skuId, boolean available) {
        this.storeId = storeId;
        this.skuId = skuId;
        this.available = available;
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

    public void updateAvailability(boolean available) {
        this.available = available;
    }
}
