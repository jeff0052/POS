# Kitchen Fulfillment Requirements

## Purpose

本文件定义餐饮 POS 中“厨房履约”能力的一期需求。目标不是只支持“Send to kitchen”这个按钮，而是把厨房接单、制作、出餐、回传状态这条链路正式纳入系统模型。

## Why This Exists

当前系统已经具备：

- Active Table Order
- POS 点单
- QR 点单
- Cashier Settlement

但还缺少正式的厨房履约层。没有这层，前厅只能知道“已送厨”，不知道：

- 哪些菜正在做
- 哪些菜已经做好
- 哪些菜已经上桌
- 哪些菜可以退
- 哪些加菜是新的厨房轮次

## Scope

一期厨房履约能力覆盖：

- 从前厅向厨房发送菜品
- 厨房查看待制作内容
- 厨房更新制作状态
- 前厅查看厨房回传状态
- 支持同桌多轮送厨
- 结账时仅统计已送厨未结账内容

一期不覆盖：

- 厨房复杂工位编排
- 多厨房站点路由
- 厨房打印模板高级配置
- 厨房绩效分析
- AI 厨房调度

## Core Concepts

### 1. Active Table Order

一张桌在任一时间只有一张活动桌单。

### 2. Kitchen Submission Round

每次从 POS 或 QR 确认“Send to kitchen”，都会形成一轮厨房提交。  
同一张桌单下可以有多轮。

### 3. Kitchen Ticket Item

每个已送厨菜品都是厨房履约对象。  
草稿中的菜不进入厨房。

## User Roles

### Cashier / Server

- 创建和编辑草稿菜品
- 发送草稿到厨房
- 查看菜品厨房状态
- 在允许的规则下做加菜 / 退菜

### Kitchen Staff

- 查看待制作菜品
- 更新制作状态
- 标记已完成 / 已出餐

### Store Manager

- 查看厨房履约效率
- 处理异常单 / 催菜 / 退菜

## Functional Requirements

### A. Send to Kitchen

- 草稿菜品可以批量送厨
- 每次送厨必须形成独立 round
- 已送厨 round 不允许直接按原草稿方式修改
- 送厨后前厅可以继续新建下一轮草稿

### B. Kitchen Queue

厨房端必须能看到：

- 桌号
- round 编号
- 菜品名称
- 数量
- 备注
- 提交时间
- 当前状态

### C. Kitchen Status Update

厨房一期最小状态：

- `SUBMITTED`
- `PREPARING`
- `READY`
- `SERVED`

每次状态变更都必须记录时间和操作者。

### D. Front-of-House Visibility

前厅必须能看到：

- 当前桌已送厨轮次
- 每轮的状态概览
- 菜品是否已做好 / 已上桌

### E. Payment Dependency

支付规则必须明确：

- 草稿菜品不进入本次结账
- 已送厨且未结账菜品进入本次结账
- 同桌多轮已送厨内容统一汇总到 payment preview

### F. Add-on Ordering

送厨后仍允许加菜：

- 新加的菜进入新的 draft round
- 再次送厨后形成新的 submitted round

### G. Voids / Returns

一期先保留规则接口，不做完整实现，但模型必须预留：

- 已送厨前可直接删除
- 已送厨后退菜需经过厨房/店长规则

## Required UI Surfaces

### Store POS

- Ordering 页显示：
  - 当前草稿
  - 已送厨轮次
  - 每轮状态

### Kitchen Display

- 厨房待办列表
- 单轮详情
- 状态更新按钮

### Merchant Admin

- 厨房履约监控入口
- 后续可扩效率报表

## Acceptance Criteria

一期厨房履约能力通过的最低标准：

1. 前厅可以将一轮草稿送厨
2. 送厨后该轮不再按草稿方式编辑
3. 前厅可继续新增下一轮草稿
4. 厨房端能看到已送厨轮次
5. 厨房状态变更能回传前厅
6. Payment preview 只统计已送厨内容

