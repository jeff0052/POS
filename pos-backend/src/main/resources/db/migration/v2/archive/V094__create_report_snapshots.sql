-- G11/G15 报表自动摘要 + 多店对比: 报表快照表
CREATE TABLE report_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    report_type VARCHAR(64) NOT NULL
      COMMENT 'DAILY_SUMMARY|WEEKLY_SUMMARY|MONTHLY_SUMMARY',
    report_date DATE NOT NULL,
    metrics_json JSON NOT NULL COMMENT '结构化指标数据',
    ai_summary TEXT NULL COMMENT 'AI 生成的自然语言摘要',
    ai_highlights JSON NULL COMMENT '["营收+15%","新增会员30"]',
    ai_warnings JSON NULL COMMENT '["库存低于安全线","差评增加"]',
    ai_suggestions JSON NULL COMMENT '["推出午市套餐","补货牛腩"]',
    ai_generated_at TIMESTAMP NULL,
    ai_model_version VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rs_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_rs UNIQUE (store_id, report_type, report_date),
    INDEX idx_rs_merchant (merchant_id, report_type, report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
