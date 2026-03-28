-- G09 顾客反馈 / Wish List
CREATE TABLE customer_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    member_id BIGINT NULL COMMENT '匿名反馈时为 NULL',
    order_id BIGINT NULL COMMENT '关联 submitted_order (可选)',
    feedback_type VARCHAR(32) NOT NULL COMMENT 'REVIEW|COMPLAINT|SUGGESTION|WISH_LIST',
    overall_rating INT NULL COMMENT '1-5',
    food_rating INT NULL,
    service_rating INT NULL,
    ambience_rating INT NULL,
    content TEXT NULL,
    photo_urls JSON NULL,
    tags JSON NULL COMMENT '["太咸","上菜慢","推荐新品"]',
    wished_item_name VARCHAR(255) NULL COMMENT '顾客想要的菜品(WISH_LIST)',
    wish_vote_count INT NOT NULL DEFAULT 1 COMMENT '同类 wish 的投票数',
    feedback_status VARCHAR(32) NOT NULL DEFAULT 'NEW'
      COMMENT 'NEW|ACKNOWLEDGED|IN_PROGRESS|RESOLVED|CLOSED',
    response_text TEXT NULL COMMENT '商家回复',
    responded_by BIGINT NULL,
    responded_at TIMESTAMP NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'QR_ORDER' COMMENT 'QR_ORDER|POS|APP|GOOGLE|MANUAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cf_store FOREIGN KEY (store_id) REFERENCES stores(id),
    INDEX idx_cf_type (store_id, feedback_type, created_at),
    INDEX idx_cf_status (store_id, feedback_status),
    INDEX idx_cf_rating (store_id, overall_rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
