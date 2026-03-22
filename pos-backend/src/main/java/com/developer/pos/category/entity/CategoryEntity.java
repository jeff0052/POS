package com.developer.pos.category.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_categories")
public class CategoryEntity {

    @Id
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder;

    private String status;

    private Integer deleted;

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getName() {
        return name;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getStatus() {
        return status;
    }

    public Integer getDeleted() {
        return deleted;
    }
}
