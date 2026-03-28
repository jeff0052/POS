-- G12 第三方对接日志
CREATE TABLE external_integration_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NULL COMMENT '可为空(平台级调用)',
    merchant_id BIGINT NOT NULL,
    integration_type VARCHAR(64) NOT NULL
      COMMENT 'GRAB|FOODPANDA|GOOGLE|MALL_CRM|GTO|PAYMENT|OCR',
    direction VARCHAR(16) NOT NULL COMMENT 'OUTBOUND|INBOUND',
    http_method VARCHAR(16) NULL,
    request_url VARCHAR(1024) NULL,
    request_headers JSON NULL,
    request_body TEXT NULL,
    response_status INT NULL,
    response_body TEXT NULL,
    latency_ms INT NULL,
    result_status VARCHAR(32) NOT NULL COMMENT 'SUCCESS|FAILED|TIMEOUT|ERROR',
    error_message VARCHAR(512) NULL,
    business_type VARCHAR(64) NULL COMMENT 'ORDER_SYNC|MENU_PUSH|REVIEW_FETCH|...',
    business_ref VARCHAR(128) NULL COMMENT '关联的业务 ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_eil_type (merchant_id, integration_type, created_at),
    INDEX idx_eil_result (merchant_id, result_status, created_at),
    INDEX idx_eil_biz (business_type, business_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
