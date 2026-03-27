package com.developer.pos.v2.image.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity(name = "V2ImageAssetEntity")
@Table(name = "image_assets")
public class ImageAssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_id", nullable = false, unique = true, length = 64)
    private String imageId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "s3_key", nullable = false, length = 512)
    private String s3Key;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected ImageAssetEntity() {
    }

    public ImageAssetEntity(
            String imageId,
            Long merchantId,
            String s3Key,
            String originalName,
            String contentType,
            long fileSizeBytes
    ) {
        this.imageId = imageId;
        this.merchantId = merchantId;
        this.s3Key = s3Key;
        this.originalName = originalName;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markDeleted() {
        this.status = "DELETED";
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getImageId() {
        return imageId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public String getS3Key() {
        return s3Key;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
