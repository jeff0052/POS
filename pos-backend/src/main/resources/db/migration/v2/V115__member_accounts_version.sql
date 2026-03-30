ALTER TABLE member_accounts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE recharge_campaigns ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE points_batches ADD UNIQUE KEY uk_points_batches_member_source (member_id, source_type, source_ref);
