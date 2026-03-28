-- V092: 外部对接日志表
-- Journey: J03 外卖, J10 财务, J12 预约
-- LONGTEXT for body: webhook payloads can be large (consistent with payment_attempts)
CREATE TABLE external_integration_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    platform VARCHAR(64) NOT NULL
      COMMENT 'GRAB | FOODPANDA | GOOGLE | WECHAT | ...',
    direction VARCHAR(16) NOT NULL
      COMMENT 'INBOUND | OUTBOUND',
    endpoint VARCHAR(512) NOT NULL,
    request_body LONGTEXT NULL,
    response_body LONGTEXT NULL,
    http_status INT NULL,
    result_status VARCHAR(32) NOT NULL
      COMMENT 'SUCCESS | FAILED | TIMEOUT',
    error_message VARCHAR(1024) NULL,
    correlation_id VARCHAR(128) NULL,
    duration_ms INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eil_store FOREIGN KEY (store_id) REFERENCES stores(id),
    INDEX idx_eil_store_platform (store_id, platform),
    INDEX idx_eil_correlation (correlation_id),
    INDEX idx_eil_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
