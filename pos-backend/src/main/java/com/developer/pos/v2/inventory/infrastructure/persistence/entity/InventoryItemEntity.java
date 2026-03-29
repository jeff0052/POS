package com.developer.pos.v2.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "V2InventoryItemEntity")
@Table(name = "inventory_items")
public class InventoryItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "item_code", nullable = false, length = 64)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "unit", nullable = false, length = 32)
    private String unit;

    @Column(name = "purchase_unit", length = 32)
    private String purchaseUnit;

    @Column(name = "purchase_to_stock_ratio", precision = 10, scale = 4)
    private BigDecimal purchaseToStockRatio = BigDecimal.ONE;

    @Column(name = "usage_unit", length = 32)
    private String usageUnit;

    @Column(name = "stock_to_usage_ratio", precision = 10, scale = 4)
    private BigDecimal stockToUsageRatio = BigDecimal.ONE;

    @Column(name = "stock_unit", length = 32)
    private String stockUnit;

    @Column(name = "unit_conversion_factor", precision = 10, scale = 4)
    private BigDecimal unitConversionFactor = BigDecimal.ONE;

    @Column(name = "current_stock", precision = 14, scale = 4, nullable = false)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(name = "safety_stock", precision = 14, scale = 4, nullable = false)
    private BigDecimal safetyStock = BigDecimal.ZERO;

    @Column(name = "expiry_warning_days")
    private Integer expiryWarningDays = 3;

    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays;

    @Column(name = "requires_batch_tracking")
    private Boolean requiresBatchTracking = false;

    @Column(name = "item_status", length = 32, nullable = false)
    private String itemStatus = "ACTIVE";

    @Column(name = "default_supplier_id")
    private Long defaultSupplierId;

    @Column(name = "last_purchase_price_cents")
    private Long lastPurchasePriceCents;

    @Column(name = "avg_daily_usage", precision = 14, scale = 4)
    private BigDecimal avgDailyUsage = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected InventoryItemEntity() {}

    public InventoryItemEntity(Long storeId, String itemCode, String itemName,
                                String unit, BigDecimal safetyStock) {
        this.storeId = storeId;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.unit = unit;
        this.safetyStock = safetyStock != null ? safetyStock : BigDecimal.ZERO;
        this.currentStock = BigDecimal.ZERO;
        this.itemStatus = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void addStock(BigDecimal qty) {
        this.currentStock = this.currentStock.add(qty);
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.itemStatus = "INACTIVE";
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public String getCategory() { return category; }
    public String getUnit() { return unit; }
    public BigDecimal getCurrentStock() { return currentStock; }
    public BigDecimal getSafetyStock() { return safetyStock; }
    public String getItemStatus() { return itemStatus; }
    public Long getDefaultSupplierId() { return defaultSupplierId; }
    public Integer getExpiryWarningDays() { return expiryWarningDays; }
    public Boolean getRequiresBatchTracking() { return requiresBatchTracking; }

    // Setters for updates
    public void setItemName(String itemName) { this.itemName = itemName; this.updatedAt = LocalDateTime.now(); }
    public void setCategory(String category) { this.category = category; this.updatedAt = LocalDateTime.now(); }
    public void setSafetyStock(BigDecimal safetyStock) { this.safetyStock = safetyStock; this.updatedAt = LocalDateTime.now(); }
    public void setDefaultSupplierId(Long defaultSupplierId) { this.defaultSupplierId = defaultSupplierId; this.updatedAt = LocalDateTime.now(); }
    public void setLastPurchasePriceCents(Long price) { this.lastPurchasePriceCents = price; this.updatedAt = LocalDateTime.now(); }
}
