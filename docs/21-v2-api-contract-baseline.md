# V2 API Contract Baseline

## Goal

定义 Restaurant POS `v2 foundation` 的第一批核心 API contract，作为后端实现和四端接入的统一接口基线。

本文件只覆盖 V2 第一阶段必须先稳定的 3 组接口：

1. Active Table Order
2. QR Ordering
3. Cashier Settlement

目标不是一次性定义全量 API，而是先把交易主干打稳。

---

## Design Principles

### 1. One Active Table Order per Table
- 同一时间一张桌只允许一张活动单
- POS 与 QR 共编辑这张活动单

### 2. API Names Must Follow Domain Language
不要用临时名字或 UI 导向名字，统一使用：
- `active-table-order`
- `qr-ordering`
- `cashier-settlement`

### 3. Status Terms Must Match Terminology Document
订单状态统一：
- `DRAFT`
- `SUBMITTED`
- `PENDING_SETTLEMENT`
- `SETTLED`

### 4. Settlement Is a Separate Domain
- 结账 API 不能只是“改订单状态”
- 结账要显式生成 settlement result

### 5. V2 APIs Must Be AI-Ready
- 当前第一批交易 API 仍以人驱动为主
- 但 API 设计必须预留 AI 协作能力
- 后续 CRM、Promotion、Report、Platform API 必须支持：
  - configuration
  - recommendation
  - execution
  - approval
  - audit

---

## API Group 1: Active Table Order

这组 API 服务于：
- POS 点桌后查看当前桌单
- cashier 改单
- QR 与 POS 共编辑当前桌单
- 送厨和推进状态

### 1.1 Get Active Table Order

`GET /api/v2/stores/{storeId}/tables/{tableId}/active-order`

作用：
- 获取当前桌活动单

返回：
- 若存在活动单，返回完整活动单
- 若不存在，返回 `null` 或 `404`，二选一

建议：
- V2 统一返回 `200 + data: null`

Response shape:

```json
{
  "data": {
    "id": "ato_001",
    "orderNo": "ATO202603240001",
    "storeId": 1001,
    "tableId": 12,
    "tableCode": "T12",
    "orderSource": "POS",
    "diningType": "DINE_IN",
    "status": "DRAFT",
    "kitchenStatus": "NOT_SUBMITTED",
    "member": {
      "id": "mem_001",
      "name": "Alice Tan",
      "tier": "Gold"
    },
    "items": [],
    "pricing": {
      "originalAmountCents": 0,
      "memberDiscountCents": 0,
      "promotionDiscountCents": 0,
      "payableAmountCents": 0
    },
    "createdAt": "2026-03-24T18:00:00+08:00",
    "updatedAt": "2026-03-24T18:00:00+08:00"
  }
}
```

### 1.2 Create or Merge Active Table Order

`POST /api/v2/stores/{storeId}/tables/{tableId}/active-order`

作用：
- POS 点单时创建或合并当前桌活动单
- QR 提交也可走同一底层用例

Request shape:

```json
{
  "orderSource": "POS",
  "diningType": "DINE_IN",
  "cashierId": 2001,
  "memberId": null,
  "items": [
    {
      "skuId": 301,
      "quantity": 2,
      "remark": "Less ice",
      "selectedOptions": [
        { "optionGroupId": 11, "optionValueId": 102 }
      ]
    }
  ]
}
```

规则：
- 如果该桌没有活动单，则创建
- 如果已有活动单，则按 merge policy 合并
- merge policy 默认按 `sku + option snapshot` 聚合数量

### 1.3 Replace Active Table Order Items

`PUT /api/v2/stores/{storeId}/tables/{tableId}/active-order/items`

作用：
- cashier 在 POS 中直接重算并保存当前桌购物车

规则：
- 这是“当前活动单全量覆盖更新”
- 用于 POS 编辑页的稳定保存

Request shape:

```json
{
  "memberId": "mem_001",
  "items": [
    {
      "skuId": 301,
      "quantity": 1,
      "remark": "No onion",
      "selectedOptions": []
    },
    {
      "skuId": 509,
      "quantity": 2,
      "remark": "",
      "selectedOptions": []
    }
  ]
}
```

### 1.4 Send Active Order to Kitchen

`POST /api/v2/active-table-orders/{activeOrderId}/submit-to-kitchen`

作用：
- 把订单从 `DRAFT` 推进到 `SUBMITTED`

规则：
- 仅 `DRAFT` 允许送厨
- 送厨后写入 `order_events`

Response:

```json
{
  "data": {
    "activeOrderId": "ato_001",
    "status": "SUBMITTED",
    "kitchenStatus": "SUBMITTED"
  }
}
```

### 1.5 Move Active Order to Pending Settlement

`POST /api/v2/active-table-orders/{activeOrderId}/move-to-settlement`

作用：
- 把订单推进到 `PENDING_SETTLEMENT`

规则：
- 通常由 cashier 操作
- 状态切换必须留事件记录

### 1.6 Clear Empty Draft Order

`DELETE /api/v2/stores/{storeId}/tables/{tableId}/active-order`

作用：
- 当当前桌购物车删空且订单仍是 `DRAFT` 时，可清掉活动单

规则：
- 只允许删除空的 `DRAFT`
- 不允许删除已 `SUBMITTED` 或 `PENDING_SETTLEMENT`

---

## API Group 2: QR Ordering

这组 API 面向顾客扫码点餐 H5。

### 2.1 Get QR Ordering Context

`GET /api/v2/qr-ordering/context?storeCode=1001&tableCode=T12`

作用：
- 扫码后获取门店、桌台、菜单摘要、当前活动单摘要

返回内容建议包括：
- store info
- table info
- categories
- sku highlights
- current active order summary

### 2.2 Submit QR Items to Active Table Order

`POST /api/v2/qr-ordering/submit`

作用：
- 顾客把新点菜品提交到当前桌活动单

Request shape:

```json
{
  "storeCode": "1001",
  "tableCode": "T12",
  "memberId": "mem_001",
  "items": [
    {
      "skuId": 301,
      "quantity": 1,
      "remark": "Less ice",
      "selectedOptions": []
    }
  ]
}
```

规则：
- 若该桌没有活动单，则创建 `DRAFT` 或直接 `SUBMITTED`
- 若已有活动单，则 merge
- 第一期建议：
  - QR 提交默认进入 `DRAFT`
  - 或按业务决定直接 `SUBMITTED`

建议先采用：
- **QR 提交后直接 `SUBMITTED`**
因为顾客已经完成点菜动作，更符合餐饮现场

### 2.3 Get Current QR Active Order

`GET /api/v2/qr-ordering/current?storeCode=1001&tableCode=T12`

作用：
- 顾客查看当前桌已点内容

返回：
- 当前桌活动单摘要
- 商品项
- 当前优惠拆解

---

## API Group 3: Cashier Settlement

这组 API 面向 POS 的最终收款流程。

### 3.1 Get Settlement Preview

`GET /api/v2/active-table-orders/{activeOrderId}/settlement-preview`

作用：
- 给 cashier 展示正式结账预览

必须返回：
- 原价合计
- 会员优惠
- 促销优惠
- 赠品
- 应付金额
- 当前状态

Response shape:

```json
{
  "data": {
    "activeOrderId": "ato_001",
    "status": "PENDING_SETTLEMENT",
    "member": {
      "id": "mem_001",
      "name": "Alice Tan",
      "tier": "Gold"
    },
    "pricing": {
      "originalAmountCents": 8800,
      "memberDiscountCents": 400,
      "promotionDiscountCents": 800,
      "payableAmountCents": 7600
    },
    "giftItems": [
      { "skuName": "Coke", "quantity": 1 }
    ]
  }
}
```

### 3.2 Submit Cashier Settlement

`POST /api/v2/cashier-settlements`

作用：
- cashier 对活动单发起正式结账

Request shape:

```json
{
  "activeOrderId": "ato_001",
  "cashierId": 2001,
  "shiftId": "shift_001",
  "paymentMethod": "CASH",
  "paidAmountCents": 7600,
  "remark": "Paid at front desk"
}
```

规则：
- 只允许 `PENDING_SETTLEMENT` 状态结账
- 成功后：
  - 生成 `settlement_record`
  - 更新订单为 `SETTLED`
  - 清桌
  - 生成支付记录
  - 触发打印/GTO 待同步事件

### 3.3 Get Settlement Result

`GET /api/v2/cashier-settlements/{settlementId}`

作用：
- 获取结账结果

返回：
- settlement info
- payment info
- order info
- print status

---

## Shared Response Model

建议 V2 所有接口统一返回：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {}
}
```

建议从 V2 开始在可变更类接口中预留元信息：

```json
{
  "meta": {
    "actorType": "HUMAN",
    "actorId": "cashier_2001",
    "decisionSource": "POS_UI"
  }
}
```

未来 AI 调用时可扩展为：

```json
{
  "meta": {
    "actorType": "AI",
    "actorId": "ops-agent",
    "decisionSource": "AI_RECOMMENDATION",
    "aiRecommendationId": "rec_001"
  }
}
```

错误返回：

```json
{
  "success": false,
  "code": "ACTIVE_ORDER_NOT_FOUND",
  "message": "No active table order found",
  "data": null
}
```

---

## Error Code Baseline

第一批建议统一这些错误码：
- `ACTIVE_ORDER_NOT_FOUND`
- `TABLE_NOT_FOUND`
- `INVALID_ORDER_STATUS`
- `EMPTY_ORDER_NOT_ALLOWED`
- `SETTLEMENT_NOT_ALLOWED`
- `SKU_NOT_AVAILABLE`
- `MEMBER_NOT_FOUND`
- `PROMOTION_EVALUATION_FAILED`

对于 AI-ready 变更类接口，后续建议补充：
- `APPROVAL_REQUIRED`
- `AI_ACTION_NOT_ALLOWED`
- `AI_RECOMMENDATION_NOT_FOUND`
- `INVALID_DECISION_SOURCE`

---

## State Transition Rules

### Allowed
- `DRAFT -> SUBMITTED`
- `DRAFT -> PENDING_SETTLEMENT`
- `SUBMITTED -> PENDING_SETTLEMENT`
- `PENDING_SETTLEMENT -> SETTLED`

### Not Allowed
- `SETTLED -> DRAFT`
- `SETTLED -> SUBMITTED`
- `SUBMITTED -> DRAFT` without explicit rollback flow

---

## AI-Ready Extension Pattern

V2 第一批交易 API 不额外拆成 AI 接口，但从 contract 设计上要预留扩展方式。

后续每个核心 domain 建议统一支持：

### 1. Configuration API
人工或系统直接配置对象。

### 2. Recommendation API
AI 只输出建议，不直接落地。

### 3. Execution API
人工或 AI 发起实际执行。

### 4. Approval API
高风险动作需要人工审核。

### 5. Audit API
所有配置与执行都要能追踪来源。

示意：

```text
/api/v2/member/tier-rules
/api/v2/member/tier-rules/recommendations
/api/v2/member/tier-rules/{id}/apply
/api/v2/member/tier-rules/{id}/approve
/api/v2/member/tier-rules/{id}/audit
```

这条规则虽然暂时不会完整落地到第一批交易 API，但应作为 V2 之后所有新 domain API 的统一设计原则。

---

## Immediate Next Step

API baseline确定后，下一步应继续：

1. V2 backend bootstrap structure
2. First 3 migration files
3. Order domain interfaces and command model

---

## Final Position

V2 的 API 不应再围绕“页面上当前刚好要点哪个按钮”来定义。

应围绕：
- active table order
- qr ordering
- cashier settlement

这三条交易主线建立稳定 contract。
