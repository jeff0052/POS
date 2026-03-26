package com.developer.pos.v2.gto.infrastructure.persistence.entity;

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
@Table(name = "gto_export_items")
public class GtoExportItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private GtoExportBatchEntity batch;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "payment_scheme")
    private String paymentScheme;

    @Column(name = "sale_count", nullable = false)
    private int saleCount;

    @Column(name = "sale_total_cents", nullable = false)
    private long saleTotalCents;

    @Column(name = "refund_count", nullable = false)
    private int refundCount;

    @Column(name = "refund_total_cents", nullable = false)
    private long refundTotalCents;

    @Column(name = "net_total_cents", nullable = false)
    private long netTotalCents;

    @Column(name = "tax_cents", nullable = false)
    private long taxCents;

    public GtoExportItemEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GtoExportBatchEntity getBatch() {
        return batch;
    }

    public void setBatch(GtoExportBatchEntity batch) {
        this.batch = batch;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(String paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public int getSaleCount() {
        return saleCount;
    }

    public void setSaleCount(int saleCount) {
        this.saleCount = saleCount;
    }

    public long getSaleTotalCents() {
        return saleTotalCents;
    }

    public void setSaleTotalCents(long saleTotalCents) {
        this.saleTotalCents = saleTotalCents;
    }

    public int getRefundCount() {
        return refundCount;
    }

    public void setRefundCount(int refundCount) {
        this.refundCount = refundCount;
    }

    public long getRefundTotalCents() {
        return refundTotalCents;
    }

    public void setRefundTotalCents(long refundTotalCents) {
        this.refundTotalCents = refundTotalCents;
    }

    public long getNetTotalCents() {
        return netTotalCents;
    }

    public void setNetTotalCents(long netTotalCents) {
        this.netTotalCents = netTotalCents;
    }

    public long getTaxCents() {
        return taxCents;
    }

    public void setTaxCents(long taxCents) {
        this.taxCents = taxCents;
    }
}
