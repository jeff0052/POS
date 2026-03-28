# Session 2 Handoff

**Date:** 2026-03-28
**From:** Claude Session 1
**Version:** V20260328016

---

## Source of Truth

- **本地代码：** `/Users/ontanetwork/Documents/Codex/`
- **分支：** `codex/reservations-transfer-backend`
- **GitHub：** `jeff0052/POS` main 分支（已同步 @ 58cac42）
- **AWS：** 54.237.230.5（跑旧版本，手动更新）

---

## 当前进度（Step 0: Journey-First 设计）

```
✅ 0.1 写 12 条 user journey
✅ 0.2 从 journey 反推状态机、约束、跨模块边界
✅ 0.3 补数据模型遗漏 → docs/80（9 新表 + 8 ALTER）
✅ 0.4 统一 review DDL → docs/82（3 BUG + agent review 11 项修复）
✅ 0.5 生成 Flyway migrations → 21 个 SQL 文件（V066-V101）
✅ 0.6 验证 migration 可执行 → 本地 MySQL 128 表全部通过
✅ 0.7 Code Review 修复 → 4 项（V099 删除、QR 统一、API 契约对齐、本文档更新）
```

**最终数字:**
- 物理表: 128（不含 flyway_schema_history）
- Migration 文件: V001-V101（其中 V099 已删除）
- V066: 74 个 CREATE TABLE（docs 66-73 追赶）
- V067: 现有表 ALTER（buffet/CRM/channel 字段）
- V070-V101: Step 0.3 gap 修复（9 新表 + ALTER）

---

## 下个 Session 第一件事：Figma 画图

用 Figma MCP 工具画 25 张图：

### A. 12 条 User Journey 流程图（含异常分支）
源文件：`docs/superpowers/specs/2026-03-28-user-journeys.md`

1. J01 顾客单点堂食
2. J02 顾客自助餐
3. J03 顾客外卖
4. J04 顾客会员全流程
5. J05 收银员日常
6. J06 厨房日常
7. J07 店长日常
8. J08 库存全链路
9. J09 老板视角
10. J10 财务月结
11. J11 并台
12. J12 预约→入座

### B. 12 个状态机流转图
源文件：`docs/superpowers/specs/2026-03-28-state-machines-and-constraints.md`

1. store_tables.table_status
2. table_sessions.session_status（含 buffet_status）
3. active_table_orders.status
4. submitted_orders.settlement_status
5. kitchen_tickets.ticket_status
6. cashier_shifts.shift_status
7. member_coupons.coupon_status
8. member_accounts 冻结机制
9. settlement_records.final_status
10. inventory_batches 生命周期
11. reservations.reservation_status
12. queue_tickets.ticket_status

### C. 1 张系统架构图
5 大系统 + 22 子系统 + 模块间依赖

---

## 画完图后继续

### 0.3 补数据模型遗漏

基于 journey 和状态机发现的 gap，对照现有 DDL（docs/75-complete-database-schema.md）补缺。

已知遗漏（从 journey 中发现）：
- `settlement_payment_holds` 表（冻结/确认/释放）— J04
- `member_accounts` 加 `frozen_points` + `frozen_cash_cents` 字段 — J04
- `member_coupons` 加 `lock_version` 字段（CAS）— J04
- `store_tables` 加 `PENDING_CLEAN` 状态 — J01
- `table_sessions` 加 `buffet_started_at` / `buffet_ends_at` / `buffet_status` — J02
- `submitted_order_items` 加 buffet 字段 — J02
- `kitchen_stations` 加 `fallback_printer_ip` / `kds_health_status` — J06
- `payment_attempts` 加 `attempt_status: REPLACED` — J05
- QR token 表 — J01
- `customer_feedback` 表 — J01
- `external_integration_logs` 表 — J03

### 0.4 统一 review DDL
### 0.5 生成 Flyway migrations

---

## 然后开始写代码（Step 1-8）

```
Step 1: 组织底座（RBAC + 统一用户）— 3-4天
Step 2: 交易核心（预约→订单→结账→退款）— 修补2天
Step 3: 商品升级（SKU三层 + 修饰符 + 多价格 + 时段菜单 + 自助餐）— 5-7天
Step 4: 厨房出品（KDS + 工作站路由）— 3-4天
Step 5: 库存管理（原料 + SOP + 采购 + 盘点）— 5-7天
Step 6: 客户营销（积分 + 储值 + 券 + 推荐 + 标签 + 触达）— 7-10天
Step 7: 渠道与外卖（归因 + 分润 + 外卖对接 + 促销增强）— 5-7天
Step 8: AI层（MCP接真数据 + AI顾问接LLM + Agent）— 5-7天
```

---

## User Journey 写法规则（Jeff 确认的标准）

1. **一步一动作**：API 调用 / 异步事件 / 定时任务，三选一
2. **状态值写真实字段值**：`store_tables.table_status: AVAILABLE → OCCUPIED`，不写业务口语
3. **API 写最终契约**：不带回旧接口名
4. **前端本地行为**只在无 API / 无状态变更时合并成一句
5. **主流程 8-15 步**，异常流单独列
6. 每步固定 6 列：Step / 触发 / API-Event / 状态变更 / 返回-副作用 / 异常

---

## 关键文档位置

| 文档 | 路径 |
|------|------|
| 12 条 User Journey | `docs/superpowers/specs/2026-03-28-user-journeys.md` |
| 状态机 + 约束 + 模块边界 | `docs/superpowers/specs/2026-03-28-state-machines-and-constraints.md` |
| 完整数据 Schema（119表） | `docs/75-complete-database-schema.md` |
| Schema README | `docs/76-database-schema-readme.md` |
| MRD V3 | `docs/74-mrd-v3.md` |
| PRD V3 | `docs/77-updated-prd-v3.md` |
| 客户原始需求 | `/Users/ontanetwork/Documents/小航需求/65-aurora-restaurant-pos-detailed-prd.md` |
| **系统架构总纲（唯一入口）** | **`docs/83-system-architecture-v3.md`** |
| 系统架构（旧版，已被 83 替代） | `docs/78-next-session-handoff.md` |
| 支付外包交接 | `docs/55-payment-service-handoff-for-external-team.md` |

---

## 项目架构（5大系统 22子系统 119张表）

```
一、交易核心 (13 tables)
 ├── 1.1 预约与候位 (2)
 ├── 1.2 订单引擎 (6)
 └── 1.3 结算与退款 (3) + Payment Holds

二、商品与供应 (43 tables)
 ├── 2.1 商品目录 (12)
 ├── 2.2 菜单控制 (4)
 ├── 2.3 厨房出品 (3)
 ├── 2.4 库存管理 (16)
 ├── 2.5 推荐系统 (2)
 └── 2.6 外卖对接 (2)

三、客户与营销 (33 tables)
 ├── 3.1-3.9 会员/积分/储值/券/裂变/运营/触达/渠道/促销

四、组织与运营 (25 tables)
 ├── 4.1-4.5 商户/权限/员工/班次/税务

五、AI 层 (9 tables)
 ├── 5.1-5.4 MCP/顾问/Agent/资产
```
