CREATE TABLE members (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    member_no VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    phone VARCHAR(64) NOT NULL,
    tier_code VARCHAR(64) NOT NULL DEFAULT 'STANDARD',
    member_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_members_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT uk_members_no UNIQUE (member_no),
    CONSTRAINT uk_members_phone UNIQUE (phone)
);

CREATE TABLE member_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    points_balance BIGINT NOT NULL DEFAULT 0,
    cash_balance_cents BIGINT NOT NULL DEFAULT 0,
    lifetime_spend_cents BIGINT NOT NULL DEFAULT 0,
    lifetime_recharge_cents BIGINT NOT NULL DEFAULT 0,
    last_activity_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_accounts_member
        FOREIGN KEY (member_id) REFERENCES members(id)
            ON DELETE CASCADE,
    CONSTRAINT uk_member_accounts_member UNIQUE (member_id)
);
