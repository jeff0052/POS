-- V091: 顾客评价/客诉表
-- Journey: J01 单点堂食, J07 店长
CREATE TABLE customer_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    feedback_no VARCHAR(64) NOT NULL,
    store_id BIGINT NOT NULL,
    table_session_id BIGINT NULL,
    member_id BIGINT NULL,
    submitted_order_id BIGINT NULL,
    feedback_type VARCHAR(32) NOT NULL DEFAULT 'REVIEW'
      COMMENT 'REVIEW | COMPLAINT | SUGGESTION',
    overall_rating INT NULL CHECK (overall_rating BETWEEN 1 AND 5),
    food_rating INT NULL CHECK (food_rating BETWEEN 1 AND 5),
    service_rating INT NULL CHECK (service_rating BETWEEN 1 AND 5),
    content TEXT NULL,
    feedback_status VARCHAR(32) NOT NULL DEFAULT 'NEW'
      COMMENT 'NEW -> IN_PROGRESS -> RESOLVED | DISMISSED',
    resolved_by BIGINT NULL,
    resolved_at TIMESTAMP NULL,
    resolution_note TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_cf_no UNIQUE (feedback_no),
    CONSTRAINT fk_cf_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_cf_order FOREIGN KEY (submitted_order_id) REFERENCES submitted_orders(id),
    INDEX idx_cf_store_status (store_id, feedback_status),
    INDEX idx_cf_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
