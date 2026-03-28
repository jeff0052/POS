# FounderPOS Database Schema README

**Version:** V20260328010
**Date:** 2026-03-28

---

## Overview

FounderPOS 使用 MySQL 8.4，通过 Flyway 管理 schema 迁移。数据库设计围绕 **SKU 为核心**，覆盖餐饮全链路：从原料采购到厨房出品到销售到报表。

**总表数：119 张**，分布在 20 个模块。

---

## Module Map

```
┌─────────────────────────────────────────────────────────────────┐
│                     FounderPOS Database                         │
│                                                                 │
│  ┌─────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐            │
│  │  Core   │ │ Catalog  │ │ Orders  │ │Settlement│            │
│  │ 6 tables│ │ 12 tables│ │ 6 tables│ │ 3 tables │            │
│  └─────────┘ └──────────┘ └─────────┘ └──────────┘            │
│                                                                 │
│  ┌─────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐            │
│  │  Menu   │ │  Buffet  │ │   KDS   │ │Inventory │            │
│  │ 2 tables│ │ 2 tables │ │ 3 tables│ │ 16 tables│            │
│  └─────────┘ └──────────┘ └─────────┘ └──────────┘            │
│                                                                 │
│  ┌─────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐            │
│  │   CRM   │ │Points/SV │ │  RBAC   │ │ Employee │            │
│  │17 tables│ │ 6 tables │ │ 6 tables│ │ 9 tables │            │
│  └─────────┘ └──────────┘ └─────────┘ └──────────┘            │
│                                                                 │
│  ┌─────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐            │
│  │ Channel │ │Promotion │ │  Recom  │ │ Delivery │            │
│  │ 6 tables│ │ 4 tables │ │ 2 tables│ │ 2 tables │            │
│  └─────────┘ └──────────┘ └─────────┘ └──────────┘            │
│                                                                 │
│  ┌─────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐            │
│  │ Shifts  │ │Reserv/Q  │ │   GTO   │ │    AI    │            │
│  │ 2 tables│ │ 2 tables │ │ 2 tables│ │ 8 tables │            │
│  └─────────┘ └──────────┘ └─────────┘ └──────────┘            │
│                                                                 │
│  ┌──────────┐                                                   │
│  │  Assets  │                                                   │
│  │ 1 table  │                                                   │
│  └──────────┘                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Table Count by Module

| Module | Tables | Description |
|--------|--------|-------------|
| Core | 6 | merchants, brands, brand_countries, stores, store_tables, store_terminals |
| Catalog | 12 | categories, products, skus, availability, price overrides, channel configs, FAQ, modifiers |
| Time-slot Menu | 2 | menu_time_slots, menu_time_slot_products |
| Buffet | 2 | buffet_packages, buffet_package_items |
| Orders | 6 | active_table_orders, items, table_sessions, submitted_orders, items, events |
| Kitchen KDS | 3 | kitchen_stations, kitchen_tickets, kitchen_ticket_items |
| Settlement | 3 | settlement_records, payment_attempts, refund_records |
| Inventory | 16 | items, batches, recipes, invoices, POs, movements, suppliers, prices, stocktake, waste, transfers |
| CRM Members | 17 | members, accounts, recharge, coupons, referrals, tags, profiles, marketing |
| Points & Stored Value | 6 | points_rules, expiry, batches, deduction, cash_ledger, cash_rules |
| RBAC | 6 | users, permissions, custom_roles, role_permissions, user_roles, user_store_access |
| Employees | 9 | employees, shift_templates, schedules, attendance, leave, payroll, performance |
| Channels | 6 | channels, commission_rules, attribution, commission_records, settlement_batches, performance |
| Promotions | 4 | promotion_rules, conditions, rewards, hits |
| Recommendations | 2 | recommendation_slots, slot_items |
| Delivery | 2 | delivery_platform_configs, delivery_orders |
| Shifts | 2 | cashier_shifts, cashier_shift_settlements |
| Reservations & Queue | 2 | reservations, queue_tickets |
| GTO | 2 | gto_export_batches, gto_export_items |
| AI & Agents | 8 | action_log, proposals, recommendations, scheduled_checks, agents, wallets, transactions, interactions |
| Assets | 1 | image_assets |
| **Total** | **119** | |

---

## Core Relationships

### SKU is the Center

```
suppliers → purchase_orders → inventory_items ← recipes → skus
                                                           │
                                    sku_price_overrides ────┤
                                    sku_channel_configs ────┤
                                    sku_modifier_group_bindings → modifier_groups → modifier_options
                                    sku_faq ───────────────┤
                                    store_sku_availability ─┤
                                    buffet_package_items ───┤
                                    recommendation_slot_items ──┤
                                                           │
                                                           ↓
                              submitted_order_items (SKU snapshot)
                                         │
                              kitchen_tickets (station routing)
                                         │
                              settlement_records → reports → GTO
```

### Order Flow

```
table_sessions → active_table_orders → submitted_orders → settlement_records
                      │                      │                    │
                active_order_items    submitted_order_items   refund_records
                      │                      │
                 order_events          kitchen_tickets
```

### Member Flow

```
members → member_accounts
           ├── points_batches (FIFO expiry)
           ├── member_points_ledger (积分流水)
           ├── member_cash_ledger (储值流水)
           ├── member_coupons → coupon_templates
           ├── member_tag_assignments → member_tags
           ├── member_consumption_profiles
           └── referral_records
```

### Channel Flow

```
channels → channel_commission_rules
              ↓
order_channel_attribution → submitted_orders
              ↓
channel_commission_records → channel_settlement_batches
              ↓
channel_performance_daily (ROI analysis)
```

---

## Key Design Decisions

### 1. SKU Three-Layer Model
每个 SKU 同时承载三个维度：
- **顾客侧**：名称、图片、描述、过敏原、修饰符、价格
- **厨房侧**：工作站路由、打印路由、出品备注、预估时间
- **库存侧**：成本价、SOP 配方、消耗量、是否扣库存

### 2. Price Override Architecture
价格不硬编码在 SKU 上。`sku_price_overrides` 支持无限场景：
- 不同门店不同价
- 不同渠道不同价（Grab / Foodpanda / 自有外卖）
- 不同时段不同价（午市 / 晚市 / 宵夜）
- 不同会员等级不同价
- 不同自助档位不同价

四级 fallback：门店+场景 → 品牌+场景 → 门店基础价 → SKU 默认价

### 3. Points Batch Expiry (FIFO)
积分不是一个总数，而是分批管理：
- 每次赚积分 → 新建一个 batch（带过期时间）
- 使用积分 → 按过期时间 ASC 排序，先到期的先扣
- 过期处理 → 每日跑批，到期 batch 标记 EXPIRED

### 4. Inventory Batch Tracking (FIFO)
原料同样分批管理：
- 每次进货 → 新建 inventory_batch（带保质期）
- 销售扣减 → 按 expiry_date ASC 排序，先过期的先扣
- 过期预警 → 提前 N 天提醒

### 5. Channel Attribution
每笔订单追踪来源渠道：
- 自动归因（URL 参数、平台直连、券码核销）
- 手动归因（收银员选择）
- 多渠道归因（一笔单可以有多个渠道）
- 分润自动计算 → 月度结算

### 6. RBAC with Merchant Customization
角色和权限不是硬编码的：
- 系统预设 10 个角色（不可删）
- 商户可以自建角色
- 每个角色可以自由组合 40+ 个权限积木
- 门店访问范围独立控制

### 7. Unified User Table
auth_users + staff 合并为 users：
- 一个人一个账号
- username + password → 后台登录
- PIN → POS 终端快速切换
- 关联 employee（人事信息）

---

## Naming Conventions

| Pattern | Example | Rule |
|---------|---------|------|
| Table names | `store_tables` | snake_case, 复数 |
| Column names | `merchant_id` | snake_case |
| Primary key | `id` | BIGINT AUTO_INCREMENT |
| Business key | `order_no`, `settlement_no` | VARCHAR(64), UNIQUE |
| Foreign key | `fk_{table}_{ref}` | 命名格式 |
| Unique constraint | `uk_{table}_{cols}` | 命名格式 |
| Index | `idx_{table}_{cols}` | 命名格式 |
| Status fields | `*_status` | VARCHAR(32) |
| Money fields | `*_cents` | BIGINT (分为单位) |
| JSON fields | `*_json` | JSON type |
| Timestamps | `created_at`, `updated_at` | TIMESTAMP |

---

## Money Handling

**所有金额字段使用 BIGINT，单位为"分"（cents）。**

- `1000` = $10.00
- `50000` = $500.00
- 前端显示时 / 100
- 不使用 DECIMAL 避免精度问题

---

## Migration Strategy

### Existing tables (48)
通过 Flyway V001-V065 管理，已在生产运行。

### New tables (71)
将在后续 Flyway migration 中按模块逐步添加：

| Migration Range | Module |
|----------------|--------|
| V070-V079 | Time-slot menu + Buffet |
| V080-V089 | Kitchen KDS |
| V090-V099 | Inventory (complete) |
| V100-V109 | CRM (coupons, referrals, tags, marketing) |
| V110-V119 | Points & Stored Value |
| V120-V129 | RBAC (users, roles, permissions) |
| V130-V139 | Employees (HR, scheduling, payroll) |
| V140-V149 | Channels (attribution, commission) |
| V150-V159 | Recommendations + Queue |
| V160-V169 | Delivery platform integration |

### ALTER TABLE statements
所有对现有表的改动合并到对应模块的 migration 中。

---

## Related Documents

| Doc | Content |
|-----|---------|
| `66-aurora-data-model-design.md` | Core new tables: buffet, KDS, inventory, recommendations, delivery, queue |
| `67-sku-centric-data-model.md` | SKU three-layer model, modifiers, channel configs, FAQ |
| `68-rbac-data-model.md` | Unified users, customizable roles, fine-grained permissions |
| `69-employee-management-data-model.md` | HR lifecycle: hire, schedule, attend, payroll, perform |
| `70-inventory-complete-data-model.md` | Batches, POs, stocktake, waste, transfers |
| `71-crm-complete-data-model.md` | Coupons, referrals, tags, profiles, marketing |
| `72-points-and-balance-data-model.md` | Points rules/batches/expiry + stored value ledger |
| `73-channel-attribution-and-commission-data-model.md` | Channel tracking, commission, settlement |
| `74-mrd-v3.md` | Market Requirements Document V3 |
| `75-complete-database-schema.md` | Full DDL for all 119 tables |
