package com.developer.pos.v2.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Merchant-level supplier entity. Visible across all stores within the same merchant. */
@Entity(name = "V2SupplierEntity")
@Table(name = "suppliers")
public class SupplierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "supplier_code", nullable = false, length = 64)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, length = 255)
    private String supplierName;

    @Column(name = "contact_name", length = 128)
    private String contactName;

    @Column(name = "contact_phone", length = 64)
    private String contactPhone;

    @Column(name = "supplier_status", length = 32, nullable = false)
    private String supplierStatus = "ACTIVE";

    @Column(name = "lead_time_days")
    private Integer leadTimeDays = 1;

    @Column(name = "rating", precision = 3, scale = 1)
    private BigDecimal rating;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "payment_terms", length = 64)
    private String paymentTerms;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected SupplierEntity() {}

    public Long getId() { return id; }
    public Long getMerchantId() { return merchantId; }
    public String getSupplierCode() { return supplierCode; }
    public String getSupplierName() { return supplierName; }
    public String getSupplierStatus() { return supplierStatus; }
}
