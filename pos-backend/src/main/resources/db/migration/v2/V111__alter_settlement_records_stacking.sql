-- V111: settlement_records cashier_id nullable + 叠加明细字段
-- cashier_id 当前为 NOT NULL，stacking 流程下收银员可能是 null
ALTER TABLE settlement_records
  MODIFY COLUMN cashier_id BIGINT NULL,
  ADD COLUMN points_deduct_cents BIGINT NOT NULL DEFAULT 0 AFTER collected_amount_cents,
  ADD COLUMN points_deducted BIGINT NOT NULL DEFAULT 0 AFTER points_deduct_cents,
  ADD COLUMN cash_balance_deduct_cents BIGINT NOT NULL DEFAULT 0 AFTER points_deducted,
  ADD COLUMN coupon_discount_cents BIGINT NOT NULL DEFAULT 0 AFTER cash_balance_deduct_cents,
  ADD COLUMN external_payment_cents BIGINT NOT NULL DEFAULT 0 AFTER coupon_discount_cents,
  ADD COLUMN coupon_id BIGINT NULL AFTER external_payment_cents;
