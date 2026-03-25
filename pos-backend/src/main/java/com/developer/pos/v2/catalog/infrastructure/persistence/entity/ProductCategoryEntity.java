package com.developer.pos.v2.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2ProductCategoryEntity")
@Table(name = "product_categories")
public class ProductCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "category_code", nullable = false)
    private String categoryCode;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ProductCategoryEntity() {
    }

    public ProductCategoryEntity(
            Long storeId,
            String categoryCode,
            String categoryName,
            boolean active,
            int sortOrder
    ) {
        this.storeId = storeId;
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.active = active;
        this.sortOrder = sortOrder;
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public boolean isActive() {
        return active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void update(String categoryCode, String categoryName, boolean active, int sortOrder) {
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.active = active;
        this.sortOrder = sortOrder;
    }
}
