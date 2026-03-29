package com.developer.pos.v2.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity(name = "V2ModifierOptionEntity")
@Table(name = "modifier_options")
public class ModifierOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "option_code", nullable = false, length = 64)
    private String optionCode;

    @Column(name = "option_name", nullable = false, length = 128)
    private String optionName;

    @Column(name = "price_adjustment_cents")
    private long priceAdjustmentCents;

    @Column(name = "is_default")
    private boolean defaultOption;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ModifierOptionEntity() {
    }

    public ModifierOptionEntity(Long groupId, String optionCode, String optionName,
                                long priceAdjustmentCents, boolean defaultOption, int sortOrder) {
        this.groupId = groupId;
        this.optionCode = optionCode;
        this.optionName = optionName;
        this.priceAdjustmentCents = priceAdjustmentCents;
        this.defaultOption = defaultOption;
        this.sortOrder = sortOrder;
    }

    public void update(String optionCode, String optionName, long priceAdjustmentCents,
                       boolean defaultOption, int sortOrder) {
        this.optionCode = optionCode;
        this.optionName = optionName;
        this.priceAdjustmentCents = priceAdjustmentCents;
        this.defaultOption = defaultOption;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getOptionCode() { return optionCode; }
    public String getOptionName() { return optionName; }
    public long getPriceAdjustmentCents() { return priceAdjustmentCents; }
    public boolean isDefaultOption() { return defaultOption; }
    public int getSortOrder() { return sortOrder; }
}
