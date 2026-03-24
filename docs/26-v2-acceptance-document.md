# V2 Acceptance Document

## 1. Purpose

本文档定义 Restaurant POS `v2` 的统一验收标准。

目标不是只回答“功能有没有做出来”，而是明确：

- 什么能力必须完成
- 什么状态才算可交付
- 哪些验收项必须可演示、可测试、可追溯
- 哪些能力属于后续阶段，不应混入当前验收范围

本文件将作为：

- 产品验收基线
- 设计验收基线
- 开发完成定义（Definition of Done）
- 测试回归清单基线

---

## 2. Acceptance Position

V2 当前不以“全系统一次性上线”为目标，而是按阶段验收：

1. `V2 Foundation Acceptance`
2. `Store Transaction MVP Acceptance`
3. `Merchant Operations Acceptance`
4. `Platform Admin Acceptance`
5. `Production Readiness Acceptance`

当前最优先的是：

**V2 Foundation Acceptance**

---

## 3. Core Acceptance Principle

### 3.1 Business Model Must Be Correct Before Feature Breadth

先验收业务模型正确，再验收功能数量。

优先保证：

- 一桌一单
- POS 与 QR 共编辑
- cashier 最终结账
- 状态流转正确
- 订单、结算、报表口径一致

### 3.2 Acceptance Must Be Traceable

每个验收项都应能追溯到至少一种交付物：

- 页面
- API
- 数据表
- 测试用例
- 日志或审计记录

### 3.3 Prototype Success Does Not Equal V2 Acceptance

原型跑通不等于正式通过验收。

V2 验收要求：

- 结构化实现
- 状态稳定
- 刷新不丢
- 数据可审计
- 文案和术语统一

---

## 4. V2 Foundation Acceptance Scope

V2 Foundation 先验收这几个核心域：

1. Store + Table
2. Catalog / SKU
3. Active Table Order
4. QR Ordering
5. Cashier Settlement
6. Staff Shift (基础)

以下内容在当前 Foundation 阶段不作为通过前置条件：

- 完整 CRM
- 完整 Promotion Center
- 完整 Report Center
- 完整 GTO
- 完整 Platform Admin
- AI recommendation/execution 落地

---

## 5. Functional Acceptance Criteria

## 5.1 Store and Table

必须满足：

- 可以创建并管理门店
- 可以创建并管理桌台
- 桌台是独立对象，不是页面临时数据
- 桌台状态可由活动订单状态推导

通过标准：

- 门店下至少可管理多张桌台
- 桌台在系统中有唯一标识
- 桌台状态与订单状态映射正确

不通过情况：

- 桌台只是前端 mock 数据
- 桌台状态不能稳定反映当前活动订单

## 5.2 Catalog / SKU

必须满足：

- 商品与 SKU 被区分建模
- 订单交易对象是 SKU，不是模糊商品名
- 订单项可保存 SKU 快照

通过标准：

- SKU 可被 QR 与 POS 共用
- 订单项中存在 SKU 快照字段
- SKU 变更不污染历史订单

## 5.3 Active Table Order

必须满足：

- 同一桌同一时刻只有一张活动订单
- POS 与 QR 共编辑同一张活动订单
- 活动订单支持加菜、删菜、改数量
- 刷新后状态不回弹

通过标准：

- POS 点菜后刷新仍保留
- QR 点菜后刷新仍保留
- POS 与 QR 加菜不会相互覆盖
- 空桌无活动订单

不通过情况：

- POS 点单和 QR 点单各自生成独立活动单
- 删除后刷新回到旧状态

## 5.4 QR Ordering

必须满足：

- 顾客扫描桌码进入当前桌菜单
- 可以浏览 SKU、加购物车、提交到当前桌活动订单
- 顾客提交后不直接付款
- 前台与后台能看到该桌订单变化

通过标准：

- 桌码能绑定 `store + table`
- QR 提交会合并到当前桌活动订单
- 提交后 POS 可见
- 提交后商户后台可见

## 5.5 Cashier Settlement

必须满足：

- cashier 对 `PENDING_SETTLEMENT` 订单执行收款
- 结账后生成明确结算结果
- 结账后桌台释放或进入清台状态

通过标准：

- 只能对待结订单收款
- 收款成功后订单进入 `SETTLED`
- 桌台不再继续显示活动待结单

## 5.6 Staff Shift (Foundation Level)

必须满足：

- cashier 有登录身份
- 订单与 cashier 归属可追溯
- 系统为后续班次体系预留结构

Foundation 阶段通过标准：

- 订单创建与结账可关联 cashier
- API / 数据结构中具备 shift / cashier 归属字段

---

## 6. State Acceptance Criteria

订单状态必须满足：

- `DRAFT`
- `SUBMITTED`
- `PENDING_SETTLEMENT`
- `SETTLED`

桌台状态必须满足：

- `AVAILABLE`
- `ORDERING`
- `DINING`
- `PENDING_SETTLEMENT`
- `CLEANING`

通过标准：

- 文档、页面、API、数据字段使用统一术语
- 页面按钮与状态推进逻辑一致
- 非法状态流转会被拒绝

示例：

- `DRAFT -> SUBMITTED` 允许
- `DRAFT -> PENDING_SETTLEMENT` 在特定场景允许
- `SETTLED -> DRAFT` 不允许

---

## 7. Four-End Acceptance View

## 7.1 Store POS

必须通过：

- 桌台管理
- POS 点单
- QR 订单接入
- 订单复核
- cashier 结账

## 7.2 QR Ordering

必须通过：

- 桌码进入
- 菜单浏览
- 点菜提交
- 与活动订单合并

## 7.3 Merchant Admin

Foundation 阶段至少通过：

- 可查看 POS / QR 订单
- 可看到待结订单
- 可看到桌号、来源、金额、状态

## 7.4 Platform Admin

Foundation 阶段不要求完整实现，但要求：

- 架构和需求已经定义
- 不影响后续接入

---

## 8. API Acceptance Criteria

V2 Foundation 核心 API 必须通过：

- `active table order`
- `qr ordering`
- `cashier settlement`

通过标准：

- API contract 与文档一致
- 接口字段名与术语文档一致
- 状态流转规则可校验
- 失败返回可识别错误
- actor 信息可追溯

---

## 9. Data Acceptance Criteria

必须满足：

- 订单数据可持久化
- 状态可恢复
- 金额可追踪
- 快照可保留

通过标准：

- 存在活动订单表
- 存在订单项表
- 存在事件记录或等效审计记录
- 结算后保留历史事实

不通过情况：

- 关键状态只存在前端内存
- 刷新后状态丢失
- 历史订单被当前商品配置污染

---

## 10. UX Acceptance Criteria

必须满足：

- 点桌后可以立即理解当前在操作哪张桌
- QR 与 POS 的关系对用户清晰
- 收款动作与点单动作清晰分离
- 状态文案统一

通过标准：

- 页面标题、按钮、状态胶囊使用统一术语
- 不存在同一概念多种叫法混用
- 关键主流程不依赖解释就能走通

---

## 11. AI-Ready Acceptance Criteria

Foundation 阶段不要求 AI 真正上线，但要求：

- 核心 API contract 已预留 AI-ready 元信息
- 新 domain 设计遵循 AI-ready checklist
- Human-driven 与 AI-assisted 结构可兼容

通过标准：

- API / 文档中已体现 `actorType`、`decisionSource` 或等价概念
- 关键域可扩展 recommendation / approval / audit

---

## 12. Demo Acceptance Scenarios

V2 Foundation 至少必须能演示以下场景：

1. cashier 在 POS 为 `T6` 点单并保存草稿
2. 顾客扫码同一桌 `T6` 再加菜
3. POS 看到合并后的当前桌活动订单
4. cashier 删菜并刷新后状态仍保持
5. cashier 将订单推进到待结账
6. cashier 完成结账
7. 桌台释放
8. 商户后台看到对应订单与状态

只有上述主链路稳定通过，Foundation 才算通过验收。

---

## 13. Exit Criteria

当以下条件全部满足时，V2 Foundation 可视为通过：

- 核心订单模型实现正确
- 状态模型实现正确
- POS / QR / Merchant Admin 三端主流程连通
- 关键数据持久化稳定
- 刷新和回到页面后状态保持一致
- 文档、术语、API contract、代码结构已同步

---

## 14. Final Definition

V2 的“通过验收”不是“页面看起来差不多”，而是：

**核心对象正确、状态流转正确、数据可追溯、四端主流程可稳定联通。**
