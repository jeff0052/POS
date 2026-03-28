# Payment Integration Requirements

## Purpose

本文档定义 Restaurant POS 的支付接入需求范围，用于回答：

- 一期要接哪些支付方式
- 每种支付方式做到什么深度
- 每种支付方式由哪个端发起
- 统一支付域需要提供哪些能力
- 当前阶段哪些能力必须完成，哪些可以后补

本文件是：

- 产品范围文档
- 技术接入边界文档
- 支付一期验收基线

---

## Executive Summary

当前系统已经完成：

- ordering
- payment page / collect payment flow
- settlement preview
- settlement status progression

但尚未完成：

- 真实支付通道接入
- 多 provider 统一支付域落地
- 支付回调、支付查询、退款与对账的统一闭环

因此，一期支付接入目标不是“所有支付方式一次到位”，而是：

**建立统一支付域，并完成最关键的 3 类 payment providers 接入：**

1. `DCS Card Terminal`
2. `VibeCash QR Gateway`
3. `Cash`

---

## 1. Payment Integration Goal

一期支付接入的目标：

1. POS 可以选择不同支付方式
2. 不同支付方式统一进入同一个 payment domain
3. 支付成功后统一推进订单与桌台状态
4. 支付记录、provider reference、结果状态可追溯
5. 为退款、对账、AI-ready 支付分析打基础

---

## 2. Payment Scope

## 2.1 In Scope for Phase 1

### DCS Card Terminal

支持：

- card sale
- terminal-side scan payment
- terminal-side QR code payment
- void
- refund
- settlement

### VibeCash QR Gateway

支持：

- create payment
- start payment
- payment result synchronization
- basic refund

### Cash

支持：

- manual cash payment confirmation
- cash payment record
- cashier attribution

### Shared Capabilities

支持：

- payment method selection
- unified payment intent
- unified payment attempt
- unified payment status
- payment preview
- payment success / failure handling
- refund record structure

## 2.2 Out of Scope for Phase 1

以下能力不作为一期支付接入前置通过条件：

- split tender
- partial capture
- installment
- tokenized recurring payment
- automatic retry routing across providers
- AI auto-execution of payment actions
- advanced reconciliation dashboard

---

## 3. Payment Provider / Method / Scheme Positioning

统一建议拆成三层：

### 3.1 Payment Provider

- `DCS`
- `VIBECASH`
- `CASH_MANUAL`

### 3.2 Payment Method

- `CARD`
- `QR`
- `CASH`

### 3.3 Payment Scheme

- `VISA`
- `MASTERCARD`
- `AMEX`
- `JCB`
- `WECHAT_PAY`
- `ALIPAY`
- `PAYNOW`
- `GRABPAY`
- `SHOPEEPAY`

## 3.4 DCS Card Terminal

Type:

- device SDK
- terminal / in-store / card-present

Primary use cases:

- POS cashier card payment

Entry end:

- Android POS

Key requirement:

- POS must not directly bind business logic to SDK callback semantics
- all DCS-specific details must stay inside adapter layer
- normalize all card networks under:
  - `provider = DCS`
  - `method = CARD`

## 3.5 VibeCash

Type:

- QR gateway provider
- online or wallet-style payment flow

Primary use cases:

- POS cashier-initiated QR payment
- customer-facing QR payment in future expansion

Entry end:

- POS
- potentially QR / Merchant Admin in future phases

Key requirement:

- normalize VibeCash transaction identifiers, status, and error model into unified payment domain
- normalize all QR brands under:
  - `provider = VIBECASH`
  - `method = QR`
  - `scheme = WECHAT_PAY / ALIPAY / PAYNOW / GRABPAY / SHOPEEPAY / ...`

## 3.6 Cash

Type:

- manual in-store payment

Primary use cases:

- cashier cash collection

Entry end:

- POS

Key requirement:

- record payment attempt and cashier attribution
- support later reconciliation
- normalize as:
  - `provider = CASH_MANUAL`
  - `method = CASH`
  - `scheme = CASH`

---

## 4. Required Business Flows

## 4.1 POS Card Payment

Flow:

1. Cashier enters payment stage
2. POS selects `Card Terminal`
3. System creates `PaymentIntent`
4. Payment orchestrator routes to `DCS Adapter`
5. DCS transaction completes
6. System creates `PaymentAttempt`
7. Settlement is marked success
8. Table session is closed

## 4.2 POS VibeCash QR Payment

Flow:

1. Cashier enters payment stage
2. POS selects `QR`
3. POS selects QR brand / scheme
4. System creates `PaymentIntent`
5. Payment orchestrator routes to `VibeCash Adapter`
6. External provider payment completes
7. Callback or query confirms final status
8. System creates `PaymentAttempt`
9. Settlement is marked success

## 4.3 POS Cash Payment

Flow:

1. Cashier enters payment stage
2. POS selects `Cash`
3. Cashier confirms cash received
4. System creates `PaymentAttempt`
5. Settlement is marked success

## 4.4 Refund

一期要求支持：

- payment-based refund request
- refund status persistence
- refund result query

说明：

- DCS 与 VibeCash 的退款实现细节可以不同
- 但业务层都必须统一落到 `RefundRecord`

---

## 5. Payment Domain Requirements

一期统一支付域至少需要：

### 5.1 PaymentIntent

必须有：

- intent id
- settlement id
- store id
- amount
- method
- provider
- status

### 5.2 PaymentAttempt

必须有：

- attempt id
- intent id
- provider transaction identifiers
- provider raw response snapshot
- normalized status
- failure code / message

### 5.3 RefundRecord

必须有：

- refund id
- payment attempt reference
- refund amount
- refund status
- provider refund reference

### 5.4 Audit Trail

必须有：

- actor type
- actor id
- provider
- created at
- updated at
- change reason

---

## 6. Normalized Payment Status

一期统一状态建议：

- `CREATED`
- `PENDING`
- `AUTHORIZED`
- `CAPTURED`
- `FAILED`
- `VOIDED`
- `REFUND_PENDING`
- `REFUNDED`

业务层只看统一状态，不直接看 provider 原始状态。

---

## 7. UI and Interaction Requirements

### POS Payment Page

一期必须支持：

- 选择支付方式
- 对 `QR` 方式进一步选择 QR brand / scheme
- 展示 payment preview
- 发起支付
- 展示支付中 / 成功 / 失败
- 支持退款入口预留

### Merchant Admin

一期必须支持查看：

- payment method
- payment provider
- payment status
- payment amount
- refund status
- provider reference

### QR

当前阶段不要求直接接支付，但后续应可复用统一 payment domain。

---

## 8. Provider-Specific Integration Requirements

## 8.1 DCS

必须满足：

- SDK connect / init / config
- sale
- void
- refund
- settlement
- query local transaction if needed

### Acceptance

- Android POS can complete one successful DCS transaction
- POS receives normalized payment result
- settlement and table release are correct

## 8.2 VibeCash

必须满足：

- create payment request
- support multiple QR brands behind the same gateway
- start payment flow
- sync final payment result
- basic refund support

### Acceptance

- POS can complete one successful VibeCash payment
- provider transaction id is persisted
- final settlement state is correct

## 8.3 Cash

必须满足：

- cashier can confirm cash collection
- payment attempt is persisted
- payment method is distinguishable in reporting

### Acceptance

- one settlement can be completed using cash
- cashier attribution is recorded

---

## 9. Risks and Design Constraints

### 9.1 Do Not Bind Product Flow to DCS SDK

POS 页面不能直接以 DCS callback 作为业务状态真相。

### 9.2 VibeCash Details Must Be Abstracted

VibeCash 的 token、callback、HTTP status、provider error code 都要收敛到统一 payment model。

### 9.3 Cash Must Still Be Structured

Cash 不是“没有支付系统”，而是一个 manual payment provider。

### 9.4 Refund Must Be Payment-Based

退款必须围绕 payment / settlement 做，不能只围绕 UI 按钮。

---

## 10. Phase 1 Acceptance Criteria

一期支付接入通过标准：

1. POS 至少支持 3 种支付方式：
   - `Card Terminal`
   - `QR`
   - `Cash`
2. `QR` 方式必须通过 `VibeCash` gateway 落到统一 `PaymentIntent / PaymentAttempt` 模型
3. `Card Terminal` 方式必须通过 `DCS` 落到统一 `PaymentIntent / PaymentAttempt` 模型
4. `Cash` 方式必须通过 manual provider 落到统一 `PaymentIntent / PaymentAttempt` 模型
5. 支付成功后能统一推进：
   - settlement success
   - table release
6. Merchant Admin 能查看统一支付结果
7. 至少支持基础退款结构与 provider reference 保存

不通过情况：

- 业务代码直接绑定某个 provider SDK
- 不同支付方式没有统一状态模型
- payment success 后桌台或 settlement 状态不一致
- provider transaction reference 无法追踪

---

## 11. Recommended Next Step

建议紧接着补：

1. `Payment Adapter Design`
2. `Payment Status Mapping Matrix`
3. `Refund Integration Requirements`

---

## Conclusion

一期支付接入不应理解成：

- “把 DCS 接进来”

而应理解成：

- “先完成统一支付域的一期能力，并接入最关键的三个 payment providers”

也就是：

- `DCS` 作为 card payment provider
- `VibeCash` 作为 QR gateway provider
- `Cash` 作为 manual provider

这样后面系统才能在支付、退款、报表、对账、AI-ready 分析上保持统一。 
