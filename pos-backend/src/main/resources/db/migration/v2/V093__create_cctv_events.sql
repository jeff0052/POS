-- G13 CCTV 事件表
CREATE TABLE cctv_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    camera_id VARCHAR(128) NOT NULL COMMENT '摄像头标识',
    camera_location VARCHAR(255) NULL COMMENT '摄像头位置描述',
    event_type VARCHAR(64) NOT NULL
      COMMENT 'SLIP_FALL|STAFF_VIOLATION|CROWD_ANOMALY|FIRE_SMOKE|THEFT_SUSPECT|HYGIENE_VIOLATION|CUSTOM',
    severity VARCHAR(32) NOT NULL DEFAULT 'INFO' COMMENT 'INFO|WARNING|CRITICAL|EMERGENCY',
    event_at TIMESTAMP NOT NULL COMMENT '事件发生时间',
    snapshot_url VARCHAR(512) NULL COMMENT '事件截图',
    video_clip_url VARCHAR(512) NULL COMMENT '事件视频片段',
    ai_confidence DECIMAL(3,2) NULL COMMENT 'AI 识别置信度 0-1',
    ai_description TEXT NULL COMMENT 'AI 生成的事件描述',
    event_status VARCHAR(32) NOT NULL DEFAULT 'NEW'
      COMMENT 'NEW|ACKNOWLEDGED|INVESTIGATING|RESOLVED|FALSE_ALARM',
    handled_by BIGINT NULL,
    handled_at TIMESTAMP NULL,
    handling_notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ce_store FOREIGN KEY (store_id) REFERENCES stores(id),
    INDEX idx_ce_severity (store_id, severity, event_status),
    INDEX idx_ce_time (store_id, event_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
