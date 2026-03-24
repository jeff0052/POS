package com.developer.pos.v2.order.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submitted_orders")
public class SubmittedOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submitted_order_id", nullable = false)
    private String submittedOrderId;

    @Column(name = "table_session_id", nullable = false)
    private Long tableSessionId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "source_order_type", nullable = false)
    private String sourceOrderType;

    @Column(name = "source_active_order_id")
    private String sourceActiveOrderId;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    @Column(name = "fulfillment_status", nullable = false)
    private String fulfillmentStatus;

    @Column(name = "settlement_status", nullable = false)
    private String settlementStatus;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "original_amount_cents", nullable = false)
    private long originalAmountCents;

    @Column(name = "member_discount_cents", nullable = false)
    private long memberDiscountCents;

    @Column(name = "promotion_discount_cents", nullable = false)
    private long promotionDiscountCents;

    @Column(name = "payable_amount_cents", nullable = false)
    private long payableAmountCents;

    @Column(name = "settled_at")
    private OffsetDateTime settledAt;

    @OneToMany(mappedBy = "submittedOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubmittedOrderItemEntity> items = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getSubmittedOrderId() {
        return submittedOrderId;
    }

    public void setSubmittedOrderId(String submittedOrderId) {
        this.submittedOrderId = submittedOrderId;
    }

    public Long getTableSessionId() {
        return tableSessionId;
    }

    public void setTableSessionId(Long tableSessionId) {
        this.tableSessionId = tableSessionId;
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

    public String getSourceOrderType() {
        return sourceOrderType;
    }

    public void setSourceOrderType(String sourceOrderType) {
        this.sourceOrderType = sourceOrderType;
    }

    public String getSourceActiveOrderId() {
        return sourceActiveOrderId;
    }

    public void setSourceActiveOrderId(String sourceActiveOrderId) {
        this.sourceActiveOrderId = sourceActiveOrderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getFulfillmentStatus() {
        return fulfillmentStatus;
    }

    public void setFulfillmentStatus(String fulfillmentStatus) {
        this.fulfillmentStatus = fulfillmentStatus;
    }

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(String settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
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

    public OffsetDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(OffsetDateTime settledAt) {
        this.settledAt = settledAt;
    }

    public List<SubmittedOrderItemEntity> getItems() {
        return items;
    }

    public void replaceItems(List<SubmittedOrderItemEntity> nextItems) {
        this.items.clear();
        nextItems.forEach(this::addItem);
    }

    public void addItem(SubmittedOrderItemEntity item) {
        item.setSubmittedOrder(this);
        this.items.add(item);
    }
}
