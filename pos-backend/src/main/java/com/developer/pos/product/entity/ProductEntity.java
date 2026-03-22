package com.developer.pos.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "category_id")
    private Long categoryId;

    private String name;

    private String barcode;

    @Column(name = "price_cents")
    private Long priceCents;

    @Column(name = "stock_qty")
    private Integer stockQty;

    private String status;

    private Integer deleted;

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getBarcode() {
        return barcode;
    }

    public Long getPriceCents() {
        return priceCents;
    }

    public Integer getStockQty() {
        return stockQty;
    }

    public String getStatus() {
        return status;
    }

    public Integer getDeleted() {
        return deleted;
    }
}
