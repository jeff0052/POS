package com.developer.pos.v2.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity(name = "V2SkuPriceOverrideEntity")
@Table(name = "sku_price_overrides")
public class SkuPriceOverrideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "price_context", nullable = false, length = 64)
    private String priceContext;

    @Column(name = "price_context_ref", length = 128)
    private String priceContextRef;

    @Column(name = "override_price_cents", nullable = false)
    private long overridePriceCents;

    @Column(name = "is_active")
    private boolean active;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected SkuPriceOverrideEntity() {
    }

    public SkuPriceOverrideEntity(Long skuId, Long storeId, String priceContext,
                                  String priceContextRef, long overridePriceCents, boolean active) {
        this.skuId = skuId;
        this.storeId = storeId;
        this.priceContext = priceContext;
        this.priceContextRef = priceContextRef;
        this.overridePriceCents = overridePriceCents;
        this.active = active;
    }

    public Long getId() { return id; }
    public Long getSkuId() { return skuId; }
    public Long getStoreId() { return storeId; }
    public String getPriceContext() { return priceContext; }
    public String getPriceContextRef() { return priceContextRef; }
    public long getOverridePriceCents() { return overridePriceCents; }
    public boolean isActive() { return active; }
}
