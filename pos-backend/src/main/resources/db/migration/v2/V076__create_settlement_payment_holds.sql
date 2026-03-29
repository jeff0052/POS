-- V076: 结算支付冻结表
-- Journey: J04 会员支付叠加
-- 冻结→确认→释放三态，支持积分/储值/券
CREATE TABLE settlement_payment_holds (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    hold_no VARCHAR(64) NOT NULL,
    settlement_record_id BIGINT NULL,
    member_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    hold_type VARCHAR(32) NOT NULL
      COMMENT 'POINTS | CASH | COUPON',
    hold_amount BIGINT NOT NULL
      COMMENT 'points count or cents depending on hold_type',
    hold_ref VARCHAR(128) NULL
      COMMENT 'coupon_no / points_batch_id etc',
    hold_status VARCHAR(32) NOT NULL DEFAULT 'HELD'
      COMMENT 'HELD -> CONFIRMED | RELEASED',
    held_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP NULL,
    released_at TIMESTAMP NULL,
    release_reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_sph_hold_no UNIQUE (hold_no),
    CONSTRAINT fk_sph_member FOREIGN KEY (member_id) REFERENCES members(id),
    INDEX idx_sph_member (member_id, hold_status),
    INDEX idx_sph_settlement (settlement_record_id),
    INDEX idx_sph_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
