package com.developer.pos.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stores")
public class StoreEntity {

    @Id
    private Long id;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "store_code", nullable = false)
    private String storeCode;

    private String address;

    private String phone;

    private String status;

    public Long getId() {
        return id;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public String getStatus() {
        return status;
    }
}
