CREATE TABLE cashier_shifts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shift_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    cashier_staff_id VARCHAR(64) NOT NULL,
    cashier_name VARCHAR(128) NOT NULL,
    shift_status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL,
    opening_cash_cents BIGINT NOT NULL DEFAULT 0,
    closing_cash_cents BIGINT NULL,
    expected_cash_cents BIGINT NULL,
    cash_difference_cents BIGINT NULL,
    total_sales_cents BIGINT NOT NULL DEFAULT 0,
    total_refunds_cents BIGINT NOT NULL DEFAULT 0,
    total_transaction_count INT NOT NULL DEFAULT 0,
    notes TEXT NULL
);

CREATE TABLE cashier_shift_settlements (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shift_id BIGINT NOT NULL,
    settlement_no VARCHAR(64) NOT NULL,
    payment_method VARCHAR(64) NOT NULL,
    amount_cents BIGINT NOT NULL,
    settled_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_shift_settlement FOREIGN KEY (shift_id) REFERENCES cashier_shifts(id)
);
