package com.developer.pos.v2.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.List;

@Entity(name = "V2ProductEntity")
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "image_id", length = 64)
    private String imageId;

    @Column(name = "menu_modes", columnDefinition = "JSON")
    @Convert(converter = StringListConverter.class)
    private List<String> menuModes;

    @Column(name = "sort_order")
    private int sortOrder;

    @Column(name = "product_status", nullable = false)
    private String productStatus;

    @Column(name = "attribute_config_json", columnDefinition = "TEXT")
    private String attributeConfigJson;

    @Column(name = "modifier_config_json", columnDefinition = "TEXT")
    private String modifierConfigJson;

    @Column(name = "combo_slot_config_json", columnDefinition = "TEXT")
    private String comboSlotConfigJson;

    protected ProductEntity() {
    }

    public ProductEntity(
            Long storeId,
            Long categoryId,
            String productCode,
            String productName,
            String productStatus,
            String attributeConfigJson,
            String modifierConfigJson,
            String comboSlotConfigJson
    ) {
        this.storeId = storeId;
        this.categoryId = categoryId;
        this.productCode = productCode;
        this.productName = productName;
        this.productStatus = productStatus;
        this.attributeConfigJson = attributeConfigJson;
        this.modifierConfigJson = modifierConfigJson;
        this.comboSlotConfigJson = comboSlotConfigJson;
    }

    public Long getId() {
        return id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getProductName() {
        return productName;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getProductStatus() {
        return productStatus;
    }

    public List<String> getMenuModes() {
        return menuModes;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getAttributeConfigJson() {
        return attributeConfigJson;
    }

    public String getModifierConfigJson() {
        return modifierConfigJson;
    }

    public String getComboSlotConfigJson() {
        return comboSlotConfigJson;
    }

    public void update(
            Long categoryId,
            String productCode,
            String productName,
            String productStatus,
            String attributeConfigJson,
            String modifierConfigJson,
            String comboSlotConfigJson
    ) {
        this.categoryId = categoryId;
        this.productCode = productCode;
        this.productName = productName;
        this.productStatus = productStatus;
        this.attributeConfigJson = attributeConfigJson;
        this.modifierConfigJson = modifierConfigJson;
        this.comboSlotConfigJson = comboSlotConfigJson;
    }
}
