# Kitchen Status Model

## Purpose

本文件定义厨房履约状态模型，用于统一：

- POS 前厅状态理解
- 厨房显示端状态
- 订单项可结账范围
- 退菜 / 加菜 / 出餐规则

## Status Layers

厨房相关状态分为两层：

### 1. Order-Level Status

用于表达活动桌单的大阶段：

- `DRAFT`
- `SUBMITTED`
- `PENDING_SETTLEMENT`
- `SETTLED`

### 2. Kitchen Item / Round Status

用于表达已送厨内容的履约阶段：

- `SUBMITTED`
- `PREPARING`
- `READY`
- `SERVED`

## Model Recommendation

建议采用：

- 一张 `Active Table Order`
- 多个 `Kitchen Submission Round`
- 每个 round 下多个 `Kitchen Ticket Item`

## Round Lifecycle

### Draft Round

- 前厅仍在点单
- 可增删改
- 不进入厨房

### Submitted Round

- 已发送到厨房
- round 锁定
- 不再按草稿逻辑直接修改

## Kitchen Item Lifecycle

### `SUBMITTED`

含义：

- 菜品已进入厨房队列
- 尚未开始制作

### `PREPARING`

含义：

- 厨房已开始处理

### `READY`

含义：

- 菜品已完成
- 等待上菜 / 领取

### `SERVED`

含义：

- 菜品已上桌 / 已交付

## Allowed Transitions

### Round

- `DRAFT -> SUBMITTED`

### Kitchen Item

- `SUBMITTED -> PREPARING`
- `PREPARING -> READY`
- `READY -> SERVED`

不允许直接跳过的情况：

- `SUBMITTED -> SERVED`
- `PREPARING -> SUBMITTED`

## Payment Rules

Payment preview 统计范围：

- 只统计属于已送厨 round 的 item
- 不统计仍处于 draft 的 item

一期建议：

- 只要 item 已进入 submitted round，就允许进入结账汇总
- 后续如需严格控制，可再增加“仅 READY / SERVED 才可结账”的门店策略

## Table Status Mapping

建议桌台状态与厨房/订单状态这样映射：

- 无活动单：`AVAILABLE`
- 有 draft：`ORDERING`
- 有 submitted/preparing/ready/served：`DINING`
- 进入收银：`PENDING_SETTLEMENT`
- 已结账待翻台：`CLEANING`

## UI Wording

建议前厅显示：

- `Draft items`
- `Sent to kitchen`
- `Preparing`
- `Ready to serve`
- `Served`
- `Payment Pending`

建议避免使用模糊词：

- `In process`
- `Cooking flow`
- `Settlement pending`

## Audit Requirements

每次厨房状态变化应记录：

- `active_order_id`
- `round_id`
- `item_id`
- `from_status`
- `to_status`
- `actor_type`
- `actor_id`
- `created_at`

