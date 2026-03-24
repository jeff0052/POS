# V2 CRM API Contract

## Goal

定义 Restaurant POS `v2 foundation` 的 CRM domain API contract。

这份 contract 不只是面向传统后台页面，也按 AI-ready 原则设计，支持：
- Human-driven configuration
- AI recommendation
- AI/Human execution
- Approval
- Audit

---

## CRM Domain Scope

CRM domain 负责：
- 会员资料
- 会员账户
- 积分
- 余额
- 充值
- 等级
- 权益
- 升级历史

CRM 不直接负责：
- 订单生命周期
- 促销规则中心
- 支付执行

但 CRM 会影响：
- 订单结算优惠
- 会员消费体验
- 复购和运营效果

---

## Design Position

CRM 是典型的 AI-ready domain。

原因：
- 规则复杂
- 高度依赖经营数据
- 既需要人工配置，也非常适合 AI 给建议
- 有中风险执行动作，适合加入审批流

因此 CRM API 从第一版开始就要按以下 5 类能力设计：

1. Configuration API
2. Recommendation API
3. Execution API
4. Approval API
5. Audit API

---

## API Group 1: Member Profile and Account

### 1.1 Get Member Detail

`GET /api/v2/members/{memberId}`

返回：
- 基本信息
- 当前等级
- 当前积分
- 当前余额
- 最近消费摘要

### 1.2 Search Members

`GET /api/v2/members?keyword=alice&storeId=1001`

支持：
- 手机号
- 姓名
- 会员编号
- 门店过滤

### 1.3 Create Member

`POST /api/v2/members`

Request:

```json
{
  "name": "Alice Tan",
  "phone": "91234567",
  "storeId": 1001,
  "source": "POS",
  "meta": {
    "actorType": "HUMAN",
    "actorId": "cashier_2001",
    "decisionSource": "POS_UI"
  }
}
```

### 1.4 Bind Member to Active Table Order

`POST /api/v2/members/{memberId}/bind-active-order`

作用：
- 在 POS / QR 点单流程中绑定会员

Request:

```json
{
  "activeOrderId": "ato_001",
  "meta": {
    "actorType": "HUMAN",
    "actorId": "cashier_2001",
    "decisionSource": "POS_UI"
  }
}
```

---

## API Group 2: Member Account and Ledger

### 2.1 Get Member Account

`GET /api/v2/members/{memberId}/account`

返回：
- points balance
- cash balance
- tier
- lifecycle stats

### 2.2 List Points Ledger

`GET /api/v2/members/{memberId}/points-ledger`

### 2.3 List Balance Ledger

`GET /api/v2/members/{memberId}/balance-ledger`

### 2.4 Recharge Member Balance

`POST /api/v2/members/{memberId}/recharges`

Request:

```json
{
  "amountCents": 5000,
  "giftAmountCents": 500,
  "storeId": 1001,
  "remark": "March recharge plan",
  "meta": {
    "actorType": "HUMAN",
    "actorId": "cashier_2001",
    "decisionSource": "POS_UI"
  }
}
```

规则：
- 必须写充值订单
- 必须写余额流水
- 赠送金额必须单独留痕

### 2.5 Adjust Member Points

`POST /api/v2/members/{memberId}/points-adjustments`

规则：
- 属于中风险动作
- 需要原因
- 可根据角色决定是否需要审批

---

## API Group 3: Tier and Benefit Configuration

### 3.1 List Member Tiers

`GET /api/v2/member-tiers`

### 3.2 Create Member Tier

`POST /api/v2/member-tiers`

### 3.3 Update Member Tier

`PUT /api/v2/member-tiers/{tierId}`

### 3.4 Configure Tier Upgrade Rules

`PUT /api/v2/member-tiers/{tierId}/upgrade-rules`

配置项可包含：
- total spending threshold
- recharge threshold
- points threshold
- rolling period

### 3.5 Configure Member Benefits

`PUT /api/v2/member-tiers/{tierId}/benefits`

配置项可包含：
- points multiplier
- exclusive sku set
- member price policy
- birthday perks

---

## API Group 4: AI Recommendation APIs

这组接口默认不直接改配置，只生成建议或草案。

### 4.1 Recommend Tier Structure

`POST /api/v2/member-tiers/recommendations`

输入：
- storeId / merchantId
- recent sales data window
- member distribution

输出：
- 推荐等级层级
- 推荐阈值
- 推荐原因
- 预期影响

### 4.2 Recommend Recharge Campaign

`POST /api/v2/members/recharge-campaigns/recommendations`

输出：
- 推荐充值档位
- 推荐赠送金额
- 适用人群
- 预期转化

### 4.3 Recommend Member Segments

`POST /api/v2/members/segments/recommendations`

输出：
- 高价值会员
- 流失风险会员
- 促销敏感会员
- 新客转会员建议

### 4.4 Recommend Benefit Adjustments

`POST /api/v2/member-benefits/recommendations`

输出：
- 建议新增权益
- 建议下调权益
- 建议变更适用层级

---

## API Group 5: Execution APIs

这组接口用于：
- 人工直接执行
- 或把 AI 草案正式落地

### 5.1 Apply Tier Recommendation

`POST /api/v2/member-tiers/recommendations/{recommendationId}/apply`

### 5.2 Apply Recharge Campaign Recommendation

`POST /api/v2/members/recharge-campaigns/recommendations/{recommendationId}/apply`

### 5.3 Apply Benefit Recommendation

`POST /api/v2/member-benefits/recommendations/{recommendationId}/apply`

规则：
- 请求里必须带 `meta`
- 若该动作被标记为中风险，返回 `APPROVAL_REQUIRED`

Request meta example:

```json
{
  "meta": {
    "actorType": "AI",
    "actorId": "crm-agent",
    "decisionSource": "AI_RECOMMENDATION",
    "aiRecommendationId": "rec_001"
  }
}
```

---

## API Group 6: Approval APIs

### 6.1 Approve CRM Change

`POST /api/v2/crm-approvals/{approvalId}/approve`

### 6.2 Reject CRM Change

`POST /api/v2/crm-approvals/{approvalId}/reject`

### 6.3 List Pending CRM Approvals

`GET /api/v2/crm-approvals?status=PENDING`

适用对象：
- 会员等级规则调整
- 充值活动草案
- 会员权益变更

---

## API Group 7: Audit APIs

### 7.1 List CRM Change Logs

`GET /api/v2/crm-audit-logs`

### 7.2 Get CRM Change Detail

`GET /api/v2/crm-audit-logs/{logId}`

### 7.3 List Recommendation History

`GET /api/v2/crm-recommendations/history`

这些接口需要能回答：
- 是谁发起的
- Human 还是 AI
- 建议内容是什么
- 最终是否被执行
- 谁批准的

---

## Shared Meta Contract

所有变更类接口建议支持：

```json
{
  "meta": {
    "actorType": "HUMAN",
    "actorId": "manager_1001",
    "decisionSource": "MERCHANT_ADMIN_UI",
    "changeReason": "Seasonal campaign update",
    "aiRecommendationId": null
  }
}
```

字段建议：
- `actorType`
- `actorId`
- `decisionSource`
- `changeReason`
- `aiRecommendationId`

---

## Error Code Baseline

CRM 域建议统一这些错误码：
- `MEMBER_NOT_FOUND`
- `MEMBER_ACCOUNT_NOT_FOUND`
- `MEMBER_TIER_NOT_FOUND`
- `INVALID_RECHARGE_AMOUNT`
- `INVALID_POINTS_ADJUSTMENT`
- `APPROVAL_REQUIRED`
- `AI_ACTION_NOT_ALLOWED`
- `CRM_RECOMMENDATION_NOT_FOUND`
- `DUPLICATE_MEMBER_PHONE`

---

## Risk Layering

### Low Risk
- 生成会员分群建议
- 生成充值活动草案
- 生成权益建议

### Medium Risk
- 直接应用等级规则调整
- 直接应用充值活动
- 调整积分规则

### High Risk
- 手工改余额
- 手工大额改积分
- 大范围会员权益下调

中高风险动作建议：
- 支持审批流
- 必须留审计记录

---

## V2 Minimal Standard for CRM

CRM 如果要进入 V2 正式实现，至少应满足：
- 会员资料与账户模型分离
- 余额与积分有独立流水
- 等级规则与权益配置独立
- 充值订单独立
- recommendation / approval / audit 已预留
- 所有变更支持 `meta` 来源标记

---

## Final Position

CRM 不应再被设计成“订单上的几个会员字段”。

它应当成为：
- 独立业务域
- 独立运营域
- 也是最典型的 AI-ready domain

这份 API contract 应作为后续 Promotion、Catalog、Report 等域的模板参考。
