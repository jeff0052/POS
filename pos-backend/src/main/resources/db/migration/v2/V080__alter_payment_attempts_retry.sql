-- G04 支付失败重试: 加重试/替换链路字段
-- 保留现有状态: PENDING_CUSTOMER|SUCCEEDED|SETTLED|FAILED|EXPIRED
-- 新增使用: REPLACED (换方式时标记旧 attempt)
ALTER TABLE payment_attempts
  ADD COLUMN retry_count INT NOT NULL DEFAULT 0 AFTER attempt_status,
  ADD COLUMN max_retries INT NOT NULL DEFAULT 3 AFTER retry_count,
  ADD COLUMN failure_reason VARCHAR(512) NULL AFTER max_retries,
  ADD COLUMN failure_code VARCHAR(64) NULL COMMENT '支付方返回的错误码' AFTER failure_reason,
  ADD COLUMN replaced_by_attempt_id BIGINT NULL
    COMMENT '换方式后新 attempt 的 id' AFTER failure_code,
  ADD COLUMN parent_attempt_id BIGINT NULL
    COMMENT '原 attempt 的 id(重试链)' AFTER replaced_by_attempt_id,
  ADD INDEX idx_pa_parent (parent_attempt_id);
