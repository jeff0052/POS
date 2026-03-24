# Technical Stack and Selection Rationale

## 1. Goal

本文档用于明确 Restaurant POS 当前的技术选型、采用原因、适用阶段以及后续演进方向。

目标不是追求“最重、最全”的架构，而是在一期阶段优先确保：

- 可以快速交付
- 可以真实联调
- 可以接入餐饮 POS 所需的支付与打印能力
- 可以支撑试点门店运行

## 2. Current Stack Overview

### 2.1 Android POS

- Kotlin
- Jetpack Compose
- Hilt
- ViewModel + StateFlow
- Room
- Retrofit

适用模块：

- 门店 POS
- cashier 点单
- cashier 结账
- 桌台管理
- 原生支付 / 打印 SDK 接入

### 2.2 POS Preview Web

- React
- Vite
- TypeScript

适用模块：

- Android POS 快速预览
- UI/流程验证
- 产品评审
- 联调演示

### 2.3 QR Ordering Web

- React
- Vite
- TypeScript

适用模块：

- 顾客扫码点餐 H5
- 桌码点餐流程验证
- 会员识别与自助下单入口

### 2.4 Merchant Admin

- React
- Vite
- TypeScript
- Ant Design
- React Router

适用模块：

- 商户后台
- 订单、会员、促销、报表、GTO 等管理能力

### 2.5 Backend

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- MySQL

适用模块：

- 订单服务
- CRM 会员
- 促销
- 结账
- 报表
- GTO 同步

### 2.6 Deployment

- Docker
- Docker Compose
- Nginx

适用阶段：

- 本地联调
- 内部测试环境
- 小规模试点环境

## 3. Why This Stack Was Chosen

## 3.1 Android Native Was Chosen for POS

门店 POS 是最靠近设备的一端，需要：

- 支付 SDK 接入
- 打印 SDK 接入
- 设备状态读取
- 更稳定的本地交互体验

因此 Android POS 采用 Kotlin + Compose 是合理选择。

### 3.2 React + Vite Was Chosen for Fast Iteration

商户后台、扫码点餐 H5 和预览端都强调：

- 迭代速度
- 可视化验证
- 快速联调

React + Vite + TypeScript 能快速支持这类产品形态。

### 3.3 Spring Boot Was Chosen for Business Domain Delivery

当前系统的重点是复杂业务域，而不是极致基础设施优化。

Spring Boot 的优势在于：

- 适合业务模块清晰拆分
- 适合快速出 API
- 适合订单、会员、促销、报表等中台逻辑
- 团队可维护性高

### 3.4 MySQL Was Chosen for Transaction and Reporting Stability

当前系统核心是：

- 一桌一单
- 结账
- 会员积分/余额
- 报表和 GTO 对账

MySQL 足够支撑一期：

- 交易记录
- 状态流
- 快照
- 批量导出

## 4. Strengths of the Current Stack

- Android 原生端适合餐饮 POS 硬件场景
- Web 两端开发速度快
- Spring Boot + MySQL 足以支撑一期交易闭环
- Docker Compose 适合快速部署测试和试点
- 整体门槛低，适合当前阶段持续推进

## 5. Known Current Limits

当前技术栈是为一期和试点服务的，还没有上这些能力：

- Redis
- 消息队列
- WebSocket / SSE 正式实时推送
- 独立对象存储
- 托管数据库体系
- 微服务拆分
- K8s / ECS 级别编排
- 完整多租户基础设施

这不是缺陷，而是阶段性取舍。

## 6. Selection Strategy

当前采用的是：

**先打通真实业务闭环，再逐步升级基础设施**

也就是说我们优先解决：

- 订单是否统一
- QR 与 POS 是否共用活动桌单
- cashier 结账是否稳定
- 会员与促销是否可追踪
- GTO 是否可导出

而不是在一开始就投入过多精力到：

- 微服务治理
- 高并发基础设施
- 大规模多租户平台化

## 7. Upgrade Path

### Phase 1: Current

- Android 原生 POS
- React Web
- Spring Boot + MySQL
- Docker Compose

### Phase 2: Trial Stabilization

建议逐步加入：

- Redis
- 更清晰的环境变量管理
- 数据库迁移工具
- 结构化日志
- 基础监控

### Phase 3: Production Expansion

在门店规模、商户数量、订单量上来后，再考虑：

- 托管数据库
- 事件驱动
- 实时推送
- 多租户增强
- 更独立的总后台能力

## 8. Final Position

当前技术选型的定位是：

**适合 Restaurant POS 一期产品验证、试点门店上线和持续快速迭代的现实型选型。**

它不是终局架构，但非常适合当前阶段。
