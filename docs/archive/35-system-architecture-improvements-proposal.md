# System Architecture Improvements Proposal

## Purpose

本文档从系统架构升级角度，总结当前 Restaurant POS 在实现过程中暴露出的结构性问题，并提出下一轮更稳的架构改进方向。

本文件不讨论产品功能范围，而专注于：

- 交易核心模型
- 状态机设计
- domain 边界
- 前后端职责
- 监测与一致性
- AI-ready 架构承载方式

---

## Executive Summary

当前系统已经完成了：

- V2 交易主线
- POS / QR ordering
- member / promotion 基础
- settlement 主线

但在实现过程中也暴露出几个典型架构问题：

1. 以 `active order` 为中心的模型不够适合餐饮
2. 状态机设计晚于 UI 流程设计
3. 前端一度同时持有本地状态和业务真相
4. 厨房、退款、监测等能力缺少自然挂点
5. 四端共享核心 domain，但端侧接口形状还不够清晰

因此，下一轮系统架构建议升级为：

- `table session centered transaction architecture`
- explicit state machine architecture
- backend as single source of truth
- event-supported domain model
- facade/BFF per end
- early consistency monitoring layer

---

## 1. Proposed Architectural Direction

### 1.1 From Active Order Centered to Table Session Centered

建议将交易中心从：

- `active order`

升级为：

- `table_session`
- `draft_order`
- `submitted_order`
- `settlement`

### Why

餐饮真实场景不是“一张单一直变化”，而是：

- 一张桌的一次就餐会话
- 期间产生一个 draft
- 多次送厨形成多个 submitted orders
- 最后统一 payment

### Benefits

- 更自然支持多轮点菜
- 已送厨和草稿天然分离
- QR 与 POS 共存更简单
- 厨房流更自然
- payment 汇总更清楚

---

## 2. State Machine as an Explicit Architecture Layer

### Problem

如果状态机隐含在 controller、service、页面按钮里，就会产生：

- 不一致状态推进
- 前端能点但后端不该收
- 页面语义与数据语义错位

### Proposal

显式引入状态机层：

- `TableSessionStateMachine`
- `DraftOrderStateMachine`
- `SubmittedOrderStateMachine`
- `SettlementStateMachine`

### Recommended states

#### Table Session

- `OPEN`
- `PAYMENT_PENDING`
- `SETTLED`
- `CLOSED`

#### Draft Order

- `DRAFT`
- `DISCARDED`

#### Submitted Order

- `SUBMITTED`
- `PREPARING`
- `READY`
- `SERVED`
- `SETTLED`
- `VOIDED`

#### Settlement

- `INITIATED`
- `AUTHORIZED`
- `CAPTURED`
- `FAILED`
- `REFUNDED`

### Rule

所有 UI、API、domain command 都应通过状态机规则推进，而不是直接自由改状态字段。

---

## 3. Backend as the Single Source of Truth

### Problem

前端一旦长期保存业务状态，会产生：

- 刷新回弹
- 状态和数据库不一致
- 用户误以为成功但其实未持久化

### Proposal

明确分层：

- Backend owns business truth
- Frontend owns UI interaction state

### Frontend may keep

- 搜索关键字
- 当前 tab
- 展开/收起
- 正在输入的草稿表单

### Frontend should not own long-lived truth

- 桌台状态
- 已送厨轮次
- payment 状态
- 活动订单主状态

### Result

刷新后页面必须能完全依赖后端重建。

---

## 4. Domain-Oriented Architecture Refinement

### Current direction

我们已经按 domain 划分了大方向：

- merchant
- store
- catalog
- order
- member
- promotion
- settlement
- report
- gto
- platform

### Next refinement

应进一步明确每个 domain 的 source of truth：

#### Table / Session Domain

- table
- table_session
- table status

#### Order Domain

- draft_order
- submitted_order
- submitted_order_item

#### Settlement Domain

- payment
- settlement_record
- settlement_order_link

#### Member Domain

- member
- member_account
- member ledger

#### Promotion Domain

- promotion_rule
- promotion_hit
- gift snapshot

#### Report Domain

- read model / aggregate facts only

### Key principle

`Order` 不是所有交易信息的垃圾桶。

---

## 5. Event-Supported Transaction Architecture

### Proposal

关键交易动作应有事件化表达，即使当前仍以模块化单体实现：

- `table_session_opened`
- `draft_updated`
- `submitted_to_kitchen`
- `payment_marked_pending`
- `payment_collected`
- `submitted_order_settled`
- `table_released`
- `member_bound`
- `promotion_applied`

### Why

这样能带来：

- 审计可追溯
- 异常排查更容易
- 报表口径更稳
- 后续 AI 分析更容易
- 后续拆分微服务更平滑

### Current recommendation

先做事件表和关键行为日志，不必一开始就上完整 MQ。

---

## 6. Facade / BFF Layer for Four Ends

### Problem

虽然四端共用核心 domain，但每个端需要的接口形状不同：

- POS
- QR
- Merchant Admin
- Platform Admin

如果所有端都直接吃同一套低层 API，容易出现：

- DTO 混乱
- 端之间耦合
- 接口语义不稳

### Proposal

在核心 domain 之上建立端侧 facade：

- `pos-facade`
- `qr-facade`
- `merchant-admin-facade`
- `platform-admin-facade`

### Benefits

- 端侧 contract 更稳定
- 核心 domain 不被端侧需求污染
- 更利于权限控制和响应裁剪

---

## 7. Monitoring and Consistency Layer

### Problem

当前很多问题都是人眼发现：

- table refresh 回弹
- payment 后桌台没释放
- QR/ POS 状态不一致

### Proposal

尽早引入一致性监测层：

- `table_state_consistency_checker`
- `order_state_consistency_checker`
- `settlement_consistency_checker`
- `submitted_order_orphan_checker`

### Example rules

- `AVAILABLE` table should not have unpaid submitted orders
- `PAYMENT_PENDING` session must have at least one unpaid submitted order
- `SETTLED` session should not still have draft order
- QR submit should not duplicate already submitted lines unintentionally

### Delivery form

第一阶段可先做：

- checklist
- scheduled checker
- admin monitor page

后续再升级成告警系统。

---

## 8. Reporting Architecture Improvement

### Problem

如果 Merchant Admin 的报表直接依赖交易表现场拼接，后面会遇到：

- 口径漂移
- 查询复杂
- 变更风险高

### Proposal

引入更明确的 reporting strategy：

- domain writes transactional facts
- report domain owns aggregated read models

### Result

报表不会反向污染交易模型。

---

## 9. AI-Ready Architecture as a Cross-Cutting Layer

### Problem

如果每个业务模块单独长一套 AI 接口，长期会变成：

- recommendation 不统一
- approval 不统一
- 审计不统一

### Proposal

AI 能力应以横切协议存在：

- recommendation
- execution
- approval
- audit

并由各 domain 接入，而不是各自重做一套。

### Example

#### Promotion

- create rule
- recommend rule
- approve rule
- execute rule
- audit rule source

#### CRM

- create tier rule
- recommend segmentation
- approve change
- audit actor/source

---

## 10. Completion Model Improvement

### Problem

当前很容易把“代码写了”误认为“功能完成”。

### Proposal

以后每个模块明确区分：

1. `Designed`
2. `Code Integrated`
3. `Build Verified`
4. `Runtime Verified`
5. `Journey Verified`
6. `Accepted`

### Why

这样不会再把：

- code integrated

误报成：

- fully complete

---

## 11. Recommended Next-Step Architecture Artifacts

如果继续推进架构升级，建议优先补这些产物：

1. `Table Session Final Domain Model`
2. `State Machine Specification`
3. `Order State Consistency Checklist`
4. `Facade / BFF API Allocation Matrix`
5. `Transaction Event Model`
6. `Reporting Read Model Proposal`

---

## Final Recommendation

下一轮系统演进不应再以“补几个功能页面”为主，而应以：

- transaction model upgrade
- state machine formalization
- backend truth consolidation
- monitoring introduction
- facade refinement

为核心。

一句话总结：

**系统已经有了可运行的交易主线，下一轮架构升级的重点不是再长更多页面，而是让模型、状态、监测、端侧边界真正稳定下来。**
