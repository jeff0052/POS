-- V101: 补充缺失索引
-- 来源: docs/82-step-0.4-ddl-review.md INDEX-1 + agent review I4/I5

-- SKU 维度查询（销量分析、在制查询、厨房票查询）
CREATE INDEX idx_soi_sku ON submitted_order_items (sku_id);
CREATE INDEX idx_atoi_sku ON active_table_order_items (sku_id);
CREATE INDEX idx_kti_sku ON kitchen_ticket_items (sku_id);

-- 结算按门店/收银员查询
CREATE INDEX idx_sr_store ON settlement_records (store_id, created_at);
CREATE INDEX idx_sr_cashier ON settlement_records (cashier_id);

-- 会员订单历史（CRM 高频查询）
CREATE INDEX idx_so_member ON submitted_orders (member_id);

-- 结算按商户查询（多店 dashboard）
CREATE INDEX idx_sr_merchant ON settlement_records (merchant_id);

-- 支付对账按门店/日期
CREATE INDEX idx_pa_store ON payment_attempts (store_id, created_at);
