# 128 表逐表对账 — 5 大系统 × 子系统归属

**Version:** V20260328018
**Date:** 2026-03-28
**Status:** DRAFT
**目的：** 精确确认每张表归属哪个子系统，修正框架数字

---

## 方法

1. 从 `docs/75-complete-database-schema.md` 逐条取 #1-#119（含 Legacy）
2. 从 `docs/80-step-0.3-data-model-gaps.md` 取 9 张新表（标 🆕）
3. 每张表映射到唯一子系统
4. 汇总计数

---

## 一、交易核心

### 1.1 预约与候位 (2)

| # | 表名 | 来源 |
|---|------|------|
| 90 | reservations | Flyway V018 |
| 91 | queue_tickets | doc/66 |

### 1.2 订单引擎 (8 → 含 2 张新表)

| # | 表名 | 来源 |
|---|------|------|
| 25 | table_sessions | Flyway V012 + doc/66 + doc/73 |
| 22 | active_table_orders | Flyway V004 + V030 |
| 23 | active_table_order_items | Flyway V004 |
| 24 | order_events | Flyway V004 |
| 26 | submitted_orders | Flyway V012 + V030 + doc/66 + doc/73 |
| 27 | submitted_order_items | Flyway V012 |
| 🆕 | qr_tokens | V071 |
| 🆕 | table_merge_records | V072 |

### 1.3 结算与退款 (4 → 含 1 张新表)

| # | 表名 | 来源 |
|---|------|------|
| 31 | settlement_records | Flyway V008 + V019 + V030 + V050 |
| 32 | payment_attempts | Flyway V015 |
| 33 | refund_records | Flyway V050 |
| 🆕 | settlement_payment_holds | V076 |

### 一 小计：2 + 8 + 4 = **14**

---

## 二、商品与供应

### 2.1 商品目录 Catalog (10)

| # | 表名 | 来源 |
|---|------|------|
| 8 | product_categories | Flyway V003 |
| 9 | products | Flyway V003 + V016 + V065 + doc/66 |
| 10 | skus | Flyway V003 + V065 + V030 + doc/66 + doc/67 |
| 11 | store_sku_availability | Flyway V003 + V030 |
| 12 | sku_price_overrides | doc/66 |
| 13 | sku_channel_configs | doc/67 |
| 14 | sku_faq | doc/67 |
| 15 | modifier_groups | doc/67 |
| 16 | modifier_options | doc/67 |
| 17 | sku_modifier_group_bindings | doc/67 |

### 2.2 菜单控制 Menu (4)

| # | 表名 | 来源 |
|---|------|------|
| 18 | menu_time_slots | doc/66 |
| 19 | menu_time_slot_products | doc/66 |
| 20 | buffet_packages | doc/66 |
| 21 | buffet_package_items | doc/66 |

### 2.3 厨房出品 Kitchen (3)

| # | 表名 | 来源 |
|---|------|------|
| 28 | kitchen_stations | doc/66 |
| 29 | kitchen_tickets | doc/66 |
| 30 | kitchen_ticket_items | doc/66 |

### 2.4 库存管理 Inventory (17 → 含 1 张新表)

| # | 表名 | 来源 |
|---|------|------|
| 34 | inventory_items | doc/66 + doc/70 |
| 35 | inventory_batches | doc/70 |
| 36 | recipes | doc/66 |
| 37 | purchase_invoices | doc/66 |
| 38 | purchase_invoice_items | doc/66 |
| 39 | purchase_orders | doc/70 |
| 40 | purchase_order_items | doc/70 |
| 41 | inventory_movements | doc/66 + doc/70 |
| 42 | suppliers | doc/66 + doc/70 |
| 43 | supplier_price_history | doc/70 |
| 44 | order_suggestions | doc/66 + doc/70 |
| 45 | stocktake_tasks | doc/70 |
| 46 | stocktake_items | doc/70 |
| 47 | waste_records | doc/70 |
| 48 | inventory_transfers | doc/70 |
| 49 | inventory_transfer_items | doc/70 |
| 🆕 | sop_import_batches | V088 |

### 2.5 推荐系统 Recommendations (2)

| # | 表名 | 来源 |
|---|------|------|
| 98 | recommendation_slots | doc/66 |
| 99 | recommendation_slot_items | doc/66 |

### 2.6 外卖对接 Delivery (3 → 含 1 张新表)

| # | 表名 | 来源 |
|---|------|------|
| 100 | delivery_platform_configs | doc/66 |
| 101 | delivery_orders | doc/66 |
| 🆕 | external_integration_logs | V092 |

> **注：** `external_integration_logs` 虽然是跨模块（外卖 J03 + Google 预约 J12 + 财务 J10 都用），但物理上最频繁的调用方是外卖对接，放这里。子系统改名 **"外部对接 Integration"** 更准确。

### 二 小计：10 + 4 + 3 + 17 + 2 + 3 = **39**

---

## 三、客户与营销

### 3.1 会员基础 Members (5)

| # | 表名 | 来源 |
|---|------|------|
| 50 | members | Flyway V009 + V030 + doc/71 |
| 51 | member_accounts | Flyway V009 + doc/71 + doc/72 |
| 55 | member_tier_rules | doc/71 |
| 52 | member_recharge_orders | Flyway V013 |
| 58 | recharge_campaigns | doc/71 |

### 3.2 积分体系 Points (5)

| # | 表名 | 来源 |
|---|------|------|
| 53 | member_points_ledger | Flyway V013 + doc/72 |
| 59 | points_rules | doc/72 |
| 60 | points_expiry_rules | doc/72 |
| 61 | points_batches | doc/72 |
| 62 | points_deduction_rules | doc/72 |

### 3.3 储值体系 Stored Value (2)

| # | 表名 | 来源 |
|---|------|------|
| 54 | member_cash_ledger | doc/72 |
| 63 | cash_balance_rules | doc/72 |

### 3.4 优惠券 Coupons (2)

| # | 表名 | 来源 |
|---|------|------|
| 56 | coupon_templates | doc/71 |
| 57 | member_coupons | doc/71 |

### 3.5 推荐裂变 Referral (2)

| # | 表名 | 来源 |
|---|------|------|
| 66 | referral_rewards_config | doc/71 |
| 67 | referral_records | doc/71 |

### 3.6 会员运营 Operations (5 → 含 1 张新表)

| # | 表名 | 来源 |
|---|------|------|
| 68 | member_tags | doc/71 |
| 69 | member_tag_assignments | doc/71 |
| 70 | member_consumption_profiles | doc/71 |
| 64 | points_redemption_items | doc/71 |
| 65 | points_redemption_records | doc/71 |

> **注：** `customer_feedback` 虽然跟顾客相关，但更偏运营管理，放新的 4.6 审计运营更合理（和 audit_trail 一起）。见下方决策点。

### 3.7 营销触达 Marketing (2)

| # | 表名 | 来源 |
|---|------|------|
| 71 | marketing_campaigns | doc/71 |
| 72 | marketing_send_records | doc/71 |

### 3.8 渠道分润 Channels (6)

| # | 表名 | 来源 |
|---|------|------|
| 102 | channels | doc/73 |
| 103 | channel_commission_rules | doc/73 |
| 104 | order_channel_attribution | doc/73 |
| 105 | channel_commission_records | doc/73 |
| 106 | channel_settlement_batches | doc/73 |
| 107 | channel_performance_daily | doc/73 |

### 3.9 促销引擎 Promotions (5 → 含 1 张新表)

| # | 表名 | 来源 |
|---|------|------|
| 94 | promotion_rules | Flyway V010 + V030 + V035 |
| 95 | promotion_rule_conditions | Flyway V010 |
| 96 | promotion_rule_rewards | Flyway V010 + V035 |
| 97 | promotion_hits | Flyway V010 |
| 🆕 | inventory_driven_promotions | V089 |

### 三 小计：5 + 5 + 2 + 2 + 2 + 5 + 2 + 6 + 5 = **34**

---

## 四、组织与运营

### 4.1 商户体系 Core (7)

| # | 表名 | 来源 |
|---|------|------|
| 1 | merchants | Flyway V001 |
| 2 | merchant_configs | Flyway V001 |
| 3 | brands | Flyway V055 |
| 4 | brand_countries | Flyway V055 |
| 5 | stores | Flyway V002 + V055 |
| 6 | store_tables | Flyway V002 + doc/66 |
| 7 | store_terminals | Flyway V002 |

> **决策点：** `store_tables` 归 4.1（物理基础设施）还是 1.2（订单引擎）？
> **建议：** 留在 4.1。store_tables 是 entity master data，不随订单生命周期创建/删除。订单引擎通过 FK 引用它。

### 4.2 权限体系 RBAC (6)

| # | 表名 | 来源 |
|---|------|------|
| 73 | users | doc/68 |
| 74 | permissions | doc/68 |
| 75 | custom_roles | doc/68 |
| 76 | custom_role_permissions | doc/68 |
| 77 | user_roles | doc/68 |
| 78 | user_store_access | doc/68 |

### 4.3 员工管理 Employees (9)

| # | 表名 | 来源 |
|---|------|------|
| 79 | employees | doc/69 |
| 80 | shift_templates | doc/69 |
| 81 | employee_schedules | doc/69 |
| 82 | attendance_records | doc/69 |
| 83 | leave_requests | doc/69 |
| 84 | leave_balances | doc/69 |
| 85 | payroll_periods | doc/69 |
| 86 | payroll_records | doc/69 |
| 87 | employee_performance_log | doc/69 |

### 4.4 班次管理 Shifts (2)

| # | 表名 | 来源 |
|---|------|------|
| 88 | cashier_shifts | Flyway V026 + doc/69 |
| 89 | cashier_shift_settlements | Flyway V026 |

### 4.5 税务合规 GTO (2)

| # | 表名 | 来源 |
|---|------|------|
| 92 | gto_export_batches | Flyway V020 |
| 93 | gto_export_items | Flyway V020 |

### 4.6 审计与反馈 Audit (2 张新表 🆕)

| # | 表名 | 来源 |
|---|------|------|
| 🆕 | audit_trail | V073 |
| 🆕 | customer_feedback | V091 |

> **理由：** audit_trail 跨所有模块（退款审批、改价审批、AI 操作审计），不属于任何单一业务模块。customer_feedback 本质是运营管理（店长处理客诉），和审计同属"运营治理"范畴。

### 四 小计：7 + 6 + 9 + 2 + 2 + 2 = **28**

---

## 五、AI 层

### 5.1 MCP 工具层 (2)

| # | 表名 | 来源 |
|---|------|------|
| 108 | action_log | Flyway V031 |
| 111 | ai_scheduled_checks | Flyway V040 |

### 5.2 AI 顾问 Advisor (3 → 含 1 张新表)

| # | 表名 | 来源 |
|---|------|------|
| 109 | ai_proposal | Flyway V032 |
| 110 | ai_recommendations | Flyway V040 |
| 🆕 | report_snapshots | V094 |

> **注：** `report_snapshots` 含 `ai_summary` / `ai_highlights` / `ai_warnings` / `ai_suggestions` 字段，是 AI 顾问的核心输出载体。

### 5.3 Agent 身份 (4)

| # | 表名 | 来源 |
|---|------|------|
| 112 | restaurant_agents | Flyway V045 |
| 113 | agent_wallets | Flyway V045 |
| 114 | wallet_transactions | Flyway V045 |
| 115 | agent_interactions | Flyway V045 |

### 5.4 资产管理 Assets (1)

| # | 表名 | 来源 |
|---|------|------|
| 116 | image_assets | Flyway V065 |

### 五 小计：2 + 3 + 4 + 1 = **10**

---

## Legacy（待迁移，不计入框架）

| # | 表名 | 说明 |
|---|------|------|
| 117 | auth_users | → 被 users (#73) 替代 |
| 118 | staff | → 被 employees (#79) 替代 |
| 119a | roles | → 被 custom_roles (#75) 替代 |
| 119b | role_permissions | → 被 custom_role_permissions (#76) 替代 |

= 4 张物理表，doc/75 编号为 3 条（#117-#119）

---

## 汇总对比

| 系统 | 原框架 header | 原框架子系统 sum | 实际（含新表） | 差异说明 |
|------|-------------|----------------|-------------|---------|
| 一 交易核心 | 13 | 11 | **14** | +3 新表（qr_tokens, table_merge_records, settlement_payment_holds） |
| 二 商品与供应 | 43 | 39 | **39** | +2 新表（sop_import_batches, external_integration_logs）；原 header 43 是错的，子系统 sum=39 也和实际 37 差 2（2.1 多报了 2） |
| 三 客户与营销 | 33 | 35 | **34** | +1 新表（inventory_driven_promotions）；原 header 33 和 subsystem sum 35 不一致 |
| 四 组织与运营 | 25 | 25 | **28** | +2 新表（audit_trail, customer_feedback）+ store_tables 归这里使 4.1=7 |
| 五 AI 层 | 9 | 9 | **10** | +1 新表（report_snapshots） |
| Legacy | — | — | (4) | 不计入框架 |
| **总计** | **123** | **119** | **125 + 4 legacy** | |

> **注意：** 125 ≠ 之前说的 128。差值 3 的原因：原框架子系统加总 = 119 = 实际非 Legacy 表数。9 张新表加入后 = 128 物理表，但 Legacy 4 张不计入框架 → 框架内 = 128 - 4 = 124。上表中 14+39+34+28+10 = 125……差 1 在哪？

让我再数一遍：
- 一：2+8+4 = 14 ✅
- 二：10+4+3+17+2+3 = 39 ✅
- 三：5+5+2+2+2+5+2+6+5 = 34 ✅
- 四：7+6+9+2+2+2 = 28 ✅
- 五：2+3+4+1 = 10 ✅
- 总计：14+39+34+28+10 = **125**

物理表总数：125 框架内 + 4 Legacy = **129** 物理表

等等——原 doc/75 编号到 119，但 #119 = roles + role_permissions = 2 张物理表。所以：
- 原物理表 = 120（不是 119）
- + 9 新表 = 129
- - 4 Legacy = **125 框架内**

**结论：原文档 "119 表" 是编号数，物理表实为 120。**

---

## 修正后框架

```
一、交易核心 (14 tables)                          ← was 13
 ├── 1.1 预约与候位      Reservations (2)
 ├── 1.2 订单引擎        Orders (8)               ← was 6, +qr_tokens +table_merge_records
 └── 1.3 结算与退款      Settlement (4)            ← was 3, +settlement_payment_holds

二、商品与供应 (39 tables)                         ← was 43
 ├── 2.1 商品目录        Catalog (10)              ← was 12
 ├── 2.2 菜单控制        Menu (4)
 ├── 2.3 厨房出品        Kitchen (3)
 ├── 2.4 库存管理        Inventory (17)            ← was 16, +sop_import_batches
 ├── 2.5 推荐系统        Recommendations (2)
 └── 2.6 外部对接        Integration (3)           ← was Delivery(2), +external_integration_logs

三、客户与营销 (34 tables)                         ← was 33
 ├── 3.1 会员基础        Members (5)               ← was 6
 ├── 3.2 积分体系        Points (5)
 ├── 3.3 储值体系        Stored Value (2)
 ├── 3.4 优惠券          Coupons (2)
 ├── 3.5 推荐裂变        Referral (2)
 ├── 3.6 会员运营        Operations (5)            ← was 6
 ├── 3.7 营销触达        Marketing (2)
 ├── 3.8 渠道分润        Channels (6)
 └── 3.9 促销引擎        Promotions (5)            ← was 4, +inventory_driven_promotions

四、组织与运营 (28 tables)                         ← was 25
 ├── 4.1 商户体系        Core (7)                  ← was 6, store_tables 归这里
 ├── 4.2 权限体系        RBAC (6)
 ├── 4.3 员工管理        Employees (9)
 ├── 4.4 班次管理        Shifts (2)
 ├── 4.5 税务合规        GTO (2)
 └── 4.6 审计与反馈      Audit (2)                 ← 新子系统: audit_trail + customer_feedback

五、AI 层 (10 tables)                             ← was 9
 ├── 5.1 MCP 工具层      MCP (2)
 ├── 5.2 AI 顾问         Advisor (3)               ← was 2, +report_snapshots
 ├── 5.3 Agent 身份      Agent (4)
 └── 5.4 资产管理        Assets (1)

Legacy（待迁移，不计入）: auth_users, staff, roles, role_permissions = 4

总计框架内：14 + 39 + 34 + 28 + 10 = 125
总计物理表：125 + 4 Legacy = 129
```

---

## 决策点（已确认 ✅ 2026-03-28）

| # | 问题 | 结论 | 理由 |
|---|------|------|------|
| D1 | store_tables 归哪里？ | ✅ **4.1 商户体系** | master data，不随订单创建/销毁 |
| D2 | customer_feedback 归哪里？ | ✅ **4.6 审计与反馈** | 主 actor 是店长，处理流程偏运营管控 |
| D3 | external_integration_logs 归哪里？ | ✅ **2.6 外部对接** | 跟业务对接强绑定，非泛化审计 |
| D4 | report_snapshots 归哪里？ | ✅ **5.2 AI 顾问** | 建表的原因是存 AI 生成内容 |
