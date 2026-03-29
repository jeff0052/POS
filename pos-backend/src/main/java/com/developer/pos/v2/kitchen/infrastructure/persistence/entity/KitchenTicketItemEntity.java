package com.developer.pos.v2.kitchen.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity(name = "V2KitchenTicketItemEntity")
@Table(name = "kitchen_ticket_items")
public class KitchenTicketItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private KitchenTicketEntity ticket;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "sku_name_snapshot", nullable = false)
    private String skuNameSnapshot;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "item_remark")
    private String itemRemark;

    @Column(name = "option_snapshot_json", columnDefinition = "JSON")
    private String optionSnapshotJson;

    protected KitchenTicketItemEntity() {}

    public KitchenTicketItemEntity(KitchenTicketEntity ticket, Long skuId, String skuNameSnapshot,
                                    int quantity, String itemRemark) {
        this.ticket = ticket;
        this.skuId = skuId;
        this.skuNameSnapshot = skuNameSnapshot;
        this.quantity = quantity;
        this.itemRemark = itemRemark;
    }

    public Long getId() { return id; }
    public Long getSkuId() { return skuId; }
    public String getSkuNameSnapshot() { return skuNameSnapshot; }
    public int getQuantity() { return quantity; }
    public String getItemRemark() { return itemRemark; }
    public String getOptionSnapshotJson() { return optionSnapshotJson; }
    public void setOptionSnapshotJson(String optionSnapshotJson) { this.optionSnapshotJson = optionSnapshotJson; }
}
