CREATE TABLE stores (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    store_code VARCHAR(64) NOT NULL,
    store_name VARCHAR(255) NOT NULL,
    store_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    phone VARCHAR(64) NULL,
    address_line VARCHAR(255) NULL,
    city VARCHAR(128) NULL,
    country_code VARCHAR(16) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_stores_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_stores_code UNIQUE (store_code),
    CONSTRAINT uk_stores_merchant_store UNIQUE (merchant_id, store_code)
);

CREATE TABLE store_tables (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    table_code VARCHAR(64) NOT NULL,
    table_name VARCHAR(128) NOT NULL,
    table_capacity INT NOT NULL DEFAULT 4,
    table_status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_store_tables_store
        FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_store_tables_code UNIQUE (store_id, table_code)
);

CREATE TABLE store_terminals (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    terminal_code VARCHAR(64) NOT NULL,
    terminal_name VARCHAR(128) NOT NULL,
    terminal_type VARCHAR(32) NOT NULL DEFAULT 'POS',
    device_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_store_terminals_store
        FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_store_terminals_code UNIQUE (store_id, terminal_code)
);
