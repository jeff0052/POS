-- Phase1 自助餐: session 加 dining_mode + buffet 计时字段
ALTER TABLE table_sessions
  ADD COLUMN dining_mode VARCHAR(32) NOT NULL DEFAULT 'A_LA_CARTE'
    COMMENT 'A_LA_CARTE|BUFFET|DELIVERY' AFTER guest_count,
  ADD COLUMN child_count INT NOT NULL DEFAULT 0 AFTER dining_mode,
  ADD COLUMN buffet_package_id BIGINT NULL AFTER child_count,
  ADD COLUMN buffet_started_at TIMESTAMP NULL AFTER buffet_package_id,
  ADD COLUMN buffet_ends_at TIMESTAMP NULL AFTER buffet_started_at,
  ADD COLUMN buffet_status VARCHAR(32) NULL
    COMMENT 'NULL(非自助)|ACTIVE|WARNING|OVERTIME|ENDED' AFTER buffet_ends_at,
  ADD COLUMN buffet_overtime_minutes INT NOT NULL DEFAULT 0 AFTER buffet_status,
  ADD INDEX idx_ts_dining (store_id, dining_mode),
  ADD INDEX idx_ts_buffet (store_id, buffet_status);
