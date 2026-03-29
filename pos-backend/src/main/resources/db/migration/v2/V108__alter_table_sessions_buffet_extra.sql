-- V108: Add child_count and buffet_overtime_minutes to table_sessions
-- V067 added dining_mode, guest_count, buffet_package_id, buffet_started_at, buffet_ends_at, buffet_status
-- but missed child_count and buffet_overtime_minutes from the V075 archive spec
ALTER TABLE table_sessions
  ADD COLUMN child_count INT NOT NULL DEFAULT 0 AFTER guest_count,
  ADD COLUMN buffet_overtime_minutes INT NOT NULL DEFAULT 0 AFTER buffet_status;
