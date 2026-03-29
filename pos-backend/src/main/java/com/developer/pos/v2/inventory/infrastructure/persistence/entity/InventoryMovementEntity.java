package com.developer.pos.v2.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "V2InventoryMovementEntity")
@Table(name = "inventory_movements")
public class InventoryMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;

    @Column(name = "batch_id")
    private Long batchId;

    /** PURCHASE | SALE_DEDUCT | WASTE | ADJUSTMENT | TRANSFER_IN | TRANSFER_OUT */
    @Column(name = "movement_type", nullable = false, length = 32)
    private String movementType;

    @Column(name = "quantity_change", nullable = false, precision = 14, scale = 4)
    private BigDecimal quantityChange;

    @Column(name = "unit_cost_cents")
    private Long unitCostCents;

    @Column(name = "balance_after", nullable = false, precision = 14, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "source_type", nullable = false, length = 64)
    private String sourceType;

    @Column(name = "source_ref", length = 128)
    private String sourceRef;

    @Column(name = "notes", length = 512)
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected InventoryMovementEntity() {}

    public InventoryMovementEntity(Long storeId, Long inventoryItemId, Long batchId,
                                    String movementType, BigDecimal quantityChange,
                                    Long unitCostCents, BigDecimal balanceAfter,
                                    String sourceType, String sourceRef) {
        this.storeId = storeId;
        this.inventoryItemId = inventoryItemId;
        this.batchId = batchId;
        this.movementType = movementType;
        this.quantityChange = quantityChange;
        this.unitCostCents = unitCostCents;
        this.balanceAfter = balanceAfter;
        this.sourceType = sourceType;
        this.sourceRef = sourceRef;
        this.createdAt = LocalDateTime.now();
    }

    public InventoryMovementEntity(Long storeId, Long inventoryItemId, Long batchId,
                                    String movementType, BigDecimal quantityChange,
                                    Long unitCostCents, BigDecimal balanceAfter,
                                    String sourceType, String sourceRef, String notes) {
        this(storeId, inventoryItemId, batchId, movementType, quantityChange,
                unitCostCents, balanceAfter, sourceType, sourceRef);
        this.notes = notes;
    }

    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public Long getInventoryItemId() { return inventoryItemId; }
    public Long getBatchId() { return batchId; }
    public String getMovementType() { return movementType; }
    public BigDecimal getQuantityChange() { return quantityChange; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getSourceType() { return sourceType; }
    public String getSourceRef() { return sourceRef; }
    public Long getUnitCostCents() { return unitCostCents; }
    public String getNotes() { return notes; }
}
