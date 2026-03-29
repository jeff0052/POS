# Settlement Flow Design

## Goal

设计一套统一的结算流程，覆盖以下场景：

- 非会员
- 会员但不用积分 / 储值 / coupon
- 会员使用 coupon
- 会员使用积分抵扣
- 会员使用储值余额
- 会员混合使用 coupon + 积分 + 储值 + 外部支付

这套流程的目标不是只画分支，而是定义一条可以落地到后端与前端的主链路。

## Design Principles

1. 结算聚合根使用 `table_session`。
2. 结算分两阶段：`calculate` 与 `confirm`。
3. 会员权益分两类：
   - 价格类权益：会员等级折扣、促销、coupon
   - 支付类权益：积分抵扣、储值余额
4. 所有支付类权益都必须先 `hold`，再 `commit`，失败时可 `release`。
5. 外部支付只负责支付剩余金额，不直接改写会员余额。

## Unified Settlement Order

默认结算顺序：

1. 原始订单金额 `gross_amount`
2. 会员等级折扣 `member_discount`
3. 自动促销 `promotion_discount`
4. coupon 减免 `coupon_discount`
5. 积分抵扣 `points_deduct_amount`
6. 储值抵扣 `cash_balance_amount`
7. 外部支付 `external_payment_amount`

说明：

- 1-4 属于“价格计算阶段”。
- 5-7 属于“支付分摊阶段”。
- `coupon -> 积分 -> 储值 -> 外部支付` 作为默认叠加顺序。
- 如果后续支持门店可配置规则，可把 4-6 的顺序抽成 `payment_stacking_rule`。

## End-to-End Flow

### Phase A. Enter Settlement

1. 收银员点击结账。
2. 后端加载 `table_session` 下所有 `UNPAID submitted_orders`。
3. 校验：
   - 没有未提交的草稿单
   - session 仍是 `OPEN`
   - 当前没有已完成 settlement record
4. 汇总订单，生成 `SettlementIntent` 草稿。

### Phase B. Build Pricing Preview

1. 计算 `gross_amount`：
   - 堂食 / buffet / surcharge / overtime fee 都先汇总成原始应收
2. 判断是否绑定会员：
   - 否：跳过会员权益加载
   - 是：加载 `member`, `member_accounts`, 可用 coupon、积分、储值
3. 应用会员等级折扣。
4. 应用自动促销。
5. 输出第一版 preview：
   - gross
   - member_discount
   - promotion_discount
   - subtotal_after_pricing

### Phase C. Benefit Selection

前端展示以下可选项：

- 不使用任何会员支付能力
- 使用 1 张 coupon
- 使用积分
- 使用储值
- 混合使用

会员分支规则：

- 非会员：
  - 不展示积分 / 储值 / coupon
- 会员但无 coupon：
  - coupon 区域灰掉
- 会员但积分不足：
  - 积分区展示不可用原因
- 会员但储值不足：
  - 允许部分抵扣，剩余走外部支付

### Phase D. Recalculate With Selection

1. 用户选择 coupon 后：
   - 校验有效期
   - 校验门店适用范围
   - 校验 dining mode
   - 校验最低消费
   - 校验是否已占用 / 已使用
   - 计算 `coupon_discount`
2. 用户选择积分后：
   - 读取 `points_deduction_rules`
   - 校验最低订单金额
   - 校验最小可抵扣积分
   - 校验最大抵扣比例
   - 按可用积分计算 `points_deduct_amount`
3. 用户选择储值后：
   - 读取 `cash_balance_rules`
   - 校验最低可用余额
   - 校验最大支付比例
   - 计算 `cash_balance_amount`
4. 计算剩余外部支付金额：
   - `external_payment_amount = payable_after_coupon - points - cash_balance`
5. 返回第二版 preview：
   - coupon_discount
   - points_deduct_amount
   - points_used
   - cash_balance_amount
   - external_payment_amount
   - final_payable

## Hold / Commit Model

### Why

积分、储值、coupon 都不是“点一下就直接扣”的资源。
必须先冻结，成功后提交，失败后释放。

### Hold Phase

在用户点击“确认支付”后，后端创建：

- `SettlementIntent`
- `CouponHold`
- `PointsHold`
- `CashBalanceHold`

规则：

- coupon：标记为 `LOCKED`
- 积分：从 `available_points` 转到 `frozen_points`
- 储值：从 `available_cash_cents` 转到 `frozen_cash_cents`

如果任一 hold 失败：

- 终止结算
- 回滚之前已成功的 hold
- 返回前端重新选择

### Confirm Phase

1. 如果 `external_payment_amount = 0`：
   - 直接进入内部确认
2. 如果 `external_payment_amount > 0`：
   - 发起外部支付
   - 等待成功回调 / 收银确认
3. 支付成功后：
   - coupon `LOCKED -> USED`
   - 积分 `frozen -> committed`
   - 储值 `frozen -> committed`
   - 写积分流水 / 储值流水 / coupon 使用记录
   - 写 settlement record
   - 更新 submitted orders = `SETTLED`
   - `table_session = CLOSED`
   - 桌台进入 `PENDING_CLEAN` 或 `AVAILABLE`

### Release Phase

以下场景统一执行 release：

- 外部支付失败
- 用户取消支付
- 支付超时
- 收银改用另一种方式

释放动作：

- coupon `LOCKED -> AVAILABLE`
- 积分 `frozen -> available`
- 储值 `frozen -> available`
- SettlementIntent 标记为 `FAILED` / `CANCELLED`

## Branches

### 1. Non-member

1. 汇总订单金额
2. 应用促销
3. 无积分、无储值、无 coupon
4. 全额走外部支付
5. 成功后直接结算完成

### 2. Member, No Coupon, No Points, No Cash Balance

1. 汇总订单金额
2. 应用会员等级折扣
3. 应用促销
4. 不进入 hold
5. 全额走外部支付

### 3. Member + Coupon

1. 先做会员折扣、促销
2. 校验 coupon
3. 生成 coupon hold
4. 剩余金额走外部支付
5. 成功后 coupon used

### 4. Member + Points

1. 先做会员折扣、促销
2. 校验 points rules
3. 生成 points hold
4. 剩余金额走外部支付
5. 成功后 points commit

### 5. Member + Cash Balance

1. 先做会员折扣、促销
2. 校验 cash balance rules
3. 生成 cash hold
4. 剩余金额走外部支付或为 0
5. 成功后 cash commit

### 6. Member + Coupon + Points + Cash Balance

1. 汇总 gross
2. 应用 member discount
3. 应用 promotion
4. 应用 coupon
5. 应用 points
6. 应用 cash balance
7. 剩余金额走外部支付
8. 成功后统一 commit
9. 任一步失败统一 release

## Recommended API Sequence

### 1. Preview

`GET /api/v2/stores/{storeId}/tables/{tableId}/settlement/preview`

返回：

- gross_amount
- member_discount
- promotion_discount
- member summary
- available coupon list
- available points
- available cash balance

### 2. Calculate

`POST /api/v2/stores/{storeId}/tables/{tableId}/settlement/calculate`

Body:

```json
{
  "memberId": 1001,
  "couponId": 2001,
  "usePoints": true,
  "pointsToUse": 500,
  "useCashBalance": true,
  "cashBalanceAmountCents": 1500
}
```

返回：

- settlementIntentId
- coupon_discount
- points_used
- points_deduct_amount
- cash_balance_amount
- external_payment_amount
- final_payable

### 3. Confirm

`POST /api/v2/stores/{storeId}/tables/{tableId}/settlement/confirm`

Body:

```json
{
  "settlementIntentId": "SETI_001",
  "paymentMethod": "CASH"
}
```

### 4. Retry / Switch / Cancel

- `POST /api/v2/settlements/intents/{intentId}/retry-payment`
- `POST /api/v2/settlements/intents/{intentId}/switch-method`
- `POST /api/v2/settlements/intents/{intentId}/cancel`

## State Machine

### SettlementIntent

- `DRAFT`
- `CALCULATED`
- `HOLD_CREATED`
- `AWAITING_EXTERNAL_PAYMENT`
- `PAYMENT_SUCCEEDED`
- `SETTLED`
- `FAILED`
- `CANCELLED`

### Coupon

- `AVAILABLE`
- `LOCKED`
- `USED`
- `EXPIRED`

### Points / Cash Hold

- `NONE`
- `HELD`
- `COMMITTED`
- `RELEASED`

## Failure Handling

### Coupon invalid after selection

- 重新 calculate
- 移除 coupon
- 前端提示重新选择

### Points or cash hold failure

- 终止 confirm
- 已成功 hold 的资源全部释放

### External payment failed

- 保留 intent
- 资源不直接 commit
- 允许 retry / switch method / cancel

### User cancels

- 全部 release
- intent = `CANCELLED`

## Recommended Frontend UX

收银台结账页按 4 个区块展示：

1. 订单金额区
   - 原始金额
   - 会员折扣
   - 促销减免
2. 会员权益区
   - coupon
   - 积分
   - 储值
3. 支付分摊区
   - coupon 减免
   - 积分抵扣
   - 储值支付
   - 外部支付剩余
4. 确认区
   - 支付方式选择
   - 支付状态
   - retry / switch / cancel

## Recommended Back-End Ownership

- `SettlementPreviewService`
  - 汇总订单、会员、促销、可用权益
- `SettlementCalculationService`
  - coupon / points / cash balance 试算
- `SettlementHoldService`
  - 创建和释放 hold
- `SettlementConfirmService`
  - commit 资源并落结算记录
- `ExternalPaymentService`
  - 外部支付、回调、重试

## Final Recommendation

最稳妥的落地方式是：

1. 先保留当前 table/session 结账主链路。
2. 在其上新增 `preview -> calculate -> confirm -> retry/switch/cancel`。
3. 把会员权益分成“价格类”和“支付类”两层处理。
4. 所有支付类权益都强制走 hold/commit/release。

这样既能覆盖非会员、会员、积分、储值、coupon 的完整场景，也能和现有系统平滑对接。
