CREATE TABLE refund_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    refund_no VARCHAR(64) NOT NULL UNIQUE,
    settlement_id BIGINT NOT NULL,
    settlement_no VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    refund_amount_cents BIGINT NOT NULL,
    refund_type VARCHAR(32) NOT NULL DEFAULT 'FULL',
    refund_reason TEXT,
    refund_status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED',
    payment_method VARCHAR(32) NOT NULL,
    operated_by BIGINT NULL,
    approved_by BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refund_settlement FOREIGN KEY (settlement_id) REFERENCES settlement_records(id)
);

CREATE INDEX idx_refund_store ON refund_records(store_id, created_at);
CREATE INDEX idx_refund_settlement ON refund_records(settlement_id);

-- Update settlement_records to track refund state
ALTER TABLE settlement_records ADD COLUMN refunded_amount_cents BIGINT NOT NULL DEFAULT 0 AFTER collected_amount_cents;
ALTER TABLE settlement_records ADD COLUMN refund_status VARCHAR(32) NULL AFTER final_status;
