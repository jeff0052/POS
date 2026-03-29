-- V113: settlement_records 加 stacking_session_id（幂等 scope）
-- stacking 流程的 active_order_id = "STACK-<uuid>" 绕过唯一约束
-- stacking_session_id = table_sessions.id，用于幂等检查
ALTER TABLE settlement_records
  ADD COLUMN stacking_session_id BIGINT NULL AFTER active_order_id,
  ADD INDEX idx_sr_stacking_session (stacking_session_id, final_status);
