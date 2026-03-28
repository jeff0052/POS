# FounderPOS V3 — State Machines, Constraints & Module Boundaries

**Version:** V20260328016
**Date:** 2026-03-28
**Status:** DRAFT
**源自:** 12 条 User Journey 反推

---

## 1. 核心状态机

### 1.1 store_tables.table_status

```
AVAILABLE → OCCUPIED → PENDING_SETTLEMENT → PENDING_CLEAN → AVAILABLE
    │           │              │
    │           │              └→ (退款全部) → OCCUPIED (重新打开)
    │           └→ RESERVED (预约到达前)
    └→ RESERVED → OCCUPIED (预约入座)
    └→ DISABLED (维修/停用)
```

**触发条件：**
| 从 | 到 | 触发 | Journey |
|---|---|---|---|
| AVAILABLE | OCCUPIED | 开台/扫码首单 | J01, J02 |
| AVAILABLE | RESERVED | 创建预约 | J12 |
| RESERVED | OCCUPIED | 预约入座 | J12 |
| OCCUPIED | PENDING_SETTLEMENT | 发起结账 | J01, J02, J05 |
| PENDING_SETTLEMENT | PENDING_CLEAN | 支付完成 | J01, J05 |
| PENDING_CLEAN | AVAILABLE | 服务员清台 | J01 |
| OCCUPIED | OCCUPIED | 并台（目标桌） | J11 |

**约束：**
- OCCUPIED 桌不能再被预约
- RESERVED 到时未到 → 超时释放回 AVAILABLE
- PENDING_CLEAN 不接受新订单

---

### 1.2 table_sessions.session_status

```
OPEN → CLOSED
```

**触发条件：**
| 从 | 到 | 触发 | Journey |
|---|---|---|---|
| (created) | OPEN | 开台 | J01, J02, J12 |
| OPEN | CLOSED | 结账完成 | J01, J02, J05 |

**约束：**
- 一张桌同时只能有一个 OPEN session
- CLOSED 不可重开（新开一个 session）
- 并台时源桌 session CLOSED，目标桌 session 保持 OPEN 并合并数据

**字段扩展（自助餐）：**
```
dining_mode: A_LA_CARTE | BUFFET | MIXED
buffet_status: ACTIVE → WARNING → OVERTIME → ENDED
```

---

### 1.3 active_table_orders.status

```
DRAFT → SUBMITTED → PENDING_SETTLEMENT → (deleted after settlement)
```

**触发条件：**
| 从 | 到 | 触发 | Journey |
|---|---|---|---|
| (created) | DRAFT | POS 开始点单 | J01, J05 |
| DRAFT | SUBMITTED | 送厨 | J01, J05 |
| SUBMITTED | PENDING_SETTLEMENT | 发起结账 | J01, J05 |
| PENDING_SETTLEMENT | (deleted) | 结账完成 | J01, J05 |

**约束：**
- 一张桌同一 session 只有一个 active_table_order
- 追加点单不创建新 active_order，而是追加 items

---

### 1.4 submitted_orders.settlement_status

```
UNPAID → SETTLED
UNPAID → CANCELLED (退单)
SETTLED → PARTIAL_REFUND → FULL_REFUND
```

**触发条件：**
| 从 | 到 | 触发 | Journey |
|---|---|---|---|
| (created) | UNPAID | 送厨 / 外卖接单 | J01, J03 |
| UNPAID | SETTLED | 结账 | J01, J05 |
| UNPAID | CANCELLED | 整单取消 | J06 (退单) |
| SETTLED | PARTIAL_REFUND | 部分退款 | J05 |
| SETTLED | FULL_REFUND | 全额退款 | J05 |

**约束：**
- CANCELLED 的订单不参与结账金额计算
- 退款金额不能超过已结算金额（悲观锁）

---

### 1.5 kitchen_tickets.ticket_status

```
SUBMITTED → PREPARING → READY → SERVED
    │           │
    └→ CANCELLED └→ CANCELLED (退单)
```

**触发条件：**
| 从 | 到 | 触发 | Journey |
|---|---|---|---|
| (created) | SUBMITTED | submitted_order 创建时按 station_id 拆票 | J01, J06 |
| SUBMITTED | PREPARING | 厨师点"开始" | J06 |
| PREPARING | READY | 厨师点"完成" | J06 |
| READY | SERVED | 服务员确认上菜 | J06 |
| SUBMITTED/PREPARING | CANCELLED | 退单 | J06 |

**约束：**
- READY 和 SERVED 不可取消
- CANCELLED 时如果库存已扣减 → 触发回补
- 一个 submitted_order 可拆成多个 kitchen_ticket（按 station_id）

---

### 1.6 cashier_shifts.shift_status

```
OPEN → CLOSED
```

**触发条件：**
| 从 | 到 | 触发 | Journey |
|---|---|---|---|
| (created) | OPEN | 收银员开班 | J05 |
| OPEN | CLOSED | 收银员关班 | J05 |

**约束：**
- 同一收银员同一门店只能有一个 OPEN shift
- 关班时必须录入实际现金金额
- 差异超阈值 → 告警

---

### 1.7 member_coupons.coupon_status

```
AVAILABLE → LOCKED → USED
    │          │
    │          └→ AVAILABLE (释放/超时)
    └→ EXPIRED (定时任务)
    └→ CANCELLED (手动作废)
```

**触发条件：**
| 从 | 到 | 触发 | Journey |
|---|---|---|---|
| (created) | AVAILABLE | 发券/领券 | J04 |
| AVAILABLE | LOCKED | 结账预览锁定 | J04 |
| LOCKED | USED | 结账确认 | J04 |
| LOCKED | AVAILABLE | 结账取消/超时释放 | J04 |
| AVAILABLE | EXPIRED | 过期任务 | J04 |

**约束：**
- LOCKED 使用 CAS（乐观锁 lock_version）防并发
- LOCKED 超过 15 分钟自动释放回 AVAILABLE
- USED 不可逆

---

### 1.8 member_accounts 冻结机制

```
结账预览:
  frozen_points += X
  frozen_cash_cents += Y

结账确认:
  points_balance -= X, frozen_points -= X
  cash_balance_cents -= Y, frozen_cash_cents -= Y

结账取消:
  frozen_points -= X (释放)
  frozen_cash_cents -= Y (释放)
```

**约束：**
- 可用积分 = points_balance - frozen_points
- 可用储值 = cash_balance_cents - frozen_cash_cents
- 冻结金额不能超过可用金额
- 超过 15 分钟未确认 → 自动释放

---

### 1.9 settlement_records.final_status

```
SETTLED → PARTIAL_REFUND → FULL_REFUND
```

**约束：**
- 退款总额不能超过 collectedAmountCents（悲观锁 FOR UPDATE）
- 退款需要权限校验：小额 CASHIER 可退，大额需 MANAGER 审批

---

### 1.10 inventory_batches 生命周期

```
(created on purchase) → 逐步扣减 remaining_qty → 归零 (用完)
                                                → 过期 (expiry_date 到)
                                                → 报损 (waste)
```

**约束：**
- FIFO：扣减时按 expiry_date ASC 优先扣最早的批次
- remaining_qty 不能为负
- 过期批次不参与正常扣减，标记后走报损流程

---

### 1.11 reservations.reservation_status

```
CONFIRMED → SEATED → COMPLETED
    │
    ├→ CANCELLED (客户取消)
    ├→ NO_SHOW (超时未到)
    └→ WAIT_LISTED → CONFIRMED (候位转确认)
```

**触发条件：**
| 从 | 到 | 触发 | Journey |
|---|---|---|---|
| (created) | CONFIRMED | 预约成功 | J12 |
| CONFIRMED | SEATED | 入座开台 | J12 |
| SEATED | COMPLETED | 结账离开 | J12 |
| CONFIRMED | CANCELLED | 取消 | J12 |
| CONFIRMED | NO_SHOW | 超时 | J12 |

---

### 1.12 queue_tickets.ticket_status

```
WAITING → CALLED → SEATED
    │        │
    │        └→ EXPIRED (叫号超时)
    └→ CANCELLED (离开)
```

---

## 2. 跨模块调用边界

### 2.1 模块间依赖图

```
                 ┌─────────────┐
                 │   AI 层     │
                 │ MCP/Advisor │
                 └──────┬──────┘
                        │ 调用所有模块的 Read/Write Tools
         ┌──────────────┼──────────────┐
         ▼              ▼              ▼
  ┌─────────────┐ ┌──────────┐ ┌──────────────┐
  │客户与营销    │ │组织与运营 │ │ 商品与供应    │
  │Member       │ │Auth/RBAC │ │ Catalog      │
  │Points       │ │Employee  │ │ Menu         │
  │Coupon       │ │Shift     │ │ Kitchen      │
  │Channel      │ │GTO       │ │ Inventory    │
  │Promotion    │ │          │ │ Delivery     │
  └──────┬──────┘ └─────┬────┘ └──────┬───────┘
         │              │              │
         └──────────────┼──────────────┘
                        ▼
               ┌─────────────────┐
               │   交易核心       │
               │ Reservation     │
               │ Order           │
               │ Settlement      │
               └─────────────────┘
```

### 2.2 跨模块调用规则

| 调用方 | 被调用方 | 调用方式 | 原因 |
|--------|---------|---------|------|
| Order → Catalog | 读 SKU 价格 | 同步读 | 下单时服务端定价 |
| Order → Kitchen | 创建 ticket | 同步写 | 送厨 |
| Order → Member | 读会员信息 | 同步读 | 会员绑定 |
| Settlement → Order | 读 submitted_orders | 同步读 | 结账汇总 |
| Settlement → Member | 扣积分/储值 | 同步写 | 支付叠加 |
| Settlement → Coupon | 锁定/使用券 | 同步写(CAS) | 用券 |
| Settlement → Inventory | SOP 扣减 | 同步写 | 结账触发库存扣减 |
| Settlement → Shift | 记录流水 | 同步写 | 班次对账 |
| Settlement → Channel | 记录归因 | 同步写 | 渠道分润 |
| Kitchen → Inventory | 退单回补 | 同步写 | 退单库存 |
| Inventory → Promotion | 生成促销草案 | 异步(定时) | 临期促销 |
| Report → 所有模块 | 读数据 | 只读 | 报表查询 |

### 2.3 关键规则

1. **所有跨模块写操作必须在同一事务中**（@Transactional）
2. **模块间不直接操作对方的 Entity**，通过 Service 方法调用
3. **报表模块只读**，不写任何业务表
4. **AI 层通过 MCP Tools 调用**，不直接访问 Repository

---

## 3. 并发/幂等/一致性约束

### 3.1 并发控制

| 场景 | 控制方式 | Journey |
|------|---------|---------|
| 同桌并发开台 | 悲观锁 `SELECT ... FOR UPDATE` on store_tables | J01 |
| 同桌并发结账 | 悲观锁 on table_sessions | J05 |
| 退款并发超额 | 悲观锁 on settlement_records | J05 |
| 券并发抢用 | 乐观锁 CAS `lock_version` | J04 |
| 库存并发扣减 | 悲观锁 on inventory_batches (FIFO) | J08 |
| 冻结余额并发 | 悲观锁 on member_accounts | J04 |

### 3.2 幂等性

| 操作 | 幂等键 | 说明 |
|------|--------|------|
| 结账 | session_id + settlement_no | 同一 session 不能重复结账 |
| 退款 | settlement_id + refund_no | 唯一约束防重复 |
| 外卖接单 | external_order_no | 同一外卖订单不重复创建 |
| 支付回调 | provider_transaction_id | 同一支付通知只处理一次 |
| 充值 | recharge_order_no | 唯一约束 |

### 3.3 数据一致性

| 约束 | 实现 | Journey |
|------|------|---------|
| 结账金额 = sum(UNPAID submitted_orders) | 服务端计算，不信任客户端 | J01, J05 |
| 退款总额 ≤ 结账金额 | DB CHECK + 应用层双重校验 | J05 |
| 库存不为负 | remaining_qty >= 0 CHECK 约束 | J08 |
| 冻结 ≤ 可用余额 | 应用层校验 + 悲观锁 | J04 |
| 会员等级只升不降 | 应用层 tierRank(new) > tierRank(current) | J04 |
| 积分过期 FIFO | 按 batch expiry_date ASC 扣减 | J04 |
| 自助餐超时费 = (超出分钟 - 宽限) × 单价 | 服务端计算 | J02 |
| 渠道分润 = 营收 × 佣金率 | 服务端计算，定时任务汇总 | J03 |

---

## 4. 全局设计决策

### 4.1 价格权威

- **服务端是唯一价格权威**。所有价格从 `sku_price_overrides` → `skus.base_price_cents` 链路计算
- 客户端提交的价格字段被忽略
- 修饰符加价在服务端查 `sku_modifier_groups` + `sku_modifier_options` 计算

### 4.2 审计链路

- 所有写操作记录 `audit_trail`（actor, action, risk_level, before/after snapshot）
- 高风险操作需要审批（退款>阈值、改价、删菜品）
- AI 操作标记 `actor_type=AI`，人工操作标记 `actor_type=HUMAN`

### 4.3 支付叠加顺序

```
1. 促销折扣（自动命中最优）
2. 优惠券（手动选择）
3. 积分抵扣（按比例，如 100 积分 = 1 元）
4. 储值余额
5. 外部支付（现金/卡/QR）
```

每一步都走冻结→确认模式，支付失败可以释放前面的冻结。

### 4.4 时区

- 所有服务端时间存 UTC
- 前端显示时转换为门店时区（`stores.timezone`）
- 报表按门店时区聚合
