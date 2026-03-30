package com.developer.pos.v2.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "V2InventoryBatchEntity")
@Table(name = "inventory_batches")
public class InventoryBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;

    @Column(name = "batch_no", nullable = false, length = 64)
    private String batchNo;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "source_ref", length = 128)
    private String sourceRef;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "received_qty", nullable = false, precision = 14, scale = 4)
    private BigDecimal receivedQty;

    @Column(name = "remaining_qty", nullable = false, precision = 14, scale = 4)
    private BigDecimal remainingQty;

    @Column(name = "unit", nullable = false, length = 32)
    private String unit;

    @Column(name = "unit_cost_cents")
    private Long unitCostCents;

    @Column(name = "total_cost_cents")
    private Long totalCostCents;

    @Column(name = "production_date")
    private LocalDate productionDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    @Column(name = "batch_status", length = 32, nullable = false)
    private String batchStatus = "ACTIVE";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    protected InventoryBatchEntity() {}

    /**
     * Creates a batch without supplier or production date.
     * @param storeId        owning store
     * @param inventoryItemId linked inventory item
     * @param batchNo        unique batch number
     * @param sourceType     e.g. PURCHASE, ADJUSTMENT
     * @param sourceRef      reference to source document
     * @param receivedQty    quantity received
     * @param unit           stock unit
     * @param unitCostCents  cost per unit in cents (nullable)
     * @param expiryDate     batch expiry date (nullable)
     */
    public InventoryBatchEntity(Long storeId, Long inventoryItemId, String batchNo,
                                 String sourceType, String sourceRef,
                                 BigDecimal receivedQty, String unit,
                                 Long unitCostCents, LocalDate expiryDate) {
        this.storeId = storeId;
        this.inventoryItemId = inventoryItemId;
        this.batchNo = batchNo;
        this.sourceType = sourceType;
        this.sourceRef = sourceRef;
        this.receivedQty = receivedQty;
        this.remainingQty = receivedQty;
        this.unit = unit;
        this.unitCostCents = unitCostCents;
        this.totalCostCents = computeTotalCostCents(unitCostCents, receivedQty);
        this.expiryDate = expiryDate;
        this.receivedDate = LocalDate.now();
        this.batchStatus = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Creates a batch with supplier and production date.
     * @param storeId         owning store
     * @param inventoryItemId linked inventory item
     * @param batchNo         unique batch number
     * @param sourceType      e.g. PURCHASE, ADJUSTMENT
     * @param supplierId      supplier who provided the batch
     * @param receivedQty     quantity received
     * @param unit            stock unit
     * @param unitCostCents   cost per unit in cents (nullable)
     * @param productionDate  production/manufacture date (nullable)
     * @param expiryDate      batch expiry date (nullable)
     */
    public InventoryBatchEntity(Long storeId, Long inventoryItemId, String batchNo,
                                 String sourceType, Long supplierId,
                                 BigDecimal receivedQty, String unit,
                                 Long unitCostCents, LocalDate productionDate,
                                 LocalDate expiryDate) {
        this.storeId = storeId;
        this.inventoryItemId = inventoryItemId;
        this.batchNo = batchNo;
        this.sourceType = sourceType;
        this.supplierId = supplierId;
        this.receivedQty = receivedQty;
        this.remainingQty = receivedQty;
        this.unit = unit;
        this.unitCostCents = unitCostCents;
        this.totalCostCents = computeTotalCostCents(unitCostCents, receivedQty);
        this.productionDate = productionDate;
        this.expiryDate = expiryDate;
        this.receivedDate = LocalDate.now();
        this.batchStatus = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private static Long computeTotalCostCents(Long unitCostCents, BigDecimal receivedQty) {
        if (unitCostCents == null) return null;
        return BigDecimal.valueOf(unitCostCents)
                .multiply(receivedQty)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public Long getInventoryItemId() { return inventoryItemId; }
    public String getBatchNo() { return batchNo; }
    public String getSourceType() { return sourceType; }
    public BigDecimal getRemainingQty() { return remainingQty; }
    public BigDecimal getReceivedQty() { return receivedQty; }
    public String getUnit() { return unit; }
    public Long getUnitCostCents() { return unitCostCents; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public String getBatchStatus() { return batchStatus; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public void deductRemainingQty(BigDecimal qty) {
        if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Deduction quantity must be positive");
        if (qty.compareTo(this.remainingQty) > 0) {
            throw new IllegalStateException("Cannot deduct " + qty
                + " from batch " + id + " (remaining: " + remainingQty + ")");
        }
        this.remainingQty = this.remainingQty.subtract(qty);
        if (this.remainingQty.compareTo(BigDecimal.ZERO) == 0) {
            this.batchStatus = "EXHAUSTED";
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void exhaust() {
        this.batchStatus = "EXHAUSTED";
        this.updatedAt = LocalDateTime.now();
    }
}
