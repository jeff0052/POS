package com.developer.pos.v2.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity(name = "V2PurchaseInvoiceEntity")
@Table(name = "purchase_invoices",
       uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "invoice_no"}))
public class PurchaseInvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "invoice_no", nullable = false, length = 128)
    private String invoiceNo;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name", length = 255)
    private String supplierName;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "total_amount_cents")
    private Long totalAmountCents = 0L;

    /** PENDING | PROCESSING | CONFIRMED | CANCELLED */
    @Column(name = "invoice_status", length = 32, nullable = false)
    private String invoiceStatus = "PENDING";

    @Column(name = "scan_image_url", length = 512)
    private String scanImageUrl;

    /** PROCESSING | COMPLETED | FAILED | null (not yet scanned) */
    @Column(name = "ocr_status", length = 32)
    private String ocrStatus;

    @Column(name = "ocr_raw_result", columnDefinition = "JSON")
    private String ocrRawResult;

    @Column(name = "ocr_confidence", precision = 3, scale = 2)
    private BigDecimal ocrConfidence;

    @Column(name = "ocr_reviewed")
    private Boolean ocrReviewed = false;

    @Column(name = "ocr_reviewed_by")
    private Long ocrReviewedBy;

    @Column(name = "ocr_reviewed_at")
    private LocalDateTime ocrReviewedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected PurchaseInvoiceEntity() {}

    public PurchaseInvoiceEntity(Long storeId, String invoiceNo, Long supplierId,
                                  String supplierName, LocalDate invoiceDate) {
        this.storeId = storeId;
        this.invoiceNo = invoiceNo;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.invoiceDate = invoiceDate;
        this.invoiceStatus = "PENDING";
        this.totalAmountCents = 0L;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void startOcrScan(String imageUrl) {
        if ("PROCESSING".equals(this.ocrStatus) || "CONFIRMED".equals(this.invoiceStatus)) {
            throw new IllegalStateException("Invoice " + id + " is already being processed or confirmed");
        }
        this.scanImageUrl = imageUrl;
        this.ocrStatus = "PROCESSING";
        this.updatedAt = LocalDateTime.now();
    }

    public void completeOcrScan(BigDecimal confidence, String rawResultJson) {
        this.ocrStatus = "COMPLETED";
        this.ocrConfidence = confidence;
        this.ocrRawResult = rawResultJson;
        this.updatedAt = LocalDateTime.now();
    }

    public void failOcrScan(String errorMessage) {
        this.ocrStatus = "FAILED";
        this.ocrRawResult = "{\"error\":\"" + (errorMessage != null ? errorMessage.replace("\"", "'") : "unknown") + "\"}";
        this.updatedAt = LocalDateTime.now();
    }

    public void completeOcr(Long totalAmountCents, Long reviewedBy) {
        if (!"COMPLETED".equals(this.ocrStatus)) {
            throw new IllegalStateException("Invoice " + id + " OCR is not completed, current: " + ocrStatus);
        }
        this.invoiceStatus = "CONFIRMED";
        this.totalAmountCents = totalAmountCents;
        this.ocrReviewed = true;
        this.ocrReviewedBy = reviewedBy;
        this.ocrReviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void completeOcr(Long totalAmountCents) {
        completeOcr(totalAmountCents, null);
    }

    public Long getId() { return id; }
    public Long getStoreId() { return storeId; }
    public String getInvoiceNo() { return invoiceNo; }
    public Long getSupplierId() { return supplierId; }
    public String getSupplierName() { return supplierName; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public Long getTotalAmountCents() { return totalAmountCents; }
    public String getInvoiceStatus() { return invoiceStatus; }
    public String getOcrStatus() { return ocrStatus; }
    public String getScanImageUrl() { return scanImageUrl; }
    public String getOcrRawResult() { return ocrRawResult; }
    public BigDecimal getOcrConfidence() { return ocrConfidence; }
    public boolean isOcrReviewed() { return Boolean.TRUE.equals(ocrReviewed); }
    public Long getOcrReviewedBy() { return ocrReviewedBy; }
    public LocalDateTime getOcrReviewedAt() { return ocrReviewedAt; }
}
