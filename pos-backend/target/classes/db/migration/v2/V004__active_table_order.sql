CREATE TABLE active_table_orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    active_order_id VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    order_source VARCHAR(32) NOT NULL,
    dining_type VARCHAR(32) NOT NULL DEFAULT 'DINE_IN',
    status VARCHAR(32) NOT NULL,
    member_id BIGINT NULL,
    cashier_id BIGINT NULL,
    current_shift_id BIGINT NULL,
    original_amount_cents BIGINT NOT NULL DEFAULT 0,
    member_discount_cents BIGINT NOT NULL DEFAULT 0,
    promotion_discount_cents BIGINT NOT NULL DEFAULT 0,
    payable_amount_cents BIGINT NOT NULL DEFAULT 0,
    pricing_snapshot_json JSON NULL,
    promotion_snapshot_json JSON NULL,
    settlement_snapshot_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_active_table_orders_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_active_table_orders_store
        FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_active_table_orders_table
        FOREIGN KEY (table_id) REFERENCES store_tables(id),
    CONSTRAINT uk_active_table_orders_id UNIQUE (active_order_id),
    CONSTRAINT uk_active_table_orders_no UNIQUE (order_no),
    CONSTRAINT uk_active_table_orders_table UNIQUE (store_id, table_id)
);

CREATE TABLE active_table_order_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    active_order_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    sku_name_snapshot VARCHAR(255) NOT NULL,
    sku_code_snapshot VARCHAR(64) NOT NULL,
    unit_price_snapshot_cents BIGINT NOT NULL,
    member_price_snapshot_cents BIGINT NULL,
    quantity INT NOT NULL,
    item_remark VARCHAR(255) NULL,
    option_snapshot_json JSON NULL,
    line_total_cents BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_active_table_order_items_order
        FOREIGN KEY (active_order_id) REFERENCES active_table_orders(id)
            ON DELETE CASCADE,
    CONSTRAINT fk_active_table_order_items_sku
        FOREIGN KEY (sku_id) REFERENCES skus(id)
);

CREATE TABLE order_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    active_order_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(64) NULL,
    decision_source VARCHAR(32) NOT NULL DEFAULT 'HUMAN',
    event_payload_json JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_events_active_order
        FOREIGN KEY (active_order_id) REFERENCES active_table_orders(id)
            ON DELETE CASCADE
);
