-- AI Operator recommendations table
CREATE TABLE ai_recommendations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recommendation_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    advisor_role VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    detail_json JSON NULL,
    risk_level VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    proposed_action VARCHAR(64) NULL,
    proposed_params_json JSON NULL,
    approved_by VARCHAR(64) NULL,
    approved_at TIMESTAMP NULL,
    rejected_reason TEXT NULL,
    executed_at TIMESTAMP NULL,
    execution_result_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    INDEX idx_rec_store_status (store_id, status),
    INDEX idx_rec_store_role (store_id, advisor_role)
);

-- AI Operator scheduled tasks
CREATE TABLE ai_scheduled_checks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    advisor_role VARCHAR(32) NOT NULL,
    check_type VARCHAR(64) NOT NULL,
    cron_expression VARCHAR(64) NOT NULL DEFAULT '0 8 * * *',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_at TIMESTAMP NULL,
    CONSTRAINT uk_ai_check UNIQUE (store_id, advisor_role, check_type)
);
