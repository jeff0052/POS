CREATE TABLE ai_proposal (
    id VARCHAR(64) PRIMARY KEY,
    advisor_role VARCHAR(32) NOT NULL,
    proposal_type VARCHAR(64) NOT NULL,
    target_domain VARCHAR(32) NOT NULL,
    target_tool VARCHAR(64) NOT NULL,
    params_json JSON,
    reason TEXT,
    risk_level VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6),
    reviewed_by VARCHAR(64),
    reviewed_at DATETIME(6),
    executed_at DATETIME(6),
    KEY idx_proposal_status (status, created_at),
    KEY idx_proposal_role (advisor_role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
