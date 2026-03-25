CREATE TABLE action_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tool_name VARCHAR(128) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(64),
    decision_source VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    recommendation_id VARCHAR(64),
    approval_status VARCHAR(32),
    risk_level VARCHAR(16),
    params_json JSON,
    result_json JSON,
    change_reason TEXT,
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_action_log_actor (actor_type, created_at),
    KEY idx_action_log_tool (tool_name, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
