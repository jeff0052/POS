# Terminology and State Model

## 1. Document Purpose

本文件用于统一 Restaurant POS 项目的核心术语、状态名称与页面用语，避免产品、设计、开发、测试在沟通时出现同一个概念多种叫法的问题。

目标是统一三端语言：

- Android POS
- QR Ordering Web
- Merchant Admin

## 2. Core Principle

### 2.1 One Table, One Active Order

同一张桌在同一时刻只允许存在一张**活动订单**。

这张活动订单可以由两种入口编辑：

- 顾客扫码点餐
- cashier / 服务员在 POS 点餐

这两个入口不是两张独立订单，而是同一张桌台活动订单的两个操作入口。

### 2.2 Ordering and Settlement Are Different Steps

必须明确区分：

- 点单
- 下单到厨房
- 待结账
- 已结账

顾客扫码点餐不等于顾客已付款。

## 3. Canonical Terms

### 3.1 Product Terms

- `Table`
  - 桌台
  - 表示餐厅中的一个物理就餐位置

- `Active Table Order`
  - 当前桌活动订单
  - 指某张桌当前正在编辑、制作、待结或尚未结清的唯一订单

- `QR Ordering`
  - 桌码扫码点餐
  - 顾客扫描桌码后进入当前桌菜单进行加菜和提交

- `POS Ordering`
  - POS 点餐
  - cashier 或服务员在收银端为当前桌加菜、改菜、删菜

- `Cashier Settlement`
  - cashier 结账
  - 前台收银员对活动订单执行最终收款

- `Send to Kitchen`
  - 下单到厨房
  - 订单进入厨房制作流转

- `Order Review`
  - 订单复核
  - 对活动订单进行核对、确认优惠、决定后续动作的页面

## 4. Order State Model

订单主状态统一使用以下四个：

### 4.1 `DRAFT`

含义：

- 正在点单中
- 菜品仍可自由编辑
- 还没有正式进入厨房制作流

适用场景：

- cashier 刚开始为某桌点单
- 顾客扫码加菜后暂时还未下发厨房

前端建议文案：

- `Ordering`
- `Draft order`

### 4.2 `SUBMITTED`

含义：

- 已下单到厨房
- 主订单已经进入制作流转
- 仍允许后续追加菜品，但语义上属于“加菜”而不是重新开单

前端建议文案：

- `Sent to kitchen`
- `In kitchen flow`

### 4.3 `PENDING_SETTLEMENT`

含义：

- 订单已经进入待结账阶段
- cashier 应确认优惠与应收金额并完成收款

注意：

- 顾客扫码点餐提交后，如果业务规则是“前台结账”，最终也应进入这一状态

前端建议文案：

- `Pending settlement`
- `Waiting for cashier`

### 4.4 `SETTLED`

含义：

- 订单已完成收款并结清
- 该订单生命周期结束
- 桌台可进入清台或重新开放

前端建议文案：

- `Settled`
- `Paid`

## 5. Table State Model

桌台状态和订单状态分开表达。

桌台状态建议统一为：

### 5.1 `AVAILABLE`

含义：

- 桌台空闲，可重新接待

### 5.2 `ORDERING`

含义：

- 桌台已有活动订单
- 当前仍在点单/改单阶段
- 对应订单通常处于 `DRAFT`

### 5.3 `DINING`

含义：

- 桌台订单已下发厨房
- 顾客正在用餐
- 对应订单通常处于 `SUBMITTED`

### 5.4 `PENDING_SETTLEMENT`

含义：

- 桌台准备结账
- 前台应尽快完成收款
- 对应订单通常处于 `PENDING_SETTLEMENT`

### 5.5 `CLEANING`

含义：

- 订单刚结清
- 桌台待清理和翻台

## 6. Source Terms

订单来源统一使用：

- `POS`
  - 前台 / 服务员 / cashier 创建或编辑

- `QR`
  - 顾客扫码桌码创建或编辑

页面文案建议：

- `POS order`
- `QR table order`

不要混用：

- `scan order`
- `customer order`
- `mobile order`

除非有更具体业务区分。

## 7. Settlement Terms

统一使用：

- `Cashier Settlement`
- `Collect Payment`
- `Payment Completed`

不要在餐饮场景里混用过多不同说法，例如：

- `Checkout`
- `Close bill`
- `Complete payment`
- `Finalize`

可以保留其中一个作为辅助文案，但主按钮和主标题要统一。

建议：

- 页面标题：`Cashier Settlement`
- 主按钮：`Collect Payment`
- 成功页：`Payment Completed`

## 8. Suggested UI Copy Mapping

### Android POS

- `Table Management`
- `Ordering`
- `Order Review`
- `Cashier Settlement`
- `Payment Completed`

### QR Ordering Web

- `扫码点餐`
- `加入购物车`
- `提交给前台`
- `等待前台结账`

### Merchant Admin

- `POS / QR`
- `Pending Settlement`
- `Member`
- `Promotion`
- `GTO Sync`

## 9. Do / Don't

### Do

- 一桌一单
- 用统一状态名
- 桌台状态与订单状态分开
- 顾客扫码点餐与 cashier 结账分开

### Don't

- 把 QR 和 POS 当成两张单
- 在不同页面用不同状态词表示同一状态
- 把“点单”和“结账”混成一个动作
- 用临时文案替代正式业务术语

## 10. Current Canonical Vocabulary

当前项目建议以这套术语作为标准：

- `Table`
- `Active Table Order`
- `QR Ordering`
- `POS Ordering`
- `Order Review`
- `Send to Kitchen`
- `Cashier Settlement`
- `Payment Completed`
- `DRAFT`
- `SUBMITTED`
- `PENDING_SETTLEMENT`
- `SETTLED`
- `AVAILABLE`
- `ORDERING`
- `DINING`
- `PENDING_SETTLEMENT`
- `CLEANING`
