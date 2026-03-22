package com.developer.pos.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    private Long id;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "paid_amount_cents")
    private Long paidAmountCents;

    @Column(name = "order_status")
    private String orderStatus;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "print_status")
    private String printStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public Long getStoreId() {
        return storeId;
    }

    public Long getCashierId() {
        return cashierId;
    }

    public Long getPaidAmountCents() {
        return paidAmountCents;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getPrintStatus() {
        return printStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
