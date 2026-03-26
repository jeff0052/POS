-- Payment Intents: core payment lifecycle
CREATE TABLE IF NOT EXISTS payment_intents (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    intent_id VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    table_id BIGINT NULL,
    session_ref VARCHAR(128) NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'SGD',
    payment_method VARCHAR(32) NOT NULL,
    payment_scheme VARCHAR(64) NULL,
    provider_code VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    provider_transaction_id VARCHAR(255) NULL,
    provider_status VARCHAR(64) NULL,
    checkout_url VARCHAR(1024) NULL,
    callback_url VARCHAR(512) NULL,
    metadata_json JSON NULL,
    error_code VARCHAR(64) NULL,
    error_message VARCHAR(512) NULL,
    refunded_amount_cents BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    CONSTRAINT uk_intent_id UNIQUE (intent_id),
    INDEX idx_intent_store_table (store_id, table_id),
    INDEX idx_intent_provider_tx (provider_transaction_id),
    INDEX idx_intent_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Webhook Events: raw provider callbacks for audit
CREATE TABLE IF NOT EXISTS webhook_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(64) NOT NULL,
    provider_code VARCHAR(32) NOT NULL,
    event_type VARCHAR(64) NULL,
    intent_id VARCHAR(64) NULL,
    raw_payload LONGTEXT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    CONSTRAINT uk_event_id UNIQUE (event_id),
    INDEX idx_event_intent (intent_id),
    INDEX idx_event_provider (provider_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
