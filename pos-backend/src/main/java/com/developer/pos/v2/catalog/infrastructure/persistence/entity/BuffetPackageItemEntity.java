package com.developer.pos.v2.catalog.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "V2BuffetPackageItemEntity")
@Table(name = "buffet_package_items")
public class BuffetPackageItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "inclusion_type", nullable = false, length = 32)
    private String inclusionType; // INCLUDED, SURCHARGE, EXCLUDED

    @Column(name = "surcharge_cents", nullable = false)
    private long surchargeCents;

    @Column(name = "max_qty_per_person")
    private Integer maxQtyPerPerson;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BuffetPackageItemEntity() {}

    public BuffetPackageItemEntity(Long packageId, Long skuId, String inclusionType,
                                   long surchargeCents, Integer maxQtyPerPerson, int sortOrder) {
        this.packageId = packageId;
        this.skuId = skuId;
        this.inclusionType = inclusionType;
        this.surchargeCents = surchargeCents;
        this.maxQtyPerPerson = maxQtyPerPerson;
        this.sortOrder = sortOrder;
    }

    public void update(String inclusionType, long surchargeCents,
                       Integer maxQtyPerPerson, int sortOrder) {
        this.inclusionType = inclusionType;
        this.surchargeCents = surchargeCents;
        this.maxQtyPerPerson = maxQtyPerPerson;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public Long getPackageId() { return packageId; }
    public Long getSkuId() { return skuId; }
    public String getInclusionType() { return inclusionType; }
    public long getSurchargeCents() { return surchargeCents; }
    public Integer getMaxQtyPerPerson() { return maxQtyPerPerson; }
    public int getSortOrder() { return sortOrder; }
}
