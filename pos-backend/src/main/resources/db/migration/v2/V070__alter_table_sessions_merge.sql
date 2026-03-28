-- G01 并台: session 加 merge 指针 + 人数
ALTER TABLE table_sessions
  ADD COLUMN merged_into_session_id BIGINT NULL
    COMMENT '被并入的主桌 session id' AFTER session_status,
  ADD COLUMN guest_count INT NOT NULL DEFAULT 1
    COMMENT '本桌开台人数' AFTER merged_into_session_id,
  ADD INDEX idx_ts_merged (merged_into_session_id);
