-- G08 巡店记录: 巡店 + 检查项
CREATE TABLE inspection_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    inspector_user_id BIGINT NOT NULL,
    inspection_date DATE NOT NULL,
    check_in_at TIMESTAMP NULL,
    check_out_at TIMESTAMP NULL,
    check_in_lat DECIMAL(10,7) NULL,
    check_in_lng DECIMAL(10,7) NULL,
    overall_score DECIMAL(3,1) NULL COMMENT '总分 0-10',
    hygiene_score DECIMAL(3,1) NULL,
    service_score DECIMAL(3,1) NULL,
    food_quality_score DECIMAL(3,1) NULL,
    compliance_score DECIMAL(3,1) NULL,
    inspection_status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS'
      COMMENT 'IN_PROGRESS|COMPLETED|SUBMITTED',
    summary TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ir_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_ir_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    INDEX idx_ir_date (store_id, inspection_date),
    INDEX idx_ir_inspector (inspector_user_id, inspection_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE inspection_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inspection_id BIGINT NOT NULL,
    category VARCHAR(64) NOT NULL COMMENT 'HYGIENE|SERVICE|FOOD|COMPLIANCE|OTHER',
    item_description VARCHAR(512) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'INFO' COMMENT 'INFO|WARNING|CRITICAL',
    is_passed BOOLEAN NULL COMMENT 'NULL=未检查',
    finding_notes TEXT NULL,
    photo_urls JSON NULL COMMENT '["url1","url2"]',
    requires_followup BOOLEAN NOT NULL DEFAULT FALSE,
    followup_deadline DATE NULL,
    followup_status VARCHAR(32) NULL COMMENT 'PENDING|IN_PROGRESS|RESOLVED',
    resolved_by BIGINT NULL,
    resolved_at TIMESTAMP NULL,
    resolution_notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ii_inspection FOREIGN KEY (inspection_id) REFERENCES inspection_records(id) ON DELETE CASCADE,
    INDEX idx_ii_followup (requires_followup, followup_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
