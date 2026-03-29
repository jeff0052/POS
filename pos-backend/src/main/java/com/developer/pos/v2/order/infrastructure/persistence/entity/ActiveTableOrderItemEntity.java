package com.developer.pos.v2.order.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "active_table_order_items")
public class ActiveTableOrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "active_order_id", nullable = false)
    private ActiveTableOrderEntity activeTableOrder;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "sku_name_snapshot", nullable = false)
    private String skuNameSnapshot;

    @Column(name = "sku_code_snapshot", nullable = false)
    private String skuCodeSnapshot;

    @Column(name = "unit_price_snapshot_cents", nullable = false)
    private long unitPriceSnapshotCents;

    @Column(name = "member_price_snapshot_cents")
    private Long memberPriceSnapshotCents;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "item_remark")
    private String itemRemark;

    @Column(name = "line_total_cents", nullable = false)
    private long lineTotalCents;

    @Column(name = "is_buffet_included")
    private boolean buffetIncluded;

    @Column(name = "buffet_surcharge_cents")
    private long buffetSurchargeCents;

    @Column(name = "buffet_inclusion_type", length = 32)
    private String buffetInclusionType; // INCLUDED, SURCHARGE, EXCLUDED

    public ActiveTableOrderItemEntity() {
    }

    public Long getId() {
        return id;
    }

    public ActiveTableOrderEntity getActiveTableOrder() {
        return activeTableOrder;
    }

    public void setActiveTableOrder(ActiveTableOrderEntity activeTableOrder) {
        this.activeTableOrder = activeTableOrder;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public String getSkuNameSnapshot() {
        return skuNameSnapshot;
    }

    public void setSkuNameSnapshot(String skuNameSnapshot) {
        this.skuNameSnapshot = skuNameSnapshot;
    }

    public String getSkuCodeSnapshot() {
        return skuCodeSnapshot;
    }

    public void setSkuCodeSnapshot(String skuCodeSnapshot) {
        this.skuCodeSnapshot = skuCodeSnapshot;
    }

    public long getUnitPriceSnapshotCents() {
        return unitPriceSnapshotCents;
    }

    public void setUnitPriceSnapshotCents(long unitPriceSnapshotCents) {
        this.unitPriceSnapshotCents = unitPriceSnapshotCents;
    }

    public Long getMemberPriceSnapshotCents() {
        return memberPriceSnapshotCents;
    }

    public void setMemberPriceSnapshotCents(Long memberPriceSnapshotCents) {
        this.memberPriceSnapshotCents = memberPriceSnapshotCents;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getItemRemark() {
        return itemRemark;
    }

    public void setItemRemark(String itemRemark) {
        this.itemRemark = itemRemark;
    }

    public long getLineTotalCents() {
        return lineTotalCents;
    }

    public void setLineTotalCents(long lineTotalCents) {
        this.lineTotalCents = lineTotalCents;
    }

    public boolean isBuffetIncluded() {
        return buffetIncluded;
    }

    public void setBuffetIncluded(boolean buffetIncluded) {
        this.buffetIncluded = buffetIncluded;
    }

    public long getBuffetSurchargeCents() {
        return buffetSurchargeCents;
    }

    public void setBuffetSurchargeCents(long buffetSurchargeCents) {
        this.buffetSurchargeCents = buffetSurchargeCents;
    }

    public String getBuffetInclusionType() {
        return buffetInclusionType;
    }

    public void setBuffetInclusionType(String buffetInclusionType) {
        this.buffetInclusionType = buffetInclusionType;
    }
}
