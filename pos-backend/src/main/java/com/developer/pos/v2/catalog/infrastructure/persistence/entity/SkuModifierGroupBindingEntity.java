package com.developer.pos.v2.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "V2SkuModifierGroupBindingEntity")
@Table(name = "sku_modifier_group_bindings")
public class SkuModifierGroupBindingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "modifier_group_id", nullable = false)
    private Long modifierGroupId;

    @Column(name = "sort_order")
    private int sortOrder;

    protected SkuModifierGroupBindingEntity() {
    }

    public SkuModifierGroupBindingEntity(Long skuId, Long modifierGroupId, int sortOrder) {
        this.skuId = skuId;
        this.modifierGroupId = modifierGroupId;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public Long getSkuId() { return skuId; }
    public Long getModifierGroupId() { return modifierGroupId; }
    public int getSortOrder() { return sortOrder; }
}
