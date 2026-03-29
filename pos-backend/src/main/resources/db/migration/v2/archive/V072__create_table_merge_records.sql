-- G01 并台: 并台/拆台记录表
CREATE TABLE table_merge_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    master_session_id BIGINT NOT NULL,
    merged_session_id BIGINT NOT NULL,
    master_table_id BIGINT NOT NULL,
    merged_table_id BIGINT NOT NULL,
    guest_count_at_merge INT NOT NULL COMMENT '并入时被并桌人数',
    merged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unmerged_at TIMESTAMP NULL COMMENT 'NULL=仍在合并中',
    operated_by BIGINT NOT NULL COMMENT '操作人 user_id',
    reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tmr_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_tmr_master_session FOREIGN KEY (master_session_id) REFERENCES table_sessions(id),
    CONSTRAINT fk_tmr_merged_session FOREIGN KEY (merged_session_id) REFERENCES table_sessions(id),
    INDEX idx_tmr_master (master_session_id),
    INDEX idx_tmr_merged (merged_session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
