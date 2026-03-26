CREATE TABLE promotion_rules (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    rule_type VARCHAR(64) NOT NULL,
    rule_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    priority INT NOT NULL DEFAULT 100,
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_promotion_rules_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_promotion_rules_store
        FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT uk_promotion_rules_code UNIQUE (rule_code)
);

CREATE TABLE promotion_rule_conditions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    condition_type VARCHAR(64) NOT NULL,
    threshold_amount_cents BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_promotion_rule_conditions_rule
        FOREIGN KEY (rule_id) REFERENCES promotion_rules(id)
            ON DELETE CASCADE
);

CREATE TABLE promotion_rule_rewards (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    reward_type VARCHAR(64) NOT NULL,
    discount_amount_cents BIGINT NULL,
    gift_sku_id BIGINT NULL,
    gift_quantity INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_promotion_rule_rewards_rule
        FOREIGN KEY (rule_id) REFERENCES promotion_rules(id)
            ON DELETE CASCADE,
    CONSTRAINT fk_promotion_rule_rewards_gift_sku
        FOREIGN KEY (gift_sku_id) REFERENCES skus(id)
);

CREATE TABLE promotion_hits (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    active_order_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    discount_amount_cents BIGINT NOT NULL DEFAULT 0,
    gift_snapshot_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_promotion_hits_active_order
        FOREIGN KEY (active_order_id) REFERENCES active_table_orders(id)
            ON DELETE CASCADE,
    CONSTRAINT fk_promotion_hits_rule
        FOREIGN KEY (rule_id) REFERENCES promotion_rules(id)
);
