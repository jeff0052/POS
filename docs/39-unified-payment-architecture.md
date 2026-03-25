# Unified Payment Architecture

## Purpose

本文档定义 Restaurant POS 的统一支付架构，用来承接多种支付方式，而不是让业务层直接依赖某一个支付 SDK 或某一套单独接口。

当前已知支付能力至少包含：

- `DCS`
- `VibeCash`
- `Cash`

后续还可能扩展：

- `PayNow`
- `Stripe`
- `Bank Transfer`
- 其他本地钱包或终端支付渠道

因此，系统需要从“单一支付接入”升级成“统一支付域 + 多支付适配器”。

---

## Executive Summary

### Wrong direction

如果系统直接把支付流程绑定到：

- 某一个 Android 终端 SDK
- 某一个 HTTP 支付 API

后面会出现：

- POS 页面与支付厂商 SDK 强耦合
- QR / Merchant Admin / Platform Admin 无法共享统一支付语义
- 退款、对账、状态回调、审计日志难以统一
- 以后新增支付方式时需要大改业务代码

### Recommended direction

系统应采用：

- `Payment Domain`
- `Payment Orchestrator`
- `Provider Adapter`

业务层只面向统一支付接口，底层再路由到：

- `DCS Card Adapter`
- `VibeCash QR Gateway Adapter`
- `Cash Adapter`
- 其他 provider adapter

---

## 1. Design Goal

统一支付架构要解决 5 个问题：

1. 用同一套业务语义管理不同支付方式
2. 支持 POS、QR、Merchant Admin 共用支付结果
3. 支持支付、撤销、退款、查询、对账
4. 支持设备 SDK 和 HTTP API 两类完全不同的 provider
5. 为 AI-ready 支付建议、风险检测、审批、审计预留统一挂点

---

## 2. Core Principles

### 2.1 Payment is a Domain, not a SDK wrapper

支付不是某一个 SDK 的调用过程，而是一个独立业务域。

### 2.2 Business code should not depend on provider details

业务层只知道：

- 订单
- 付款金额
- 支付方式
- 支付结果

不应该知道：

- DCS 的服务绑定
- VibeCash 的 token / callback
- 第三方返回的原始字段细节

### 2.3 Provider-specific flows live in adapters

每个支付渠道自己的复杂性，都应该被收敛在 adapter 中。

### 2.4 Payment must be auditable

所有支付动作都应有：

- 来源
- 发起人
- provider
- 请求结果
- 状态变更
- 失败原因

### 2.5 Payment status must be normalized

不同 provider 的状态要收敛成统一状态，而不是让前端直接面对 provider 原始值。

---

## 3. Proposed Architecture

### 3.1 Layers

#### Payment Domain

统一定义：

- `PaymentIntent`
- `PaymentAttempt`
- `PaymentMethod`
- `PaymentProvider`
- `PaymentScheme`
- `PaymentStatus`
- `Refund`
- `Void`
- `Settlement`

#### Payment Orchestrator

负责：

- 根据支付方式选择 provider
- 创建支付意图
- 发起支付
- 接收回调/轮询结果
- 更新统一支付状态
- 通知订单 / 结算域

#### Provider Adapters

负责对接具体渠道：

- `DcsPaymentAdapter`
- `VibeCashAdapter`
- `CashPaymentAdapter`

#### Channel Facades

不同端通过 facade 使用支付能力：

- POS facade
- QR facade
- Merchant Admin facade

---

## 4. Recommended Core Model

### 4.1 PaymentIntent

表示一笔待支付意图。

Recommended fields:

- `payment_intent_id`
- `merchant_id`
- `store_id`
- `table_session_id`
- `settlement_id`
- `amount_cents`
- `currency_code`
- `payment_method_code`
- `payment_provider_code`
- `payment_scheme_code`
- `status`
- `created_by_type`
- `created_by_id`
- `created_at`
- `expired_at`

### 4.2 PaymentAttempt

表示某一次实际支付尝试。

Recommended fields:

- `payment_attempt_id`
- `payment_intent_id`
- `provider_code`
- `provider_transaction_id`
- `provider_reference_no`
- `provider_voucher_no`
- `provider_rrn`
- `request_payload_snapshot`
- `response_payload_snapshot`
- `status`
- `error_code`
- `error_message`
- `started_at`
- `finished_at`

### 4.3 RefundRecord

- `refund_id`
- `payment_attempt_id`
- `amount_cents`
- `status`
- `provider_refund_id`
- `reason`
- `created_by_type`
- `created_by_id`

### 4.4 PaymentStatus

统一建议：

- `CREATED`
- `PENDING`
- `AUTHORIZED`
- `CAPTURED`
- `FAILED`
- `VOIDED`
- `REFUND_PENDING`
- `REFUNDED`

---

## 5. Provider Mapping

### 5.1 DCS Payment SDK

类型：

- Android terminal SDK
- service binding
- callback/state-machine style

适合：

- card-present payment
- refund / void / settlement on device

Adapter responsibilities:

- 连接服务
- 初始化国家/币种/商户参数
- 执行卡交易、退款、撤销、批结算
- 将 DCS 原始结果映射到统一 `PaymentAttempt`

Recommended normalized mapping:

- `provider = DCS`
- `method = CARD`
- `scheme = VISA / MASTERCARD / AMEX / JCB / ...`

### 5.2 VibeCash

类型：

- QR payment gateway provider
- HTTP API / wallet / online payment style

适合：

- QR payment methods behind one gateway
- wallet / balance / online payment
- QR redirect / remote confirmation flow

Adapter responsibilities:

- 创建支付请求
- 跟踪 provider transaction
- 接 webhook / callback
- 回写统一支付状态

Recommended normalized mapping:

- `provider = VIBECASH`
- `method = QR`
- `scheme = WECHAT_PAY / ALIPAY / PAYNOW / GRABPAY / SHOPEEPAY / ...`

### 5.3 Cash

类型：

- manual payment provider

Adapter responsibilities:

- 无第三方 provider 交互
- 直接记录人工确认的 payment attempt
- 写入审计日志

Recommended normalized mapping:

- `provider = CASH_MANUAL`
- `method = CASH`
- `scheme = CASH`

---

## 6. Provider / Method / Scheme Model

建议支付统一拆成三层：

### Payment Provider

表示谁在提供支付接入能力：

- `DCS`
- `VIBECASH`
- `CASH_MANUAL`

### Payment Method

表示业务层看到的支付形态：

- `CARD`
- `QR`
- `CASH`

### Payment Scheme

表示具体支付网络或钱包品牌：

- `VISA`
- `MASTERCARD`
- `AMEX`
- `JCB`
- `WECHAT_PAY`
- `ALIPAY`
- `PAYNOW`
- `GRABPAY`
- `SHOPEEPAY`

### Why this matters

这样设计之后：

- Provider 可以换
- Method 可以保持稳定
- Scheme 可以继续扩

也方便：

- 报表
- 对账
- 退款
- 风控
- AI-ready 分析

---

## 7. UI and Product Impact

POS 的 Payment 页面不应再只表现为一个“Collect payment”按钮。

正确结构应该是：

1. 选择支付方式
   - `Card Terminal`
   - `VibeCash`
   - `Cash`
2. 展示该方式对应的支付步骤
3. 展示支付结果
4. 只有支付成功后才推进 `SETTLED`

### Example

#### Card Terminal

- 进入 DCS adapter flow
- 等待刷卡 / 插卡 / 扫码
- 成功后更新 PaymentAttempt

#### VibeCash

- 创建 provider payment session
- 等待外部确认或回调
- 成功后更新 PaymentAttempt

#### Cash

- 直接人工确认收款
- 记录 cash payment attempt

---

## 8. Integration with Current POS Architecture

### 7.1 Relationship with Table Session

支付应挂在：

- `table_session`
- `settlement`

而不是直接挂在某一张 draft order 上。

因为最终支付的是：

- 当前 table session 下
- 所有应付的 submitted orders 汇总

### 7.2 Relationship with Ordering

Ordering 负责：

- 形成 submitted orders

Payment 负责：

- 对 submitted orders 的汇总金额发起支付

### 7.3 Relationship with Merchant Admin

Merchant Admin 不负责发起 terminal SDK 支付，但要能查看：

- payment intent
- payment attempt
- payment provider
- refund
- void
- provider reference

### 7.4 Relationship with Reports

Reports 不直接读 provider 原始返回，而应读统一 payment facts。

---

## 9. Recommended Adapter Interface

建议统一 provider adapter contract：

### createPaymentIntent

输入：

- settlement / order context
- amount
- payment method

输出：

- `PaymentIntent`

### startPayment

输入：

- `paymentIntentId`
- provider-specific params

输出：

- `PaymentAttempt`

### queryPayment

输入：

- `paymentAttemptId`

输出：

- normalized payment status

### voidPayment

输入：

- original payment reference

输出：

- updated payment status

### refundPayment

输入：

- original payment reference
- refund amount

输出：

- refund result

---

## 10. AI-Ready Considerations

支付域后续也应支持 AI-ready 能力，但必须非常克制。

适合 AI 参与的：

- payment failure clustering
- suspicious retry detection
- settlement discrepancy detection
- provider routing suggestion
- refund risk suggestion

不适合 AI 自动执行的高风险动作：

- 自动退款
- 自动撤销
- 自动更换支付通道并重扣

因此支付域需要预留：

- `decision_source`
- `actor_type`
- `approval_status`
- `change_reason`
- `audit_log`

---

## 11. Recommended Next Step

建议下一步补两份配套文档：

1. `Payment Integration Requirements`
2. `Payment Adapter Design`

其中：

- `Payment Integration Requirements` 负责定义一期到底接哪些方式
- `Payment Adapter Design` 负责定义 DCS / VibeCash / Cash 的适配器接口

---

## Conclusion

对于当前系统来说：

- `DCS` 不是支付架构本身，而是 card payment provider
- `VibeCash` 不是支付架构本身，而是 QR gateway provider

它们都应该是：

- 统一支付域下面的 provider adapter

只有这样，系统才能：

- 同时支持多种支付方式
- 保持订单和结算语义一致
- 支持退款、对账、报表、审计
- 为未来 AI-ready 支付分析和建议保留统一挂点
