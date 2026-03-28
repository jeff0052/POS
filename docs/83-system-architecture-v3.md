# FounderPOS V3 — 架构概览

**Version:** V20260328022
**Date:** 2026-03-28
**Status:** FINAL
**定位：** 架构全貌概览，帮你快速理解系统结构。**不是实现基线**——口径冲突时以 Layer 1 文档为准（见下方权威层级）。

---

## 0. 产品定位

**一句话：** 一个老板 + 一个 AI 伙伴 = 管好一家餐厅。

FounderPOS 不是传统 POS，是 **AI 驱动的餐厅操作系统**。四层架构：

```
┌─────────────────────────────┐
│  L4  Agent 身份层            │ 每家店 = 一个 AI Agent，有钱包、有人格
├─────────────────────────────┤
│  L3  AI 运营层              │ 库存顾问、营销顾问、运营顾问，自动提建议
├─────────────────────────────┤
│  L2  MCP 工具层             │ 40+ 工具，AI 读写真实业务数据
├─────────────────────────────┤
│  L1  交易底座               │ 点单、结账、库存、会员、厨房——传统 POS 功能
└─────────────────────────────┘
```

> 详细愿景 → `docs/74-mrd-v3.md`
> 产品需求 → `docs/77-updated-prd-v3.md`
> 客户原始需求 → `docs/65-aurora-restaurant-pos-detailed-prd.md`

---

## 1. 五大系统总览

```
125 表（框架内）+ 4 Legacy = 129 物理表

一、交易核心 (14)     ── 钱从哪来
二、商品与供应 (39)    ── 卖什么、怎么做、原料够不够
三、客户与营销 (34)    ── 谁在买、怎么让他再来
四、组织与运营 (28)    ── 谁在干活、怎么管
五、AI 层 (10)        ── AI 怎么帮老板
```

> 逐表对账 → `docs/81-table-reconciliation.md`

---

## 2. 一、交易核心（14 表）

**职责：** 从客人进门到付钱走人的全链路。

### 2.1 预约与候位 Reservations（2 表）

| 表 | 说明 |
|----|------|
| reservations | 预约记录，含日期/人数/来源/状态 |
| queue_tickets | 候位票，含叫号次数/过号追踪 |

**状态机：**
- `reservations`: CONFIRMED → SEATED → COMPLETED / NO_SHOW / CANCELLED
- `queue_tickets`: WAITING → CALLED → SEATED / SKIPPED / CANCELLED

**Journey：** J12 预约→入座

### 2.2 订单引擎 Orders（8 表）

| 表 | 说明 |
|----|------|
| table_sessions | 桌台会话（一次开台到结账） |
| active_table_orders | 当前活动订单（结账后删除） |
| active_table_order_items | 活动订单行项 |
| order_events | 订单事件日志 |
| submitted_orders | 已提交订单（持久化，含结算状态） |
| submitted_order_items | 提交订单行项（含 buffet 字段） |
| qr_tokens | 🆕 扫码令牌 |
| table_merge_records | 🆕 并台记录 |

**状态机：**
- `table_sessions`: OPEN → CLOSED
- `active_table_orders`: DRAFT → SUBMITTED → PENDING_SETTLEMENT → (deleted)
- `submitted_orders`: UNPAID → SETTLED / CANCELLED; SETTLED → PARTIAL_REFUND → FULL_REFUND

**Journey：** J01 单点堂食, J02 自助餐, J05 收银员, J11 并台

### 2.3 结算与退款 Settlement（4 表）

| 表 | 说明 |
|----|------|
| settlement_records | 结账记录（金额、快照） |
| payment_attempts | 支付尝试（含 REPLACED 状态） |
| refund_records | 退款记录 |
| settlement_payment_holds | 🆕 冻结/确认/释放（积分/储值/券） |

**状态机：**
- `settlement_records`: SETTLED → PARTIAL_REFUND → FULL_REFUND
- `settlement_payment_holds`: HELD → CONFIRMED / RELEASED

**Journey：** J04 会员支付叠加, J05 收银员

**关键设计决策：**
- 服务端是唯一价格权威，客户端价格被忽略
- 支付叠加顺序：促销 → 券 → 积分 → 储值 → 外部支付
- 每步走冻结→确认模式

> 详细 → `docs/superpowers/specs/2026-03-28-final-executable-spec.md` D2-D3

---

## 3. 二、商品与供应（39 表）

**职责：** 菜单展示、厨房出品、原料管理、外部对接。

### 3.1 商品目录 Catalog（10 表）

| 表 | 说明 |
|----|------|
| product_categories | 分类 |
| products | 商品（SPU 级） |
| skus | SKU（最小售卖单元，含价格/工作站/配方） |
| store_sku_availability | 门店 SKU 上下架 |
| sku_price_overrides | 价格覆盖（时段/渠道/门店） |
| sku_channel_configs | 渠道特定配置 |
| sku_faq | SKU FAQ |
| modifier_groups | 修饰符组（辣度/规格/加料） |
| modifier_options | 修饰符选项 |
| sku_modifier_group_bindings | SKU↔修饰符绑定 |

**核心模型：** Category → Product → SKU → Modifier（三层 + 修饰符）

### 3.2 菜单控制 Menu（4 表）

| 表 | 说明 |
|----|------|
| menu_time_slots | 时段菜单（午市/晚市） |
| menu_time_slot_products | 时段↔商品绑定 |
| buffet_packages | 自助餐档位 |
| buffet_package_items | 档位↔SKU 绑定（含差价） |

### 3.3 厨房出品 Kitchen（3 表）

| 表 | 说明 |
|----|------|
| kitchen_stations | 工作站（含 fallback 打印机 + KDS 健康状态） |
| kitchen_tickets | 厨房票（按 station 拆分） |
| kitchen_ticket_items | 厨房票行项 |

**状态机：** `kitchen_tickets`: SUBMITTED → PREPARING → READY → SERVED / CANCELLED

**Journey：** J06 厨房日常

### 3.4 库存管理 Inventory（17 表）

| 表 | 说明 |
|----|------|
| inventory_items | 原料主数据 |
| inventory_batches | 批次（FIFO，含保质期） |
| recipes | SOP 配方（含修饰符消耗规则） |
| purchase_invoices | 送货单（含 OCR） |
| purchase_invoice_items | 送货单行项 |
| purchase_orders | 采购单 |
| purchase_order_items | 采购单行项 |
| inventory_movements | 库存流水 |
| suppliers | 供应商 |
| supplier_price_history | 供应商历史价格 |
| order_suggestions | 智能补货建议 |
| stocktake_tasks | 盘点任务 |
| stocktake_items | 盘点行项 |
| waste_records | 报损记录 |
| inventory_transfers | 跨店调拨 |
| inventory_transfer_items | 调拨行项 |
| sop_import_batches | 🆕 SOP 批量导入 |

**Journey：** J08 库存全链路

### 3.5 推荐系统 Recommendations（2 表）

| 表 | 说明 |
|----|------|
| recommendation_slots | 推荐位 |
| recommendation_slot_items | 推荐位↔SKU |

### 3.6 外部对接 Integration（3 表）

| 表 | 说明 |
|----|------|
| delivery_platform_configs | 外卖平台配置 |
| delivery_orders | 外卖订单 |
| external_integration_logs | 🆕 外部接口调用日志 |

**Journey：** J03 外卖, J12 Google 预约同步

---

## 4. 三、客户与营销（34 表）

**职责：** 会员体系、积分储值、券、裂变、渠道、促销。

### 4.1 会员基础 Members（5 表）

| 表 | 说明 |
|----|------|
| members | 会员主表 |
| member_accounts | 账户（积分/储值/冻结余额） |
| member_tier_rules | 等级规则 |
| member_recharge_orders | 充值记录 |
| recharge_campaigns | 充值活动 |

### 4.2 积分体系 Points（5 表）

| 表 | 说明 |
|----|------|
| member_points_ledger | 积分流水 |
| points_rules | 积分规则 |
| points_expiry_rules | 过期规则 |
| points_batches | 积分批次（FIFO 过期） |
| points_deduction_rules | 抵扣规则 |

### 4.3 储值体系 Stored Value（2 表）

| 表 | 说明 |
|----|------|
| member_cash_ledger | 储值流水 |
| cash_balance_rules | 储值规则 |

### 4.4 优惠券 Coupons（2 表）

| 表 | 说明 |
|----|------|
| coupon_templates | 券模板 |
| member_coupons | 会员持有的券（含 CAS lock_version） |

**状态机：** AVAILABLE → LOCKED → USED / AVAILABLE(释放) / EXPIRED

### 4.5 推荐裂变 Referral（2 表）

| 表 | 说明 |
|----|------|
| referral_rewards_config | 裂变奖励配置 |
| referral_records | 推荐记录 |

### 4.6 会员运营 Operations（5 表）

| 表 | 说明 |
|----|------|
| member_tags | 会员标签 |
| member_tag_assignments | 标签↔会员绑定 |
| member_consumption_profiles | 消费画像 |
| points_redemption_items | 积分兑换商品 |
| points_redemption_records | 兑换记录 |

### 4.7 营销触达 Marketing（2 表）

| 表 | 说明 |
|----|------|
| marketing_campaigns | 营销活动 |
| marketing_send_records | 发送记录 |

### 4.8 渠道分润 Channels（6 表）

| 表 | 说明 |
|----|------|
| channels | 渠道主表 |
| channel_commission_rules | 佣金规则 |
| order_channel_attribution | 订单渠道归因 |
| channel_commission_records | 佣金明细 |
| channel_settlement_batches | 渠道结算批次 |
| channel_performance_daily | 渠道日报 |

### 4.9 促销引擎 Promotions（5 表）

| 表 | 说明 |
|----|------|
| promotion_rules | 促销规则 |
| promotion_rule_conditions | 触发条件 |
| promotion_rule_rewards | 奖励 |
| promotion_hits | 命中记录 |
| inventory_driven_promotions | 🆕 库存驱动促销草案 |

**Journey：** J04 会员全流程, J09 老板视角

---

## 5. 四、组织与运营（28 表）

**职责：** 商户/门店配置、权限、员工、班次、审计。

### 5.1 商户体系 Core（7 表）

| 表 | 说明 |
|----|------|
| merchants | 商户 |
| merchant_configs | 商户配置 |
| brands | 品牌 |
| brand_countries | 品牌↔国家 |
| stores | 门店 |
| store_tables | 桌台（含状态机） |
| store_terminals | 终端设备 |

**状态机 `store_tables`：** AVAILABLE → OCCUPIED → PENDING_SETTLEMENT → PENDING_CLEAN → AVAILABLE; 也可 AVAILABLE → RESERVED → OCCUPIED; 并台时 → MERGED

### 5.2 权限体系 RBAC（6 表）

| 表 | 说明 |
|----|------|
| users | 统一用户（替代 auth_users + staff） |
| permissions | 权限定义 |
| custom_roles | 自定义角色 |
| custom_role_permissions | 角色↔权限 |
| user_roles | 用户↔角色 |
| user_store_access | 用户↔门店权限 |

### 5.3 员工管理 Employees（9 表）

| 表 | 说明 |
|----|------|
| employees | 员工主表 |
| shift_templates | 班次模板 |
| employee_schedules | 排班 |
| attendance_records | 考勤 |
| leave_requests | 请假 |
| leave_balances | 假期余额 |
| payroll_periods | 薪资期间 |
| payroll_records | 薪资明细 |
| employee_performance_log | 绩效日志 |

### 5.4 班次管理 Shifts（2 表）

| 表 | 说明 |
|----|------|
| cashier_shifts | 收银班次 |
| cashier_shift_settlements | 班次结算明细 |

**状态机：** OPEN → CLOSED

**Journey：** J05 收银员日常

### 5.5 税务合规 GTO（2 表）

| 表 | 说明 |
|----|------|
| gto_export_batches | GTO 导出批次 |
| gto_export_items | GTO 导出行项 |

**Journey：** J10 财务月结

### 5.6 审计与反馈 Audit（2 表）🆕

| 表 | 说明 |
|----|------|
| audit_trail | 🆕 审计追踪（含审批流） |
| customer_feedback | 🆕 顾客评价/客诉 |

**Journey：** J05 退款审批, J07 店长处理客诉, J09 老板审批

---

## 6. 五、AI 层（10 表）

**职责：** MCP 工具、AI 顾问建议、Agent 身份、资产。

### 6.1 MCP 工具层（2 表）

| 表 | 说明 |
|----|------|
| action_log | MCP 工具调用日志 |
| ai_scheduled_checks | AI 定时检查配置 |

### 6.2 AI 顾问 Advisor（3 表）

| 表 | 说明 |
|----|------|
| ai_proposal | AI 提案 |
| ai_recommendations | AI 建议（含审批） |
| report_snapshots | 🆕 报表快照 + AI 摘要/亮点/警告/建议 |

**Journey：** J09 老板视角（日报 + AI 摘要）

### 6.3 Agent 身份（4 表）

| 表 | 说明 |
|----|------|
| restaurant_agents | 餐厅 Agent |
| agent_wallets | Agent 钱包 |
| wallet_transactions | 钱包交易 |
| agent_interactions | Agent 交互记录 |

### 6.4 资产管理 Assets（1 表）

| 表 | 说明 |
|----|------|
| image_assets | 图片资产（S3） |

---

## 7. 跨系统设计约束

### 7.1 并发控制

| 场景 | 方式 | Journey |
|------|------|---------|
| 同桌并发开台 | 悲观锁 `SELECT ... FOR UPDATE` on store_tables | J01 |
| 同桌并发结账 | 悲观锁 on table_sessions | J05 |
| 退款并发超额 | 悲观锁 on settlement_records | J05 |
| 券并发抢用 | 乐观锁 CAS `lock_version` | J04 |
| 库存并发扣减 | 悲观锁 on inventory_batches (FIFO) | J08 |
| 冻结余额并发 | 悲观锁 on member_accounts | J04 |

### 7.2 幂等性

| 操作 | 幂等键 |
|------|--------|
| 结账 | session_id + settlement_no |
| 退款 | settlement_id + refund_no |
| 外卖接单 | external_order_no |
| 支付回调 | provider_transaction_id |
| 充值 | recharge_order_no |

### 7.3 全局规则

- **价格权威：** 服务端计算，客户端不可信
- **审计：** 所有写操作记 audit_trail，高风险需审批
- **时区：** DB 存 UTC，前端转门店时区
- **金额：** 全部 `_cents BIGINT`，不用浮点
- **AI 操作：** `actor_type=AI`，人工 `actor_type=HUMAN`

### 7.4 跨模块调用规则

- 所有跨模块写操作在同一事务（@Transactional）
- 模块间通过 Service 方法，不直接操作对方 Entity
- 报表模块只读
- AI 层通过 MCP Tools，不直接访问 Repository

> 详细约束 → `docs/superpowers/specs/2026-03-28-state-machines-and-constraints.md` 第 2-4 节

---

## 8. User Journey 索引

| # | Journey | 主系统 | 涉及系统 |
|---|---------|-------|---------|
| J01 | 顾客单点堂食 | 一 交易 | 二(catalog/kitchen), 四(core) |
| J02 | 顾客自助餐 | 一 交易 | 二(menu/kitchen) |
| J03 | 顾客外卖 | 二 供应 | 一(orders/settlement) |
| J04 | 顾客会员全流程 | 三 营销 | 一(settlement) |
| J05 | 收银员日常 | 四 运营 | 一(orders/settlement) |
| J06 | 厨房日常 | 二 供应 | 一(orders) |
| J07 | 店长日常 | 四 运营 | 二(inventory), 三(promotions) |
| J08 | 库存全链路 | 二 供应 | 三(promotions) |
| J09 | 老板视角 | 五 AI | 四(audit) |
| J10 | 财务月结 | 四 运营 | 三(channels) |
| J11 | 并台 | 一 交易 | 四(core) |
| J12 | 预约→入座 | 一 交易 | 二(integration) |

> 详细 Journey → `docs/superpowers/specs/2026-03-28-user-journeys.md`

---

## 9. 实施计划

### 9.1 Flyway Migration（19 个）

| 编号 | 内容 | 系统 |
|------|------|------|
| V070 | table_sessions 加 merged_into_session_id | 一 |
| V071 | CREATE qr_tokens | 一 |
| V072 | CREATE table_merge_records | 一 |
| V073 | CREATE audit_trail | 四 |
| V074 | order_items 加 buffet 字段 | 一 |
| V076 | CREATE settlement_payment_holds | 一 |
| V081 | member_coupons 加 lock_version | 三 |
| V086 | recipes 加修饰符消耗 | 二 |
| V087 | purchase_invoices 加 ocr_raw_result | 二 |
| V088 | CREATE sop_import_batches | 二 |
| V089 | CREATE inventory_driven_promotions | 三 |
| V091 | CREATE customer_feedback | 四 |
| V092 | CREATE external_integration_logs | 二 |
| V094 | CREATE report_snapshots | 五 |
| V095 | kitchen_stations 加 fallback/health | 二 |
| V096 | reservations 加联系人/来源 | 一 |
| V098 | queue_tickets 加叫号追踪 | 一 |
| V099 | active_table_orders 统一命名 | 一 |
| V101 | 补充缺失索引（8 个） | 全局 |

> DDL 细节 → `docs/80-step-0.3-data-model-gaps.md`
> Review 记录 → `docs/82-step-0.4-ddl-review.md`

### 9.2 Sprint 规划（6 个 Sprint）

| Sprint | 内容 | 预估 |
|--------|------|------|
| S1 | 组织底座（RBAC + 统一用户） | 3-4 天 |
| S2 | 交易核心（预约→订单→结账→退款） | 2 天修补 |
| S3 | 商品升级（SKU 三层 + 修饰符 + 自助餐） | 5-7 天 |
| S4 | 厨房出品 + 库存管理 | 8-11 天 |
| S5 | 客户营销（积分/储值/券/渠道） | 12-17 天 |
| S6 | AI 层 + 报表 + 收尾 | 5-7 天 |

> Sprint 细节 → `docs/superpowers/specs/2026-03-28-sprint-plan-complete.md`
> 可执行 spec → `docs/superpowers/specs/2026-03-28-final-executable-spec.md`

### 9.3 支付外包

支付由外部团队实现，只需实现 VibeCash QR 和 DCS 刷卡两个 adapter。

> 交接文档 → `docs/55-payment-service-handoff-for-external-team.md`

---

## 10. 文档权威层级

```
Layer 0: 导航入口
  └── README.md                              ← 不做决策，只做索引

Layer 1: 实现基线（口径冲突时这层说了算）
  ├── specs/final-executable-spec.md         ← 设计决策 D1-D10
  ├── docs/80-step-0.3-data-model-gaps.md    ← DDL 定义
  └── docs/82-step-0.4-ddl-review.md         ← DDL 审查修复

Layer 2: 架构概览（本文档）
  └── docs/83-system-architecture-v3.md      ← 汇总全貌，与 L1 冲突时以 L1 为准

Layer 3: 设计展开
  ├── specs/user-journeys.md                 ← 12 条 User Journey
  └── specs/state-machines-and-constraints.md ← 12 状态机 + 约束

Layer 4: 执行计划
  ├── docs/84-implementation-plan-and-roadmap.md ← 16 session 执行清单
  └── specs/sprint-plan-complete.md          ← ⚠️ SUPERSEDED，仅参考 Java 类名/API
```

### 文档完整索引

| 类别 | 文档 | 路径 |
|------|------|------|
| **需求** | 客户原始需求 | `docs/65-aurora-restaurant-pos-detailed-prd.md` |
| | MRD V3 | `docs/74-mrd-v3.md` |
| | PRD V3 | `docs/77-updated-prd-v3.md` |
| **数据** | 120 表完整 DDL | `docs/75-complete-database-schema.md` |
| | Schema 导读 | `docs/76-database-schema-readme.md` |
| | 9 新表 + 8 ALTER | `docs/80-step-0.3-data-model-gaps.md` |
| | 125 表对账 | `docs/81-table-reconciliation.md` |
| | DDL Review | `docs/82-step-0.4-ddl-review.md` |
| **设计** | 12 User Journey | `specs/user-journeys.md` |
| | 12 状态机 + 约束 | `specs/state-machines-and-constraints.md` |
| | 可执行 Spec (L1) | `specs/final-executable-spec.md` |
| **规划** | Implementation Plan | `docs/84-implementation-plan-and-roadmap.md` |
| | Sprint 计划 (废弃) | `specs/sprint-plan-complete.md` |
| | Session 交接 | `docs/79-session-2-handoff.md` |
| **外包** | 支付交接 | `docs/55-payment-service-handoff-for-external-team.md` |

---

## Legacy 表（4 张，待迁移）

| 表 | 替代 | 何时迁移 |
|----|------|---------|
| auth_users | → users (#73) | S1 |
| staff | → employees (#79) | S1 |
| roles | → custom_roles (#75) | S1 |
| role_permissions | → custom_role_permissions (#76) | S1 |

---

*本文档是 FounderPOS V3 的唯一入口。写代码前先看这里，再按引用跳转到细节文档。*
