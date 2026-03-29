-- V072: 并台记录表
-- Journey: J11 并台
CREATE TABLE table_merge_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    master_table_id BIGINT NOT NULL,
    master_session_id BIGINT NOT NULL,
    merged_table_id BIGINT NOT NULL,
    merged_session_id BIGINT NOT NULL,
    merged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unmerged_at TIMESTAMP NULL,
    unmerged_by BIGINT NULL,
    merge_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'
      COMMENT 'ACTIVE | UNMERGED | SETTLED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tmr_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_tmr_master_table FOREIGN KEY (master_table_id) REFERENCES store_tables(id),
    CONSTRAINT fk_tmr_merged_table FOREIGN KEY (merged_table_id) REFERENCES store_tables(id),
    CONSTRAINT fk_tmr_master_session FOREIGN KEY (master_session_id) REFERENCES table_sessions(id),
    CONSTRAINT fk_tmr_merged_session FOREIGN KEY (merged_session_id) REFERENCES table_sessions(id),
    INDEX idx_tmr_master (master_session_id),
    INDEX idx_tmr_store (store_id, merge_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
