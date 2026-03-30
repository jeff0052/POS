package com.developer.pos.v2.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Recipe linking SKU to inventory item. Store isolation is inherited from the SKU/item entities. */
@Entity(name = "V2RecipeEntity")
@Table(name = "recipes")
public class RecipeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;

    @Column(name = "consumption_qty", nullable = false, precision = 10, scale = 4)
    private BigDecimal consumptionQty;

    @Column(name = "consumption_unit", nullable = false, length = 32)
    private String consumptionUnit;

    /** JSON: [{"modifierOptionId": 101, "extraQty": 0.01, "unit": "kg"}] */
    @Column(name = "modifier_consumption_rules", columnDefinition = "JSON")
    private String modifierConsumptionRules;

    @Column(name = "base_multiplier", precision = 5, scale = 2)
    private BigDecimal baseMultiplier = BigDecimal.ONE;

    @Column(name = "notes", length = 512)
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected RecipeEntity() {}

    public static RecipeEntity create(Long skuId, Long inventoryItemId,
                                        BigDecimal consumptionQty, String consumptionUnit,
                                        BigDecimal baseMultiplier, String notes) {
        RecipeEntity r = new RecipeEntity();
        r.skuId = skuId;
        r.inventoryItemId = inventoryItemId;
        r.consumptionQty = consumptionQty;
        r.consumptionUnit = consumptionUnit;
        r.baseMultiplier = baseMultiplier != null ? baseMultiplier : BigDecimal.ONE;
        r.notes = notes;
        r.createdAt = java.time.LocalDateTime.now();
        return r;
    }

    public Long getId() { return id; }
    public Long getSkuId() { return skuId; }
    public Long getInventoryItemId() { return inventoryItemId; }
    public BigDecimal getConsumptionQty() { return consumptionQty; }
    public String getConsumptionUnit() { return consumptionUnit; }
    public String getModifierConsumptionRules() { return modifierConsumptionRules; }
    public void setModifierConsumptionRules(String rules) { this.modifierConsumptionRules = rules; }
    public BigDecimal getBaseMultiplier() { return baseMultiplier; }
}
