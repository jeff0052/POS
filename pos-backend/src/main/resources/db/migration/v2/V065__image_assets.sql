CREATE TABLE image_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_id VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    s3_key VARCHAR(512) NOT NULL,
    original_name VARCHAR(255),
    content_type VARCHAR(64) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_image_id UNIQUE (image_id),
    INDEX idx_image_merchant (merchant_id),
    INDEX idx_image_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE products ADD COLUMN image_id VARCHAR(64) NULL AFTER product_name;
ALTER TABLE skus ADD COLUMN image_id VARCHAR(64) NULL AFTER sku_name;
