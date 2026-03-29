package com.developer.pos.v2.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity(name = "V2ModifierGroupEntity")
@Table(name = "modifier_groups")
public class ModifierGroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "group_code", nullable = false, length = 64)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 128)
    private String groupName;

    @Column(name = "selection_type", nullable = false, length = 16)
    private String selectionType;

    @Column(name = "is_required")
    private boolean required;

    @Column(name = "min_select")
    private int minSelect;

    @Column(name = "max_select")
    private int maxSelect;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ModifierGroupEntity() {
    }

    public ModifierGroupEntity(Long merchantId, String groupCode, String groupName,
                               String selectionType, boolean required, int minSelect, int maxSelect, int sortOrder) {
        this.merchantId = merchantId;
        this.groupCode = groupCode;
        this.groupName = groupName;
        this.selectionType = selectionType;
        this.required = required;
        this.minSelect = minSelect;
        this.maxSelect = maxSelect;
        this.sortOrder = sortOrder;
    }

    public void update(String groupCode, String groupName, String selectionType,
                       boolean required, int minSelect, int maxSelect, int sortOrder) {
        this.groupCode = groupCode;
        this.groupName = groupName;
        this.selectionType = selectionType;
        this.required = required;
        this.minSelect = minSelect;
        this.maxSelect = maxSelect;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public Long getMerchantId() { return merchantId; }
    public String getGroupCode() { return groupCode; }
    public String getGroupName() { return groupName; }
    public String getSelectionType() { return selectionType; }
    public boolean isRequired() { return required; }
    public int getMinSelect() { return minSelect; }
    public int getMaxSelect() { return maxSelect; }
    public int getSortOrder() { return sortOrder; }
}
