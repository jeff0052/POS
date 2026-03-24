# Kitchen Display Flow

## Purpose

本文件定义厨房端的最小流程，用于支持：

- 厨房接单
- 制作状态更新
- 前厅同步查看

## End-to-End Flow

### 1. Front-of-House Drafting

- Cashier / server 在 POS 中创建草稿菜品
- 顾客 QR 下单可直接形成已送厨 round

### 2. Send to Kitchen

- POS 点击 `Send to kitchen`
- 系统创建一个 `Kitchen Submission Round`
- round 下的 item 进入 `SUBMITTED`

### 3. Kitchen Queue Display

厨房端展示：

- 桌号
- round 编号
- 提交时间
- 菜品列表
- 备注
- 状态

排序建议：

- 先按状态
- 再按提交时间

## Kitchen Screen Sections

### A. New Tickets

显示：

- `SUBMITTED`

动作：

- `Start preparing`

### B. In Progress

显示：

- `PREPARING`

动作：

- `Mark ready`

### C. Ready

显示：

- `READY`

动作：

- `Mark served`

### D. Served History

显示：

- `SERVED`

一期可只做只读历史区。

## Front-of-House Sync

POS 必须同步显示：

- 哪些 round 已送厨
- 每个 round 当前状态
- 是否还存在 draft add-ons

推荐的展示方式：

- Current Draft
- Sent to Kitchen
- Ready / Served badges

## Special Cases

### Add-on After Submission

- 客人加菜
- 新菜进入新的 draft round
- 再次点击 `Send to kitchen`
- 厨房端看到新 round，不覆盖旧 round

### QR Ordering

- 顾客扫码提交后默认形成 `SUBMITTED` round
- 厨房端直接看到该 round

### Payment

- Payment 页面汇总所有已提交未结账 round
- 草稿 round 不参与 payment

## MVP UI Requirements

一期厨房端不追求复杂设计，先满足：

- 列表足够清晰
- 可快速切换状态
- 桌号和 round 易识别
- 菜品和备注易读

## Success Criteria

厨房端 MVP 成功标准：

1. 前厅送厨后厨房端立即可见
2. 厨房状态可从 `SUBMITTED -> PREPARING -> READY -> SERVED`
3. 前厅能看到厨房最新状态
4. 同桌多轮不互相覆盖
5. 支付时不会把草稿轮次算进去
