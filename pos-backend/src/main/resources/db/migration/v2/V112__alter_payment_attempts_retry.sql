-- V112: payment_attempts 加 retry/replace 链字段
-- settlement_record_id = null → 旧路径；非 null → stacking 路径
ALTER TABLE payment_attempts
  ADD COLUMN settlement_record_id BIGINT NULL AFTER attempt_status,
  ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER settlement_record_id,
  ADD COLUMN max_retries INT NOT NULL DEFAULT 3 AFTER retry_count,
  ADD COLUMN failure_reason VARCHAR(512) NULL AFTER max_retries,
  ADD COLUMN failure_code VARCHAR(64) NULL AFTER failure_reason,
  ADD COLUMN replaced_by_attempt_id BIGINT NULL AFTER failure_code,
  ADD COLUMN parent_attempt_id BIGINT NULL AFTER replaced_by_attempt_id,
  ADD INDEX idx_pa_parent (parent_attempt_id),
  ADD INDEX idx_pa_settlement (settlement_record_id);
