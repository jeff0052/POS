package com.developer.pos.v2.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "V2SopImportBatchEntity")
@Table(name = "sop_import_batches")
public class SopImportBatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_asset_id", length = 64)
    private String fileAssetId;

    @Column(name = "total_rows", nullable = false)
    private int totalRows = 0;

    @Column(name = "success_rows", nullable = false)
    private int successRows = 0;

    @Column(name = "error_rows", nullable = false)
    private int errorRows = 0;

    @Column(name = "batch_status", nullable = false, length = 32)
    private String batchStatus = "VALIDATING";

    @Column(name = "error_details", columnDefinition = "JSON")
    private String errorDetails;

    @Column(name = "imported_by")
    private Long importedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected SopImportBatchEntity() {}

    public SopImportBatchEntity(Long storeId, String fileName, String fileAssetId, Long importedBy) {
        this.storeId = storeId;
        this.fileName = fileName;
        this.fileAssetId = fileAssetId;
        this.importedBy = importedBy;
        this.batchStatus = "VALIDATING";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markValidated(int totalRows, int errorRows, String errorDetailsJson) {
        if (!"VALIDATING".equals(this.batchStatus)) {
            throw new IllegalStateException("Batch " + id + " is not validating, current: " + batchStatus);
        }
        this.totalRows = totalRows;
        this.errorRows = errorRows;
        this.errorDetails = errorDetailsJson;
        this.batchStatus = "VALIDATED";
        this.updatedAt = LocalDateTime.now();
    }

    public void startImport() {
        if (!"VALIDATED".equals(this.batchStatus)) {
            throw new IllegalStateException("Batch " + id + " is not validated, current: " + batchStatus);
        }
        this.batchStatus = "IMPORTING";
        this.updatedAt = LocalDateTime.now();
    }

    public void completeImport(int successRows) {
        if (!"IMPORTING".equals(this.batchStatus)) {
            throw new IllegalStateException("Batch " + id + " is not importing, current: " + batchStatus);
        }
        this.successRows = successRows;
        this.batchStatus = "COMPLETED";
        this.updatedAt = LocalDateTime.now();
    }

    public void fail(String errorDetailsJson) {
        this.errorDetails = errorDetailsJson;
        this.batchStatus = "FAILED";
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public String getFileName() { return fileName; }
    public String getFileAssetId() { return fileAssetId; }
    public int getTotalRows() { return totalRows; }
    public int getSuccessRows() { return successRows; }
    public int getErrorRows() { return errorRows; }
    public String getBatchStatus() { return batchStatus; }
    public String getErrorDetails() { return errorDetails; }
    public Long getImportedBy() { return importedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
