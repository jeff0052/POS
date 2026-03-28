-- G16 KDS 回退打印机: 工作站加心跳监测和回退配置
ALTER TABLE kitchen_stations
  ADD COLUMN fallback_printer_ip VARCHAR(64) NULL
    COMMENT 'KDS 故障时回退的打印机 IP' AFTER kds_display_id,
  ADD COLUMN kds_health_status VARCHAR(32) NOT NULL DEFAULT 'ONLINE'
    COMMENT 'ONLINE|OFFLINE|DEGRADED' AFTER fallback_printer_ip,
  ADD COLUMN last_heartbeat_at TIMESTAMP NULL COMMENT 'KDS 最后心跳时间' AFTER kds_health_status,
  ADD COLUMN fallback_mode VARCHAR(32) NOT NULL DEFAULT 'AUTO'
    COMMENT 'AUTO|MANUAL|DISABLED' AFTER last_heartbeat_at;
