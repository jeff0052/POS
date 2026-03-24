package com.developer.pos.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "qr_table_orders")
public class QrTableOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    @Column(name = "queue_no", nullable = false)
    private String queueNo;

    @Column(name = "store_code", nullable = false)
    private String storeCode;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "table_code", nullable = false)
    private String tableCode;

    @Column(name = "settlement_status", nullable = false)
    private String settlementStatus;

    @Column(name = "member_name")
    private String memberName;

    @Column(name = "member_tier")
    private String memberTier;

    @Column(name = "original_amount_cents", nullable = false)
    private Long originalAmountCents;

    @Column(name = "member_discount_cents", nullable = false)
    private Long memberDiscountCents;

    @Column(name = "promotion_discount_cents", nullable = false)
    private Long promotionDiscountCents;

    @Column(name = "payable_amount_cents", nullable = false)
    private Long payableAmountCents;

    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getQueueNo() {
        return queueNo;
    }

    public void setQueueNo(String queueNo) {
        this.queueNo = queueNo;
    }

    public String getStoreCode() {
        return storeCode;
    }

    public void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getTableCode() {
        return tableCode;
    }

    public void setTableCode(String tableCode) {
        this.tableCode = tableCode;
    }

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(String settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMemberTier() {
        return memberTier;
    }

    public void setMemberTier(String memberTier) {
        this.memberTier = memberTier;
    }

    public Long getOriginalAmountCents() {
        return originalAmountCents;
    }

    public void setOriginalAmountCents(Long originalAmountCents) {
        this.originalAmountCents = originalAmountCents;
    }

    public Long getMemberDiscountCents() {
        return memberDiscountCents;
    }

    public void setMemberDiscountCents(Long memberDiscountCents) {
        this.memberDiscountCents = memberDiscountCents;
    }

    public Long getPromotionDiscountCents() {
        return promotionDiscountCents;
    }

    public void setPromotionDiscountCents(Long promotionDiscountCents) {
        this.promotionDiscountCents = promotionDiscountCents;
    }

    public Long getPayableAmountCents() {
        return payableAmountCents;
    }

    public void setPayableAmountCents(Long payableAmountCents) {
        this.payableAmountCents = payableAmountCents;
    }

    public String getItemsJson() {
        return itemsJson;
    }

    public void setItemsJson(String itemsJson) {
        this.itemsJson = itemsJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
