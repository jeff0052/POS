-- V070: table_sessions 加并台支持
-- Journey: J11 并台
-- 被并桌的 session 指向主桌 session，NULL = 未并台
ALTER TABLE table_sessions
  ADD COLUMN merged_into_session_id BIGINT NULL AFTER buffet_status,
  ADD INDEX idx_ts_merged (merged_into_session_id);
