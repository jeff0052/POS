# Payment Adapter Design

## Purpose

本文档定义 Restaurant POS 统一支付域下的适配器设计方案，用于把：

- `DCS`
- `VibeCash`
- `Cash`

三类 provider 接入统一 payment architecture。

本文件重点回答：

- `Payment Orchestrator` 应该怎么工作
- provider adapter 的统一 contract 是什么
- DCS / VibeCash / Cash 各自负责什么
- provider / method / scheme 应该怎么映射
- 回调、查询、退款、失败处理应该怎么落

---

## Executive Summary

统一支付设计不应让业务代码直接依赖：

- Android terminal SDK
- gateway-specific HTTP API
- 某一个支付品牌的返回值

而应采用：

- `Payment Orchestrator`
- `Payment Adapter Interface`
- `Provider-Specific Adapter`

让上层业务始终只看：

- `PaymentIntent`
- `PaymentAttempt`
- `PaymentStatus`
- `RefundRecord`

---

## 1. Design Goals

支付适配器设计需要满足：

1. provider-specific complexity 封装在 adapter 内
2. 不同支付通道使用统一 business contract
3. 支持同步式和异步式支付 provider
4. 支持支付、查询、撤销、退款
5. 支持审计、报表、对账和后续 AI-ready 扩展

---

## 2. Recommended Structure

### 2.1 Layers

#### Payment Domain

负责统一业务语义：

- `PaymentIntent`
- `PaymentAttempt`
- `PaymentStatus`
- `RefundRecord`

#### Payment Orchestrator

负责：

- 创建 payment intent
- 根据 provider / method / scheme 选 adapter
- 调 adapter 发起支付
- 接收 callback / query result
- 持久化 payment attempt
- 回写 settlement / table session

#### Payment Adapter Interface

定义所有 provider adapter 必须实现的统一 contract。

#### Concrete Adapters

- `DcsPaymentAdapter`
- `VibeCashAdapter`
- `CashPaymentAdapter`

---

## 3. Core Model Dependencies

Payment Adapter Design 依赖以下统一模型：

### Provider

- `DCS`
- `VIBECASH`
- `CASH_MANUAL`

### Method

- `CARD`
- `QR`
- `CASH`

### Scheme

- `VISA`
- `MASTERCARD`
- `AMEX`
- `JCB`
- `WECHAT_PAY`
- `ALIPAY`
- `PAYNOW`
- `GRABPAY`
- `SHOPEEPAY`

---

## 4. Payment Orchestrator Responsibilities

`PaymentOrchestrator` 是统一支付入口。

### 4.1 createPaymentIntent

输入：

- settlement context
- amount
- selected method
- selected scheme
- actor

输出：

- `PaymentIntent`

### 4.2 startPayment

输入：

- `paymentIntentId`
- provider-specific start params

输出：

- created `PaymentAttempt`
- initial normalized payment state

### 4.3 syncPaymentResult

用于：

- callback result
- polling result
- terminal query result

输出：

- updated `PaymentAttempt`
- updated normalized status

### 4.4 voidPayment

用于：

- payment reversal / void

### 4.5 refundPayment

用于：

- full refund
- partial refund if provider supports it in future

### 4.6 settleBusinessState

在支付成功后统一做：

- settlement success
- table release
- payment status persistence
- audit event creation

---

## 5. Adapter Interface Contract

建议统一 contract 如下：

## 5.1 resolveProvider

根据：

- method
- scheme
- store configuration

选择 provider。

例子：

- `CARD + VISA` -> `DCS`
- `QR + PAYNOW` -> `VIBECASH`
- `CASH + CASH` -> `CASH_MANUAL`

## 5.2 startPayment

输入：

- `PaymentIntent`
- provider start params

输出：

- `AdapterStartPaymentResult`

Recommended fields:

- `providerCode`
- `providerTransactionId`
- `providerReferenceNo`
- `providerVoucherNo`
- `providerRrn`
- `normalizedStatus`
- `rawResponse`
- `actionRequired`

## 5.3 queryPayment

输入：

- original provider references

输出：

- latest normalized payment result

## 5.4 voidPayment

输入：

- original payment references

输出：

- normalized void result

## 5.5 refundPayment

输入：

- original payment references
- refund amount

输出：

- normalized refund result

---

## 6. DCS Adapter Design

## 6.1 Positioning

`DcsPaymentAdapter` 负责：

- card-present transaction
- terminal-side transaction flow
- terminal refund / void / batch settlement integration

Normalized mapping:

- `provider = DCS`
- `method = CARD`
- `scheme = VISA / MASTERCARD / AMEX / JCB / ...`

## 6.2 Integration Characteristics

特点：

- Android service binding
- callback/state-machine style
- terminal device interaction

因此：

- DCS adapter 只能运行在 Android POS 端
- backend 不应直接依赖 DCS SDK

## 6.3 Responsibilities

- 建立 SDK 连接
- init / config merchant params
- 发起 card sale
- 处理 callback 中间状态
- 映射最终 transaction result
- 生成 normalized payment result

## 6.4 DCS-Specific Challenge

DCS 是状态机式流程，不是简单 HTTP 请求。

所以 adapter 内部需要处理：

- wait card
- app select
- confirm card no
- cert verify
- final transaction result

这些都不应泄漏到 payment domain 外层。

---

## 7. VibeCash Adapter Design

## 7.1 Positioning

`VibeCashAdapter` 负责：

- QR payment gateway integration
- 多 QR brand / scheme 的统一路由

Normalized mapping:

- `provider = VIBECASH`
- `method = QR`
- `scheme = WECHAT_PAY / ALIPAY / PAYNOW / GRABPAY / SHOPEEPAY / ...`

## 7.2 Responsibilities

- create payment request
- start payment
- persist provider transaction identifiers
- query payment state
- handle callback / webhook
- normalize provider-native statuses

## 7.3 Design Constraint

VibeCash 是 gateway，不是单一支付方式。

因此 adapter 必须支持：

- method 固定为 `QR`
- scheme 动态选择不同 QR brand

也就是说，POS UI 选择的是：

- `QR`
- `PayNow`

底层 orchestrator 才路由到：

- `VibeCashAdapter`

---

## 8. Cash Adapter Design

## 8.1 Positioning

`CashPaymentAdapter` 负责：

- 人工现金支付确认

Normalized mapping:

- `provider = CASH_MANUAL`
- `method = CASH`
- `scheme = CASH`

## 8.2 Responsibilities

- 接收 cashier confirmation
- 生成 payment attempt
- 更新 normalized payment result
- 保留 cashier attribution

Cash 没有第三方 provider 交互，但必须仍然走统一 adapter contract。

---

## 9. Callback and Result Handling

## 9.1 Sync Providers

对于同步返回成功/失败的 provider：

- `startPayment` 可直接返回 final normalized result

## 9.2 Async Providers

对于异步 provider：

- `startPayment` 返回 `PENDING`
- 后续通过 callback 或 query 更新状态

## 9.3 Required Normalized Result

不论 provider 类型如何，adapter 最终都必须输出：

- `providerCode`
- `providerTransactionId`
- `providerReferenceNo`
- `providerVoucherNo`
- `providerRrn`
- `normalizedStatus`
- `errorCode`
- `errorMessage`
- `rawResponseSnapshot`

---

## 10. Refund and Void Design

退款和撤销也必须走 adapter，而不是直接散落到各 provider SDK/API 调用中。

### 10.1 Void

适用：

- payment 尚可撤销的场景

### 10.2 Refund

适用：

- payment 已完成后的退款

### 10.3 Unified Output

无论是 DCS 还是 VibeCash：

- 都应更新 `RefundRecord`
- 都应返回 normalized refund status

---

## 11. Storage and Audit Requirements

每次 adapter 调用都应保留：

- provider code
- method
- scheme
- request snapshot
- response snapshot
- normalized status
- actor type
- actor id
- created at
- updated at

这样后面才能支撑：

- merchant admin 查询
- reports
- reconciliation
- anomaly detection
- AI-ready analysis

---

## 12. Recommended Backend Interfaces

建议 payment orchestrator 对上层暴露：

- `createPaymentIntent(...)`
- `startPayment(...)`
- `queryPayment(...)`
- `voidPayment(...)`
- `refundPayment(...)`
- `syncPaymentResult(...)`

建议 concrete adapter 暴露：

- `supports(provider, method, scheme)`
- `startPayment(intent, params)`
- `queryPayment(attempt)`
- `voidPayment(attempt)`
- `refundPayment(attempt, amount)`

---

## 13. Implementation Order

推荐实现顺序：

1. `CashPaymentAdapter`
   - 最简单，先打通 unified payment path
2. `VibeCashAdapter`
   - 完成 QR gateway path
3. `DcsPaymentAdapter`
   - 完成 terminal card path
4. callback/query/refund unification
5. merchant admin payment visibility

---

## 14. Risks

### 14.1 Leaking Provider Details Upward

如果 payment page 或 service 直接知道 provider-specific 细节，后面会越来越难维护。

### 14.2 Mixing Provider with Method

如果把：

- `VibeCash`
- `PayNow`

写成同一层，会让架构变乱。

正确做法是：

- `VibeCash` 是 provider
- `PayNow` 是 scheme

### 14.3 Treating Cash as “No Adapter Needed”

现金也必须被结构化，否则报表和对账会出问题。

---

## Conclusion

统一支付适配器设计的关键不是“给每个支付方式写一层封装”，而是：

- 明确 provider / method / scheme 三层
- 用 orchestrator 统一路由
- 用 adapter 封装每个 provider 的复杂性

对于当前系统：

- `DCS` 应作为 card payment adapter
- `VibeCash` 应作为 QR gateway adapter
- `Cash` 应作为 manual payment adapter

这样后面支付、退款、对账、报表、AI-ready 扩展才能真正站稳。 
