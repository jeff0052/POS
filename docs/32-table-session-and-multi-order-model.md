# Table Session and Multi-Order Model

## Purpose

本文件定义餐饮 POS 的正式桌台交易模型：

- 一张桌不是只有一张提交订单
- 一张桌在营业过程中对应一个 `Table Session`
- 同一张桌可以在该 session 下产生多张已送厨订单
- 最终由 cashier 统一结账

这个模型用于替代“所有内容都塞进一张 active order”的简化思路。

## Core Principle

### One Table Session, Multiple Submitted Orders

一张桌在一次营业过程中：

- 只有一个打开中的 `Table Session`
- 可以有一个当前 `Draft Order`
- 可以有多张已送厨的 `Submitted Orders`
- Payment 时汇总该桌所有未结账的已提交订单

## Why This Model Is Better

相比“一张活动单里混合 draft 和 submitted item”，这个模型更符合真实餐饮流程：

1. 每次 `Send to kitchen` 都形成一个独立提交单
2. 已送厨订单天然锁定，不按草稿逻辑修改
3. 客人加菜时，只需要创建下一张新订单
4. 厨房端天然按轮次接单
5. 结账时更容易汇总和审计

## Core Objects

### 1. Table Session

表示一张桌当前打开中的就餐会话。

主要字段建议：

- `table_session_id`
- `store_id`
- `table_id`
- `session_status`
- `opened_at`
- `closed_at`
- `opened_by_staff_id`
- `guest_count`

### 2. Draft Order

表示当前尚未送厨的草稿单。

特点：

- 可增删改
- 不进入厨房
- 不进入 payment

### 3. Submitted Order

表示一次已经发往厨房的订单轮次。

特点：

- 由 `Send to kitchen` 生成
- 发出后锁定
- 进入厨房履约
- 进入结账汇总

### 4. Settlement

表示 cashier 对该桌当前 session 的统一收款行为。

特点：

- 汇总所有未结账 submitted orders
- draft order 不进入本次 settlement

## State Model

### Table Session Status

- `OPEN`
- `PAYMENT_PENDING`
- `SETTLED`
- `CLOSED`

### Draft Order Status

- `DRAFT`
- `DISCARDED`

### Submitted Order Status

- `SUBMITTED`
- `PREPARING`
- `READY`
- `SERVED`
- `SETTLED`
- `VOIDED`

## User Flow

### POS Ordering Flow

1. 选择桌台
2. 创建或打开当前 `Table Session`
3. 在 `Draft Order` 中点菜
4. 点击 `Send to kitchen`
5. 系统生成一张新的 `Submitted Order`
6. 清空当前 draft
7. 如需加菜，继续在新的 draft 中点菜
8. 再次 `Send to kitchen`
9. 最后进入 payment，统一结账该桌所有未结账 submitted orders

### QR Ordering Flow

1. 顾客扫码进入该桌
2. 系统打开当前 `Table Session`
3. 顾客提交菜品
4. 系统直接生成一张新的 `Submitted Order`
5. 厨房可立即接单
6. cashier 最终统一结账

## Payment Rule

Payment 只汇总：

- 当前 `Table Session`
- 所有 `Submitted / Preparing / Ready / Served`
- 且尚未 `SETTLED` 的订单

Payment 不应包含：

- 当前 `Draft Order`
- 已 `VOIDED`
- 已 `SETTLED`

## Kitchen Rule

厨房侧只接收：

- `Submitted Orders`

厨房不应看到：

- Draft Order

## UI Implications

### Ordering Page

应显示两部分：

- `Current Draft`
- `Sent to Kitchen`

### Kitchen Display

应按 `Submitted Order` 展示，不按桌台草稿展示。

### Payment Page

应汇总当前桌 session 下的：

- 已提交订单列表
- 金额汇总
- 会员优惠
- 促销优惠

## Recommended Backend Evolution

V2 后端建议从当前的 `active_table_orders` 模型演进为：

### Table Session Domain

- `table_sessions`

### Order Domain

- `draft_orders`
- `submitted_orders`
- `submitted_order_items`

### Settlement Domain

- `settlement_records`
- `settlement_order_links`

## Migration Strategy

不要一次性推翻当前 V2。

建议分三步演进：

1. 保留现有 active order 逻辑继续可用
2. 新增 `table_session + submitted_order` 设计文档和新表
3. 先让 `Send to kitchen` 生成 submitted order，再逐步收掉旧 draft/order 混合逻辑

## Acceptance

该模型成立的最低标准：

1. 一张桌可以连续送出多张厨房订单
2. 已送厨订单不会被后续加菜覆盖
3. 顾客 QR 下单会形成新的 submitted order
4. Payment 统一汇总该桌所有未结账 submitted orders
5. Draft 不进入 payment
