CREATE TABLE cashier_shifts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shift_id VARCHAR(64) NOT NULL,
    store_id BIGINT NOT NULL,
    cashier_id BIGINT NOT NULL,
    cashier_name VARCHAR(128) NOT NULL,
    shift_status VARCHAR(32) NOT NULL,
    opening_float_cents BIGINT NOT NULL,
    closing_cash_cents BIGINT NULL,
    closing_note VARCHAR(255) NULL,
    opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_cashier_shifts_shift_id UNIQUE (shift_id)
);
