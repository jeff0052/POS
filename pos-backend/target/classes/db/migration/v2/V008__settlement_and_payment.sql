CREATE TABLE settlement_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    settlement_no VARCHAR(64) NOT NULL,
    active_order_id VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    cashier_id BIGINT NOT NULL,
    payment_method VARCHAR(64) NOT NULL,
    final_status VARCHAR(32) NOT NULL,
    payable_amount_cents BIGINT NOT NULL,
    collected_amount_cents BIGINT NOT NULL,
    pricing_snapshot_json JSON NULL,
    settlement_snapshot_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_settlement_records_no UNIQUE (settlement_no)
);
