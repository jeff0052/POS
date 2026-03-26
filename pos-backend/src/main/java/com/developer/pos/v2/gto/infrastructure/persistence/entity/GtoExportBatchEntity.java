package com.developer.pos.v2.gto.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gto_export_batches")
public class GtoExportBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false, unique = true)
    private String batchId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "export_date", nullable = false)
    private LocalDate exportDate;

    @Column(name = "batch_status", nullable = false)
    private String batchStatus;

    @Column(name = "total_sales_cents", nullable = false)
    private long totalSalesCents;

    @Column(name = "total_tax_cents", nullable = false)
    private long totalTaxCents;

    @Column(name = "total_transaction_count", nullable = false)
    private int totalTransactionCount;

    @Column(name = "file_content_json", columnDefinition = "JSON")
    private String fileContentJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<GtoExportItemEntity> items = new ArrayList<>();

    public GtoExportBatchEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
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

    public LocalDate getExportDate() {
        return exportDate;
    }

    public void setExportDate(LocalDate exportDate) {
        this.exportDate = exportDate;
    }

    public String getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(String batchStatus) {
        this.batchStatus = batchStatus;
    }

    public long getTotalSalesCents() {
        return totalSalesCents;
    }

    public void setTotalSalesCents(long totalSalesCents) {
        this.totalSalesCents = totalSalesCents;
    }

    public long getTotalTaxCents() {
        return totalTaxCents;
    }

    public void setTotalTaxCents(long totalTaxCents) {
        this.totalTaxCents = totalTaxCents;
    }

    public int getTotalTransactionCount() {
        return totalTransactionCount;
    }

    public void setTotalTransactionCount(int totalTransactionCount) {
        this.totalTransactionCount = totalTransactionCount;
    }

    public String getFileContentJson() {
        return fileContentJson;
    }

    public void setFileContentJson(String fileContentJson) {
        this.fileContentJson = fileContentJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<GtoExportItemEntity> getItems() {
        return items;
    }

    public void setItems(List<GtoExportItemEntity> items) {
        this.items = items;
    }
}
