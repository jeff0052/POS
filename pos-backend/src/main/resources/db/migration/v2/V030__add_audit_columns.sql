-- Add audit columns to core V2 tables for AI-ready ActionContext tracking

ALTER TABLE active_table_orders
    ADD COLUMN actor_type     VARCHAR(32)  DEFAULT 'HUMAN',
    ADD COLUMN actor_id       VARCHAR(64),
    ADD COLUMN decision_source VARCHAR(32) DEFAULT 'MANUAL',
    ADD COLUMN change_reason  TEXT;

ALTER TABLE submitted_orders
    ADD COLUMN actor_type     VARCHAR(32)  DEFAULT 'HUMAN',
    ADD COLUMN actor_id       VARCHAR(64),
    ADD COLUMN decision_source VARCHAR(32) DEFAULT 'MANUAL',
    ADD COLUMN change_reason  TEXT;

ALTER TABLE promotion_rules
    ADD COLUMN actor_type     VARCHAR(32)  DEFAULT 'HUMAN',
    ADD COLUMN actor_id       VARCHAR(64),
    ADD COLUMN decision_source VARCHAR(32) DEFAULT 'MANUAL',
    ADD COLUMN change_reason  TEXT;

ALTER TABLE members
    ADD COLUMN actor_type     VARCHAR(32)  DEFAULT 'HUMAN',
    ADD COLUMN actor_id       VARCHAR(64),
    ADD COLUMN decision_source VARCHAR(32) DEFAULT 'MANUAL',
    ADD COLUMN change_reason  TEXT;

ALTER TABLE settlement_records
    ADD COLUMN actor_type     VARCHAR(32)  DEFAULT 'HUMAN',
    ADD COLUMN actor_id       VARCHAR(64),
    ADD COLUMN decision_source VARCHAR(32) DEFAULT 'MANUAL',
    ADD COLUMN change_reason  TEXT;

ALTER TABLE skus
    ADD COLUMN actor_type     VARCHAR(32)  DEFAULT 'HUMAN',
    ADD COLUMN actor_id       VARCHAR(64),
    ADD COLUMN decision_source VARCHAR(32) DEFAULT 'MANUAL',
    ADD COLUMN change_reason  TEXT;

ALTER TABLE store_sku_availability
    ADD COLUMN actor_type     VARCHAR(32)  DEFAULT 'HUMAN',
    ADD COLUMN actor_id       VARCHAR(64),
    ADD COLUMN decision_source VARCHAR(32) DEFAULT 'MANUAL',
    ADD COLUMN change_reason  TEXT;
