-- V095: 厨房工作站 fallback + 健康监控
-- Journey: J06 厨房
-- fallback_mode: AUTO | MANUAL | DISABLED
-- kds_health_status: ONLINE | OFFLINE
-- last_heartbeat_at > 90s -> OFFLINE -> auto fallback to printer
ALTER TABLE kitchen_stations
  ADD COLUMN fallback_printer_ip VARCHAR(64) NULL AFTER printer_ip,
  ADD COLUMN fallback_mode VARCHAR(32) DEFAULT 'AUTO' AFTER fallback_printer_ip,
  ADD COLUMN kds_health_status VARCHAR(32) DEFAULT 'ONLINE' AFTER fallback_mode,
  ADD COLUMN last_heartbeat_at TIMESTAMP NULL AFTER kds_health_status;
