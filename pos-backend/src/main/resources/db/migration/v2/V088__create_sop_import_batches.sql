-- G07 SOP 批量导入: 导入批次追踪表
CREATE TABLE sop_import_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    batch_no VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(512) NOT NULL,
    total_rows INT NOT NULL DEFAULT 0,
    success_rows INT NOT NULL DEFAULT 0,
    error_rows INT NOT NULL DEFAULT 0,
    error_details JSON NULL COMMENT '[{"row":3,"field":"sku_code","error":"not found"}]',
    import_status VARCHAR(32) NOT NULL DEFAULT 'UPLOADED'
      COMMENT 'UPLOADED|VALIDATING|VALIDATED|IMPORTING|COMPLETED|FAILED',
    imported_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    CONSTRAINT fk_sib_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_sib UNIQUE (store_id, batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
