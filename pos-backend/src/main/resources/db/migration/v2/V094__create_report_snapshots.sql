-- V094: 报表快照 + AI 摘要
-- Journey: J09 老板视角
CREATE TABLE report_snapshots (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    report_type VARCHAR(32) NOT NULL
      COMMENT 'DAILY_SUMMARY | WEEKLY_SUMMARY | MONTHLY_SUMMARY',
    report_date DATE NOT NULL,
    metrics_json JSON NOT NULL
      COMMENT '{ revenue_cents, order_count, avg_ticket_cents, table_turnover_rate, ... }',
    ai_summary TEXT NULL,
    ai_highlights JSON NULL
      COMMENT '["revenue hit weekly high", "buffet 30% of orders"]',
    ai_warnings JSON NULL
      COMMENT '["beef stock only 2 days left"]',
    ai_suggestions JSON NULL
      COMMENT '["launch lunch buffet package"]',
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_rs UNIQUE (store_id, report_type, report_date),
    CONSTRAINT fk_rs_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_rs_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    INDEX idx_rs_merchant (merchant_id, report_type, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
