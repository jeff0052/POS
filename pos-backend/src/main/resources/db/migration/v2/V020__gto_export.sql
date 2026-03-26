CREATE TABLE gto_export_batches (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    export_date DATE NOT NULL,
    batch_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    total_sales_cents BIGINT NOT NULL DEFAULT 0,
    total_tax_cents BIGINT NOT NULL DEFAULT 0,
    total_transaction_count INT NOT NULL DEFAULT 0,
    file_content_json JSON NULL,
    error_message TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    CONSTRAINT uk_gto_batch_store_date UNIQUE (store_id, export_date)
);

CREATE TABLE gto_export_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    payment_method VARCHAR(64) NOT NULL,
    payment_scheme VARCHAR(64) NULL,
    sale_count INT NOT NULL DEFAULT 0,
    sale_total_cents BIGINT NOT NULL DEFAULT 0,
    refund_count INT NOT NULL DEFAULT 0,
    refund_total_cents BIGINT NOT NULL DEFAULT 0,
    net_total_cents BIGINT NOT NULL DEFAULT 0,
    tax_cents BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_gto_item_batch FOREIGN KEY (batch_id) REFERENCES gto_export_batches(id)
);
