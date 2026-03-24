package com.developer.pos.v2.store.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2StoreEntity")
@Table(name = "stores")
public class StoreEntity {

    @Id
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "store_code", nullable = false)
    private String storeCode;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    protected StoreEntity() {
    }

    public Long getId() {
        return id;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public String getStoreName() {
        return storeName;
    }
}
