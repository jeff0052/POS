-- V071: 动态二维码令牌表
-- Journey: J01 顾客单点堂食
-- 每次清台刷新 token（旧 token → EXPIRED），QR URL = /qr/{storeId}/{tableId}/{token}
CREATE TABLE qr_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    token VARCHAR(128) NOT NULL,
    token_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
      COMMENT 'ACTIVE | EXPIRED | REVOKED',
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_qr_token UNIQUE (token),
    CONSTRAINT fk_qr_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_qr_table FOREIGN KEY (table_id) REFERENCES store_tables(id),
    INDEX idx_qr_store_table (store_id, table_id, token_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
