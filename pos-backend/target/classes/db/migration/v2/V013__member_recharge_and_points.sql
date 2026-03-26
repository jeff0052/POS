CREATE TABLE member_recharge_orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recharge_no VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    amount_cents BIGINT NOT NULL DEFAULT 0,
    bonus_amount_cents BIGINT NOT NULL DEFAULT 0,
    final_status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    operator_name VARCHAR(128) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_member_recharge_orders_no UNIQUE (recharge_no),
    CONSTRAINT fk_member_recharge_orders_member FOREIGN KEY (member_id) REFERENCES members(id)
);

CREATE TABLE member_points_ledger (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ledger_no VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    points_delta BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_ref VARCHAR(128) NULL,
    operator_name VARCHAR(128) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_member_points_ledger_no UNIQUE (ledger_no),
    CONSTRAINT fk_member_points_ledger_member FOREIGN KEY (member_id) REFERENCES members(id)
);
