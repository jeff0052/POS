package com.developer.pos.v2.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity(name = "V2PurchaseInvoiceItemEntity")
@Table(name = "purchase_invoice_items")
public class PurchaseInvoiceItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id", nullable = false)
    private Long invoiceId;

    @Column(name = "inventory_item_id", nullable = false)
    private Long inventoryItemId;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit", nullable = false, length = 32)
    private String unit;

    @Column(name = "unit_price_cents", nullable = false)
    private Long unitPriceCents;

    @Column(name = "line_total_cents", nullable = false)
    private Long lineTotalCents;

    protected PurchaseInvoiceItemEntity() {}

    public PurchaseInvoiceItemEntity(Long invoiceId, Long inventoryItemId,
                                      BigDecimal quantity, String unit,
                                      Long unitPriceCents) {
        this.invoiceId = invoiceId;
        this.inventoryItemId = inventoryItemId;
        this.quantity = quantity;
        this.unit = unit;
        this.unitPriceCents = unitPriceCents;
        this.lineTotalCents = java.math.BigDecimal.valueOf(unitPriceCents).multiply(quantity)
            .setScale(0, java.math.RoundingMode.HALF_UP).longValue();
    }

    public Long getId() { return id; }
    public Long getInvoiceId() { return invoiceId; }
    public Long getInventoryItemId() { return inventoryItemId; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public Long getUnitPriceCents() { return unitPriceCents; }
    public Long getLineTotalCents() { return lineTotalCents; }
}
