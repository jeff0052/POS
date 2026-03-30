package com.developer.pos.v2.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "V2InventoryDrivenPromotionEntity")
@Table(name = "inventory_driven_promotions")
public class InventoryDrivenPromotionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;

    @Column(name = "inventory_batch_id")
    private Long inventoryBatchId;

    @Column(name = "trigger_type", nullable = false, length = 32)
    private String triggerType;

    @Column(name = "suggested_discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal suggestedDiscountPercent;

    @Column(name = "suggested_sku_ids", nullable = false, columnDefinition = "JSON")
    private String suggestedSkuIds;

    @Column(name = "draft_status", nullable = false, length = 32)
    private String draftStatus = "DRAFT";

    @Column(name = "promotion_rule_id")
    private Long promotionRuleId;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected InventoryDrivenPromotionEntity() {}

    public InventoryDrivenPromotionEntity(Long storeId, Long inventoryItemId, Long inventoryBatchId,
                                           String triggerType, BigDecimal suggestedDiscountPercent,
                                           String suggestedSkuIds, LocalDateTime expiresAt) {
        this.storeId = storeId;
        this.inventoryItemId = inventoryItemId;
        this.inventoryBatchId = inventoryBatchId;
        this.triggerType = triggerType;
        this.suggestedDiscountPercent = suggestedDiscountPercent;
        this.suggestedSkuIds = suggestedSkuIds;
        this.draftStatus = "DRAFT";
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void approve(Long userId, Long promotionRuleId) {
        if (!"DRAFT".equals(this.draftStatus)) {
            throw new IllegalStateException("Promotion draft " + id + " is not in DRAFT status: " + draftStatus);
        }
        this.draftStatus = "APPROVED";
        this.approvedBy = userId;
        this.approvedAt = LocalDateTime.now();
        this.promotionRuleId = promotionRuleId;
        this.updatedAt = LocalDateTime.now();
    }

    public void reject(Long userId) {
        if (!"DRAFT".equals(this.draftStatus)) {
            throw new IllegalStateException("Promotion draft " + id + " is not in DRAFT status: " + draftStatus);
        }
        this.draftStatus = "REJECTED";
        this.approvedBy = userId;
        this.approvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void expire() {
        this.draftStatus = "EXPIRED";
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public Long getInventoryItemId() { return inventoryItemId; }
    public Long getInventoryBatchId() { return inventoryBatchId; }
    public String getTriggerType() { return triggerType; }
    public BigDecimal getSuggestedDiscountPercent() { return suggestedDiscountPercent; }
    public String getSuggestedSkuIds() { return suggestedSkuIds; }
    public String getDraftStatus() { return draftStatus; }
    public Long getPromotionRuleId() { return promotionRuleId; }
    public Long getApprovedBy() { return approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
