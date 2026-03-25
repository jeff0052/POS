package com.developer.pos.v2.order.infrastructure.persistence.entity;

import com.developer.pos.v2.common.entity.BaseAuditableEntity;
import com.developer.pos.v2.mcp.ActionContextAuditListener;
import com.developer.pos.v2.order.domain.source.OrderSource;
import com.developer.pos.v2.order.domain.status.ActiveOrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "active_table_orders")
@EntityListeners(ActionContextAuditListener.class)
public class ActiveTableOrderEntity extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "active_order_id", nullable = false)
    private String activeOrderId;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_source", nullable = false)
    private OrderSource orderSource;

    @Column(name = "dining_type", nullable = false)
    private String diningType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActiveOrderStatus status;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "cashier_id")
    private Long cashierId;

    @Column(name = "current_shift_id")
    private Long currentShiftId;

    @Column(name = "original_amount_cents", nullable = false)
    private long originalAmountCents;

    @Column(name = "member_discount_cents", nullable = false)
    private long memberDiscountCents;

    @Column(name = "promotion_discount_cents", nullable = false)
    private long promotionDiscountCents;

    @Column(name = "payable_amount_cents", nullable = false)
    private long payableAmountCents;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "activeTableOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActiveTableOrderItemEntity> items = new ArrayList<>();

    public ActiveTableOrderEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getActiveOrderId() {
        return activeOrderId;
    }

    public void setActiveOrderId(String activeOrderId) {
        this.activeOrderId = activeOrderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public OrderSource getOrderSource() {
        return orderSource;
    }

    public void setOrderSource(OrderSource orderSource) {
        this.orderSource = orderSource;
    }

    public String getDiningType() {
        return diningType;
    }

    public void setDiningType(String diningType) {
        this.diningType = diningType;
    }

    public ActiveOrderStatus getStatus() {
        return status;
    }

    public void setStatus(ActiveOrderStatus status) {
        this.status = status;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getCashierId() {
        return cashierId;
    }

    public void setCashierId(Long cashierId) {
        this.cashierId = cashierId;
    }

    public Long getCurrentShiftId() {
        return currentShiftId;
    }

    public void setCurrentShiftId(Long currentShiftId) {
        this.currentShiftId = currentShiftId;
    }

    public long getOriginalAmountCents() {
        return originalAmountCents;
    }

    public void setOriginalAmountCents(long originalAmountCents) {
        this.originalAmountCents = originalAmountCents;
    }

    public long getMemberDiscountCents() {
        return memberDiscountCents;
    }

    public void setMemberDiscountCents(long memberDiscountCents) {
        this.memberDiscountCents = memberDiscountCents;
    }

    public long getPromotionDiscountCents() {
        return promotionDiscountCents;
    }

    public void setPromotionDiscountCents(long promotionDiscountCents) {
        this.promotionDiscountCents = promotionDiscountCents;
    }

    public long getPayableAmountCents() {
        return payableAmountCents;
    }

    public void setPayableAmountCents(long payableAmountCents) {
        this.payableAmountCents = payableAmountCents;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public List<ActiveTableOrderItemEntity> getItems() {
        return items;
    }

    public void replaceItems(List<ActiveTableOrderItemEntity> nextItems) {
        this.items.clear();
        nextItems.forEach(this::addItem);
    }

    public void addItem(ActiveTableOrderItemEntity item) {
        item.setActiveTableOrder(this);
        this.items.add(item);
    }
}
