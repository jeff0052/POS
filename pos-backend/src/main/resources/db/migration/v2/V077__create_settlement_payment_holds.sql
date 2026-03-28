-- G02 支付叠加: 冻结记录持久化表
CREATE TABLE settlement_payment_holds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    table_session_id BIGINT NOT NULL COMMENT '结算的 session (主桌)',
    stacking_rule_id BIGINT NULL,

    step_order INT NOT NULL COMMENT '扣减顺序 1,2,3,4',
    hold_type VARCHAR(32) NOT NULL COMMENT 'POINTS|COUPON|CASH_BALANCE|EXTERNAL',
    hold_amount_cents BIGINT NOT NULL COMMENT '冻结金额(分)',
    points_held BIGINT NULL COMMENT '冻结积分数(仅 POINTS)',
    coupon_id BIGINT NULL COMMENT '使用的券 ID(仅 COUPON)',
    member_id BIGINT NULL,

    hold_status VARCHAR(32) NOT NULL DEFAULT 'HELD' COMMENT 'HELD|CONFIRMED|RELEASED',
    held_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP NULL,
    released_at TIMESTAMP NULL,
    release_reason VARCHAR(255) NULL,

    payment_attempt_id BIGINT NULL COMMENT '外部支付 attempt(仅 EXTERNAL)',
    settlement_record_id BIGINT NULL COMMENT '确认后关联的 settlement',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_sph_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_sph_session FOREIGN KEY (table_session_id) REFERENCES table_sessions(id),
    INDEX idx_sph_session (table_session_id, hold_status),
    INDEX idx_sph_member (member_id, hold_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
