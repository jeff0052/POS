-- V088: SOP 配方批量导入表
-- Journey: J08 库存
CREATE TABLE sop_import_batches (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_asset_id VARCHAR(64) NULL,
    total_rows INT NOT NULL DEFAULT 0,
    success_rows INT NOT NULL DEFAULT 0,
    error_rows INT NOT NULL DEFAULT 0,
    batch_status VARCHAR(32) NOT NULL DEFAULT 'VALIDATING'
      COMMENT 'VALIDATING -> VALIDATED -> IMPORTING -> COMPLETED | FAILED',
    error_details JSON NULL
      COMMENT '[{"row": 5, "error": "SKU not found: ABC"}]',
    imported_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sib_store FOREIGN KEY (store_id) REFERENCES stores(id),
    INDEX idx_sib_store (store_id, batch_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
