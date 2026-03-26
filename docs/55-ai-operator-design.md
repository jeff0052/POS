# AI Operator Design

Date: 2026-03-26
Status: Implemented (PR #13)

---

## 1. Purpose

定义 Restaurant AI Operator 的架构 — 一个内嵌在 POS 系统中的 AI 运营大脑，通过 5 个顾问角色帮助餐饮老板做经营决策。

## 2. Core Concept

**不是 5 个独立 Agent，是 1 个 AI Operator + 5 个角色 Prompt。**

避免过度复杂。一个 Restaurant AI Operator 实例，根据场景切换角色上下文，共享同一套 MCP Tools 和审批流程。

## 3. Operating Model

```
Sense  →  通过 Read Tools 持续感知经营数据
Think  →  LLM 分析数据，识别问题和机会
Propose →  生成具体建议方案（AiRecommendation）
Approve →  推送给老板审批（LOW risk 可自动批准）
Act    →  通过 Write Tools 执行已批准的方案
```

## 4. Advisor Roles

### 4.1 菜单顾问 (MENU_ADVISOR)

**感知：** SKU 销量、毛利、退菜率、出菜时间
**输出：**
- "下架 3 道亏损菜，预计月省 SGD 450"
- "推主推套餐组合：A+B+C，毛利提升 12%"
- "午市 top 3 SKU 占总销量 45%，建议午市简化菜单"

**使用的 MCP Tools：** `catalog.list_skus`, `report.sku_performance`, `catalog.update_sku_status`

### 4.2 营销顾问 (PROMOTION_ADVISOR)

**感知：** 客流趋势、促销效果、时段分布
**输出：**
- "周三客流低 30%，建议满 50 减 10 活动"
- "上周满减活动带动客单价提升 18%，建议续期"
- "节日活动方案：套餐 + 会员专享价"

**使用的 MCP Tools：** `report.daily_summary`, `promotion.list_rules`, `promotion.create_rule`

### 4.3 会员顾问 (MEMBER_ADVISOR)

**感知：** 消费频次、充值余额、流失风险
**输出：**
- "32 名会员 30 天未到店，建议推送召回优惠"
- "Gold 会员月均消费下降 20%，建议调整权益"
- "充值活动建议：充 500 送 50，预计回收 SGD 15,000"

**使用的 MCP Tools：** `member.list_members`, `member.get_member`, `member.update_tier`

### 4.4 经营顾问 (BUSINESS_ADVISOR)

**感知：** 日报、周报、毛利、人效、翻台率
**输出：**
- "本周经营摘要：营收 SGD 28,500，环比 +5%，毛利 61%"
- "周三毛利仅 52%，主因是食材成本上升 8%"
- "翻台率 2.1 次/天，低于同区域均值 2.8"

**使用的 MCP Tools：** `report.daily_summary`, `report.sku_performance`

### 4.5 出品顾问 (KITCHEN_ADVISOR)

**感知：** 平均出菜时间、退菜原因、高峰时段
**输出：**
- "午市高峰出菜平均 18 分钟，建议简化 3 道慢菜"
- "退菜率最高：宫保鸡丁（8%），主因口味偏差"
- "17:30-19:00 出菜瓶颈，建议错峰备料"

**使用的 MCP Tools：** `order.list_orders`, `report.sku_performance`, `catalog.list_skus`

## 5. Recommendation Lifecycle

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ PENDING  │────>│ APPROVED │────>│ EXECUTED │     │ REJECTED │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
     │                                                   ▲
     │           ┌───────────────┐                       │
     └──────────>│ AUTO_APPROVED │──────> EXECUTED        │
     (LOW risk)  └───────────────┘                       │
     │                                                   │
     └───────────────────────────────────────────────────┘
```

### 状态说明

| 状态 | 含义 |
|------|------|
| PENDING | AI 生成建议，等待老板审批 |
| APPROVED | 老板批准，等待执行 |
| AUTO_APPROVED | LOW risk 建议，系统自动批准 |
| EXECUTED | 已通过 MCP Tool 执行 |
| REJECTED | 老板拒绝，记录原因 |

## 6. Trigger Modes

### 6.1 定时巡检

| 频率 | 内容 |
|------|------|
| 每天早上 8:00 | 生成前一天经营摘要 |
| 每周一 8:00 | 生成周报 + 本周建议 |
| 每月 1 日 | 月度经营分析 + 下月策略建议 |

### 6.2 事件驱动

| 事件 | 触发角色 |
|------|---------|
| 出菜超时 > 20 分钟 | 出品顾问 |
| 会员 30 天未消费 | 会员顾问 |
| 促销活动到期 | 营销顾问 |
| 日营收异常波动 > 20% | 经营顾问 |
| SKU 退菜率 > 5% | 菜单顾问 |

### 6.3 老板提问

老板可以直接问 AI Operator：
- "最近生意怎么样" → 经营顾问
- "帮我想个促销方案" → 营销顾问
- "哪些菜该下架" → 菜单顾问
- "会员运营怎么做" → 会员顾问

## 7. Data Model

### ai_recommendations 表

| 字段 | 类型 | 说明 |
|------|------|------|
| recommendation_id | VARCHAR(64) | 唯一 ID |
| merchant_id | BIGINT | 商户 ID |
| store_id | BIGINT | 门店 ID |
| advisor_role | VARCHAR(32) | 顾问角色 |
| title | VARCHAR(255) | 建议标题 |
| summary | TEXT | 摘要 |
| detail_json | JSON | 完整分析 |
| risk_level | VARCHAR(16) | LOW/MEDIUM/HIGH |
| status | VARCHAR(32) | 状态 |
| proposed_action | VARCHAR(64) | 对应的 MCP Tool 名称 |
| proposed_params_json | JSON | Tool 参数 |
| approved_by | VARCHAR(64) | 审批人 |
| rejected_reason | TEXT | 拒绝原因 |
| execution_result_json | JSON | 执行结果 |
| expires_at | TIMESTAMP | 建议过期时间 |

## 8. Prompt Engineering

每个角色有独立的 system prompt，包含：
1. 角色定义和职责边界
2. 输出格式规范（JSON 数组）
3. 风险判断标准
4. 中文输出要求

数据上下文通过 MCP Read Tools 获取后注入 prompt。

## 9. Security

- AI Operator 只能调用已注册的 MCP Tools
- MEDIUM/HIGH risk 操作必须人类审批
- 所有操作通过 ActionContext 记录审计日志
- Recommendation 有过期时间，过期自动失效
