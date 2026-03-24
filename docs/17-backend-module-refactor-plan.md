# Backend Module Refactor Plan

## 1. Goal

本文档用于说明 Restaurant POS 后端当前为什么需要重构、哪些地方先重构、哪些地方暂时不动，以及如何在不打断当前业务推进的前提下完成结构化重构。

本计划的目标不是“推倒重写”，而是：

- 围绕核心模型收敛结构
- 让后端更适合持续扩展
- 为未来按服务边界演进打基础

## 2. Refactor Position

当前项目不适合做一次性大重构，也不适合为了结构优雅而暂停业务推进。

正确策略是：

**边推进业务闭环，边进行有边界的结构化重构。**

## 3. Why Refactor Is Needed

当前代码具备很强的原型验证价值，但随着产品模型越来越清晰，已经暴露出以下问题：

- 一些业务逻辑是为了快速联调先写的，边界还不够清晰
- 订单、会员、促销、报表、GTO 的职责分离还不够稳定
- 后端部分结构仍偏“接口骨架”而不是“稳定领域模型”
- 若不开始收边界，后续扩展总后台、商户后台、AI 层时会越来越难

## 4. Refactor Strategy

重构采用分层推进：

### 第一层：必须尽快收敛

- 统一订单核心模型
- 明确后端领域边界
- 收拢跨域写数据问题

### 第二层：边做业务边收

- 商户后台的 service / page 边界
- Android Preview 状态层
- Android 原生 ViewModel 与状态流

### 第三层：后面再做

- 基础设施升级
- 真正独立部署
- 更重的微服务拆分

## 5. Refactor Priority

### Priority 1: Order Core Refactor

目标：

- 把“活动桌单”变成真正稳定的交易核心

重点：

- 一桌一单
- QR / POS 共用活动单
- 订单状态流
- 订单项快照
- 结账前后状态清晰

为什么优先：

- 它是所有 CRM / Promotion / Settlement / Report / GTO 的前提

### Priority 2: Domain Boundary Refactor

目标：

- 让后端按领域边界组织，而不是按页面需求堆逻辑

重点：

- Merchant
- Store
- Catalog / SKU
- Order
- Member / CRM
- Staff
- Promotion
- Settlement
- Report
- GTO
- Platform Admin

### Priority 3: Catalog / SKU Refactor

目标：

- 把商品体系从“简单 product 列表”升级成可长期承载的 SKU 中枢

重点：

- Product / SKU 分层
- 规格和选项
- 价格快照
- 门店可售范围

### Priority 4: CRM and Promotion Refactor

目标：

- 让会员和促销不再只是订单旁边的临时字段

重点：

- 独立实体
- 独立规则
- 命中快照
- 流水与报表基础

### Priority 5: Settlement and Reporting Refactor

目标：

- 让结账、退款、打印、报表真正围绕交易事实沉淀

重点：

- SettlementRecord
- PaymentRecord
- RefundRecord
- 报表读取事实表

## 6. What Should Be Kept for Now

以下内容当前不应被大幅推翻：

- 已跑通的 QR 点单主链路
- 已打通的 POS 与 QR 共用活动桌单逻辑
- 已经建立的 Merchant Admin 页面骨架
- 已经建立的 Docker / 本地联调方式

这些东西当前价值很高，应在其基础上演进，而不是重写。

## 7. What Should Be Refactored First

最推荐先动的代码区域：

### 7.1 Backend Order Module

先把：

- 活动桌单
- 当前桌单查询
- 当前桌单更新
- 结账状态流

收成真正统一模型。

### 7.2 Backend Package Structure

把现有后端整理成更清晰的领域结构：

- catalog
- order
- member
- promotion
- settlement
- report
- gto
- platform

### 7.3 Preview State Layer

把 `android-preview-web` 中：

- 桌台状态
- 当前活动订单
- QR / POS 合单逻辑

从页面层继续往更稳定的状态层收。

## 8. Recommended Refactor Order

建议顺序：

1. Order core
2. Backend package/domain structure
3. Catalog / SKU
4. CRM
5. Promotion
6. Settlement
7. Report
8. GTO
9. Platform admin support modules

## 9. Refactor Rules

重构时必须遵守：

1. 不推翻已跑通闭环
2. 不因为重构停止产品推进
3. 每次只收一个核心域
4. 每次重构都要明确边界和 source of truth
5. 重构结果必须能落到文档、代码、测试上

## 10. Final Position

Restaurant POS 当前确实需要重构，但这次重构不应被理解成“重新做一遍”。

正确理解是：

**在保留已跑通业务闭环的前提下，把系统逐步收敛成一个可持续扩展、可服务化演进的架构。**
