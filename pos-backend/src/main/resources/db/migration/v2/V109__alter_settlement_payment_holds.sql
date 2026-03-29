-- V109: settlement_payment_holds 补齐 stacking 字段
-- 保留 V076 的 hold_no, hold_ref（向后兼容）
-- hold_amount 重命名为 hold_amount_cents，加 canonical 字段
ALTER TABLE settlement_payment_holds
  ADD COLUMN table_session_id BIGINT NULL AFTER store_id,
  ADD COLUMN step_order INT NOT NULL DEFAULT 0 AFTER table_session_id,
  CHANGE COLUMN hold_amount hold_amount_cents BIGINT NOT NULL,
  ADD COLUMN points_held BIGINT NULL AFTER hold_amount_cents,
  ADD COLUMN coupon_id BIGINT NULL AFTER points_held,
  ADD COLUMN payment_attempt_id BIGINT NULL AFTER release_reason,
  MODIFY COLUMN member_id BIGINT NULL,
  ADD INDEX idx_sph_session (table_session_id, hold_status);
