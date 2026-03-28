-- G02 支付叠加: 会员账户加冻结字段
-- 可用积分 = points_balance - frozen_points
-- 可用储值 = cash_balance_cents - frozen_cash_cents
ALTER TABLE member_accounts
  ADD COLUMN frozen_points BIGINT NOT NULL DEFAULT 0
    COMMENT '冻结积分(结算中)' AFTER points_balance,
  ADD COLUMN frozen_cash_cents BIGINT NOT NULL DEFAULT 0
    COMMENT '冻结储值(结算中)' AFTER cash_balance_cents;
