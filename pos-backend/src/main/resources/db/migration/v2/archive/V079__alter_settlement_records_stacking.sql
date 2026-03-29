-- G02 支付叠加: 结算记录加各项叠加明细
-- 注意: 字段加在 collected_amount_cents 之后 (该表没有 total_amount_cents)
ALTER TABLE settlement_records
  ADD COLUMN points_deduct_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '积分抵扣金额(分)' AFTER collected_amount_cents,
  ADD COLUMN points_deducted BIGINT NOT NULL DEFAULT 0
    COMMENT '消耗的积分数' AFTER points_deduct_cents,
  ADD COLUMN cash_balance_deduct_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '储值扣款(分)' AFTER points_deducted,
  ADD COLUMN coupon_discount_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '优惠券减免(分)' AFTER cash_balance_deduct_cents,
  ADD COLUMN promotion_discount_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '促销减免(分)' AFTER coupon_discount_cents,
  ADD COLUMN external_payment_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '外部支付金额(分)' AFTER promotion_discount_cents,
  ADD COLUMN coupon_id BIGINT NULL AFTER external_payment_cents,
  ADD COLUMN stacking_rule_id BIGINT NULL AFTER coupon_id;
