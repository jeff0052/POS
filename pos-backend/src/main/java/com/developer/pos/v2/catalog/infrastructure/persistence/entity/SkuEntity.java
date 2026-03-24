package com.developer.pos.v2.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2SkuEntity")
@Table(name = "skus")
public class SkuEntity {

    @Id
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku_code", nullable = false)
    private String skuCode;

    @Column(name = "sku_name", nullable = false)
    private String skuName;

    @Column(name = "base_price_cents", nullable = false)
    private long basePriceCents;

    @Column(name = "sku_status", nullable = false)
    private String skuStatus;

    protected SkuEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public String getSkuName() {
        return skuName;
    }

    public long getBasePriceCents() {
        return basePriceCents;
    }

    public String getSkuStatus() {
        return skuStatus;
    }
}
