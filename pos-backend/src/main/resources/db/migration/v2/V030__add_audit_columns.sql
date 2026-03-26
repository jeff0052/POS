-- Add audit columns to active_table_orders
ALTER TABLE active_table_orders
    ADD COLUMN actor_type     VARCHAR(32)  DEFAULT 'HUMAN',
    ADD COLUMN actor_id       VARCHAR(64),
    ADD COLUMN decision_source VARCHAR(32) DEFAULT 'MANUAL',
    ADD COLUMN change_reason  TEXT;

-- Add audit columns to submitted_orders
ALTER TABLE submitted_orders
    ADD COLUMN actor_type     VARCHAR(32)  DEFAULT 'HUMAN',
    ADD COLUMN actor_id       VARCHAR(64),
    ADD COLUMN decision_source VARCHAR(32) DEFAULT 'MANUAL',
    ADD COLUMN change_reason  TEXT;

-- Add audit columns to promotion_rules
ALTER TABLE promotion_rules
    ADD COLUMN actor_type     VARCHAR(32)  DEFAULT 'HUMAN',
    ADD COLUMN actor_id       VARCHAR(64),
    ADD COLUMN decision_source VARCHAR(32) DEFAULT 'MANUAL',
    ADD COLUMN change_reason  TEXT;

-- Add audit columns to members
ALTER TABLE members
    ADD COLUMN actor_type     VARCHAR(32)  DEFAULT 'HUMAN',
    ADD COLUMN actor_id       VARCHAR(64),
    ADD COLUMN decision_source VARCHAR(32) DEFAULT 'MANUAL',
    ADD COLUMN change_reason  TEXT;

-- Add audit columns to settlement_records
ALTER TABLE settlement_records
    ADD COLUMN actor_type     VARCHAR(32)  DEFAULT 'HUMAN',
    ADD COLUMN actor_id       VARCHAR(64),
    ADD COLUMN decision_source VARCHAR(32) DEFAULT 'MANUAL',
    ADD COLUMN change_reason  TEXT;

-- Add audit columns to skus
ALTER TABLE skus
    ADD COLUMN actor_type     VARCHAR(32)  DEFAULT 'HUMAN',
    ADD COLUMN actor_id       VARCHAR(64),
    ADD COLUMN decision_source VARCHAR(32) DEFAULT 'MANUAL',
    ADD COLUMN change_reason  TEXT;

-- Add audit columns to store_sku_availability
ALTER TABLE store_sku_availability
    ADD COLUMN actor_type     VARCHAR(32)  DEFAULT 'HUMAN',
    ADD COLUMN actor_id       VARCHAR(64),
    ADD COLUMN decision_source VARCHAR(32) DEFAULT 'MANUAL',
    ADD COLUMN change_reason  TEXT;
