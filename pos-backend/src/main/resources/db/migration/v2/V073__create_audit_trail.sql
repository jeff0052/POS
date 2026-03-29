-- V073: 审计追踪表
-- Journey: J05 收银员, J07 店长, J09 老板, J10 财务
-- target_id 用 VARCHAR(64) 兼容 VARCHAR 业务 ID（如 active_order_id）
CREATE TABLE audit_trail (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    trail_no VARCHAR(64) NOT NULL,
    store_id BIGINT NOT NULL,
    actor_type VARCHAR(32) NOT NULL
      COMMENT 'HUMAN | AI | SYSTEM',
    actor_id BIGINT NULL,
    actor_name VARCHAR(128) NULL,
    action VARCHAR(64) NOT NULL
      COMMENT 'REFUND | PRICE_CHANGE | VOID_ORDER | DISCOUNT_OVERRIDE | TABLE_MERGE | ...',
    target_type VARCHAR(64) NOT NULL
      COMMENT 'settlement_records | skus | submitted_orders | ...',
    target_id VARCHAR(64) NOT NULL
      COMMENT 'VARCHAR for compatibility with string business IDs',
    before_snapshot JSON NULL,
    after_snapshot JSON NULL,
    risk_level VARCHAR(16) NOT NULL DEFAULT 'LOW'
      COMMENT 'LOW | MEDIUM | HIGH | CRITICAL',
    requires_approval BOOLEAN DEFAULT FALSE,
    approval_status VARCHAR(32) NULL
      COMMENT 'PENDING | APPROVED | REJECTED',
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    approval_note VARCHAR(512) NULL,
    ip_address VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_at_trail_no UNIQUE (trail_no),
    CONSTRAINT fk_at_store FOREIGN KEY (store_id) REFERENCES stores(id),
    INDEX idx_at_store_action (store_id, action),
    INDEX idx_at_approval (store_id, approval_status),
    INDEX idx_at_target (target_type, target_id),
    INDEX idx_at_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
