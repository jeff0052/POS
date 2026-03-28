-- G02 支付叠加: 叠加规则表
CREATE TABLE payment_stacking_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    store_id BIGINT NULL COMMENT 'NULL=品牌级规则',
    rule_name VARCHAR(128) NOT NULL,

    -- 叠加开关
    allow_points_deduct BOOLEAN NOT NULL DEFAULT TRUE,
    allow_cash_balance BOOLEAN NOT NULL DEFAULT TRUE,
    allow_coupon BOOLEAN NOT NULL DEFAULT TRUE,
    allow_mixed_payment BOOLEAN NOT NULL DEFAULT TRUE,

    -- 扣减优先级 (数字越小越先扣)
    points_priority INT NOT NULL DEFAULT 1,
    coupon_priority INT NOT NULL DEFAULT 2,
    cash_balance_priority INT NOT NULL DEFAULT 3,
    external_payment_priority INT NOT NULL DEFAULT 4,

    -- 积分限制
    max_points_deduct_percent INT NOT NULL DEFAULT 50 COMMENT '积分最多抵扣订单金额的%',
    points_to_cents_rate INT NOT NULL DEFAULT 100 COMMENT '多少积分=1分钱',
    min_points_deduct BIGINT NOT NULL DEFAULT 0,

    -- 储值限制
    max_cash_balance_percent INT NOT NULL DEFAULT 100,

    -- 优惠券限制
    max_coupons_per_order INT NOT NULL DEFAULT 1,
    coupon_stackable_with_promotion BOOLEAN NOT NULL DEFAULT FALSE,

    -- 适用范围
    applicable_dining_modes JSON NOT NULL DEFAULT '["A_LA_CARTE","BUFFET","DELIVERY"]',
    applicable_order_min_cents BIGINT NOT NULL DEFAULT 0,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0 COMMENT '多条规则时取 priority 最高的 active 规则',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_psr_merchant FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    INDEX idx_psr_lookup (merchant_id, store_id, is_active, priority DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
