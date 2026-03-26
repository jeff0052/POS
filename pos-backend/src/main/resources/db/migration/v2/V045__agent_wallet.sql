-- Restaurant Agent identity
CREATE TABLE restaurant_agents (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    agent_name VARCHAR(255) NOT NULL,
    agent_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    cuisine_type VARCHAR(64) NULL,
    address VARCHAR(512) NULL,
    operating_hours VARCHAR(255) NULL,
    capabilities_json JSON NULL,
    agent_config_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_agent_store UNIQUE (store_id)
);

-- Agent wallet
CREATE TABLE agent_wallets (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    wallet_id VARCHAR(64) NOT NULL UNIQUE,
    agent_id VARCHAR(64) NOT NULL,
    balance_cents BIGINT NOT NULL DEFAULT 0,
    total_income_cents BIGINT NOT NULL DEFAULT 0,
    total_expense_cents BIGINT NOT NULL DEFAULT 0,
    currency_code VARCHAR(16) NOT NULL DEFAULT 'SGD',
    wallet_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_wallet_agent UNIQUE (agent_id)
);

-- Wallet transactions
CREATE TABLE wallet_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL UNIQUE,
    wallet_id VARCHAR(64) NOT NULL,
    transaction_type VARCHAR(32) NOT NULL,
    amount_cents BIGINT NOT NULL,
    balance_after_cents BIGINT NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_ref VARCHAR(128) NULL,
    counterparty VARCHAR(255) NULL,
    description VARCHAR(512) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_wallet_tx (wallet_id, created_at)
);

-- External interaction requests
CREATE TABLE agent_interactions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    interaction_id VARCHAR(64) NOT NULL UNIQUE,
    agent_id VARCHAR(64) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    interaction_type VARCHAR(64) NOT NULL,
    requester_agent_id VARCHAR(128) NULL,
    request_summary TEXT NOT NULL,
    request_detail_json JSON NULL,
    response_summary TEXT NULL,
    response_detail_json JSON NULL,
    risk_level VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    auto_handled BOOLEAN NOT NULL DEFAULT FALSE,
    handled_by VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,
    INDEX idx_agent_interaction (agent_id, status)
);
