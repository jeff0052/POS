# V2 Database Migration Baseline

## Goal

定义 Restaurant POS `v2 foundation` 的数据库迁移基线，明确：
- V2 应该从哪些核心表开始
- 迁移文件如何命名
- 哪些表属于哪个 domain
- 哪些快照必须从第一天就保留

本文件是后端模块结构的数据库对应版本。

---

## Design Position

V2 数据库采用：
- 结构化迁移管理
- 按 domain 分阶段落表
- 严格版本演进

不再继续依赖：
- 单个 `init.sql` 无限增长
- 原型表和正式表混用
- 临时字段驱动表设计

建议 V2 正式使用：
- Flyway
或
- Liquibase

当前推荐优先：
- Flyway

---

## Migration Naming Convention

建议使用：

```text
V001__merchant_base.sql
V002__store_and_table.sql
V003__catalog_and_sku.sql
V004__active_table_order.sql
V005__staff_and_shift.sql
V006__member_crm.sql
V007__promotion_rules.sql
V008__settlement_and_payment.sql
V009__report_foundation.sql
V010__gto_export_batches.sql
V011__platform_admin.sql
```

规则：
- 只增不改历史迁移文件
- 每份迁移文件只负责一个清晰主题
- 名称必须表达 domain 和内容

---

## V2 Migration Phases

## Phase 1: Merchant and Store Foundation

### V001__merchant_base.sql
核心表：
- `merchants`
- `merchant_configs`

目的：
- 建立商户主体
- 支撑后续多门店与平台总后台

### V002__store_and_table.sql
核心表：
- `stores`
- `store_settings`
- `store_terminals`
- `store_tables`

目的：
- 建立门店基础能力
- 把桌台作为一等对象建模

---

## Phase 2: Catalog / SKU Foundation

### V003__catalog_and_sku.sql
核心表：
- `product_categories`
- `products`
- `skus`
- `sku_option_groups`
- `sku_options`
- `sku_option_values`
- `store_sku_availability`
- `sku_price_rules`

目的：
- 把餐饮商品从“product 列表”升级成真正可售卖的 SKU 体系

关键原则：
- 订单交易对象是 `sku`
- 不直接用 `product` 作为最终成交单元

---

## Phase 3: Active Table Order Foundation

### V004__active_table_order.sql
核心表：
- `active_table_orders`
- `active_table_order_items`
- `order_events`
- `order_item_modifiers`

目的：
- 建立一桌一单模型
- 支撑 QR / POS 共编辑
- 记录活动订单和改单过程

建议关键字段：

### active_table_orders
- `id`
- `merchant_id`
- `store_id`
- `table_id`
- `order_no`
- `order_source` (`POS`, `QR`)
- `dining_type`
- `member_id` nullable
- `status` (`DRAFT`, `SUBMITTED`, `PENDING_SETTLEMENT`, `SETTLED`)
- `kitchen_status`
- `cashier_id` nullable
- `current_shift_id` nullable
- `original_amount_cents`
- `member_discount_cents`
- `promotion_discount_cents`
- `payable_amount_cents`
- `pricing_snapshot_json`
- `promotion_snapshot_json`
- `settlement_snapshot_json`
- `created_at`
- `updated_at`

### active_table_order_items
- `id`
- `active_order_id`
- `sku_id`
- `sku_name_snapshot`
- `unit_price_snapshot_cents`
- `member_price_snapshot_cents`
- `quantity`
- `item_remark`
- `option_snapshot_json`
- `line_total_cents`
- `created_at`
- `updated_at`

### order_events
用于记录：
- QR 提交
- cashier 加菜
- cashier 删菜
- 送厨
- 进入待结
- 结账完成

---

## Phase 4: Staff and Shift

### V005__staff_and_shift.sql
核心表：
- `staff_users`
- `staff_roles`
- `staff_role_permissions`
- `store_staff_assignments`
- `cashier_shifts`
- `cashier_shift_events`

目的：
- 支撑 cashier 登录、开班、换班、交班、责任归属

---

## Phase 5: Member / CRM

### V006__member_crm.sql
核心表：
- `members`
- `member_accounts`
- `member_tiers`
- `member_tier_rules`
- `member_points_ledger`
- `member_balance_ledger`
- `member_recharge_orders`
- `member_benefits`
- `member_upgrade_history`

目的：
- 把会员作为独立经营域建立

---

## Phase 6: Promotion

### V007__promotion_rules.sql
核心表：
- `promotion_rules`
- `promotion_rule_conditions`
- `promotion_rule_rewards`
- `promotion_hits`

目的：
- 支撑满减、满赠、会员价、等级折扣

关键原则：
- 规则与订单解耦
- 订单只保存命中快照

---

## Phase 7: Settlement and Payment

### V008__settlement_and_payment.sql
核心表：
- `settlement_records`
- `payment_records`
- `refund_records`
- `print_records`

目的：
- 把订单和收款/退款/打印解耦
- 让结账域成为独立事实来源

建议关键字段：

### settlement_records
- `id`
- `active_order_id`
- `merchant_id`
- `store_id`
- `table_id`
- `cashier_id`
- `shift_id`
- `settlement_no`
- `status`
- `original_amount_cents`
- `member_discount_cents`
- `promotion_discount_cents`
- `payable_amount_cents`
- `paid_amount_cents`
- `settled_at`

---

## Phase 8: Reporting Foundation

### V009__report_foundation.sql
核心表：
- `sales_daily_snapshots`
- `sales_item_facts`
- `sales_discount_facts`
- `member_sales_facts`
- `table_turnover_facts`

目的：
- 为商户后台报表和 AI 分析做事实层

关键原则：
- 报表不要直接从 UI 临时拼
- 应围绕交易事实表沉淀

---

## Phase 9: GTO

### V010__gto_export_batches.sql
核心表：
- `gto_export_batches`
- `gto_export_items`
- `gto_export_logs`

目的：
- 支撑日结批量导出、重试、失败追踪

---

## Phase 10: Platform Admin

### V011__platform_admin.sql
核心表：
- `platform_users`
- `platform_roles`
- `platform_permissions`
- `configuration_templates`
- `merchant_provisioning_logs`

目的：
- 支撑总后台账号、权限、模板配置和商户开通

---

## Source of Truth Rules

### 1. Active Order Is the Source of Current Table State
- 桌台当前状态来自活动桌单
- 不应由页面或临时缓存单独定义

### 2. Settlement Records Are the Source of Payment Truth
- 支付与退款以结账域流水为准
- 不应把订单表当作唯一收款事实表

### 3. Catalog Owns SKU Definition
- SKU 的定义、选项、价格归 catalog 域
- 订单只保存快照

### 4. CRM Owns Member Account Facts
- 积分、余额、等级变化必须有独立流水

---

## Required Snapshots from Day One

以下快照必须从第一天开始保存：
- `sku_name_snapshot`
- `unit_price_snapshot`
- `member_price_snapshot`
- `option_snapshot_json`
- `pricing_snapshot_json`
- `promotion_snapshot_json`
- `settlement_snapshot_json`

原因：
- 商品会改
- 价格会改
- 促销会改
- 会员权益会改

历史订单和报表不能被未来配置污染。

---

## What the Current Prototype Schema Should Become

当前原型里已有的：
- `stores`
- `store_settings`
- `product_categories`
- `products`
- `orders`
- `qr_table_orders`

在 V2 中的定位应当是：
- `stores` / `store_settings` 保留并升级
- `products` 升级为 `products + skus`
- `orders + qr_table_orders` 重构为：
  - `active_table_orders`
  - `active_table_order_items`
  - `settlement_records`
  - `payment_records`

也就是说：
- V2 不建议继续把 `qr_table_orders` 作为长期主模型
- 它在原型阶段有价值，但正式结构应统一到活动桌单模型

---

## Immediate Next Step

数据库基线确定后，下一步应继续补：

1. V2 API contract baseline
2. V2 backend bootstrap structure
3. V2 first migration set for:
   - store and table
   - catalog and sku
   - active table order

---

## Final Position

V2 的数据库不应从原型表继续自然增长。

应从一开始就：
- 按 domain 规划
- 按迁移版本演进
- 按交易事实和快照原则落表

这样后续四端扩展、AI 分析、总后台、GTO、会员和促销，都会有可持续的数据库基础。
