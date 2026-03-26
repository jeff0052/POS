CREATE TABLE merchants (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_code VARCHAR(64) NOT NULL,
    merchant_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Singapore',
    currency_code VARCHAR(16) NOT NULL DEFAULT 'SGD',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_merchants_code UNIQUE (merchant_code)
);

CREATE TABLE merchant_configs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    config_key VARCHAR(128) NOT NULL,
    config_value JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_merchant_configs_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_merchant_configs_key UNIQUE (merchant_id, config_key)
);
