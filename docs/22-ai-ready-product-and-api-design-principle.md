# AI-Ready Product and API Design Principle

## Goal

定义 Restaurant POS 在产品设计和 API 设计上的 AI-ready 原则。

本原则用于确保：
- 底层交易系统保持传统、稳定、可审计
- 上层每个核心域都能逐步演进为 AI-assisted / AI-driven
- 未来引入 AI 时，不需要推翻基础业务系统

---

## Core Position

Restaurant POS 的底层仍然必须是一个传统的、可控的业务系统。

也就是说：
- 订单
- SKU
- 会员
- 员工
- 促销
- 结账
- 报表

这些能力必须首先满足：
- 稳定
- 可追踪
- 可审计
- 可解释

AI 不是替代这套底层，而是建立在这套底层之上，逐步参与：
- 建议
- 辅助
- 半自动执行
- 自动化运营

---

## Design Principle

### 1. Human and AI Must Both Be First-Class Operators

从现在开始，每个核心模块都应按双入口设计：

1. Human-driven
2. AI-assisted / AI-driven

也就是说：
- 人可以直接配置
- AI 可以生成建议
- AI 可以在权限范围内执行动作
- 最终所有动作都必须可审计

---

## Domain-Level AI-Ready Model

每个核心 domain 都应支持以下 5 类能力：

### 1. Configuration
人工直接配置规则、参数或对象。

### 2. Recommendation
AI 生成建议，但不直接落地。

### 3. Execution
人工或 AI 发起实际变更动作。

### 4. Approval
高风险动作支持人工确认、拒绝、修改后批准。

### 5. Audit
记录：
- 谁发起
- 来源是 Human 还是 AI
- 为什么做
- 依据是什么
- 最终谁批准

---

## Example by Domain

### CRM / Member
Human:
- 配等级
- 配积分规则
- 配充值活动
- 配会员权益

AI-ready:
- 推荐会员分层规则
- 推荐充值活动
- 识别高流失会员
- 推荐召回人群

### Catalog / SKU
Human:
- 管理商品、SKU、上下架

AI-ready:
- 推荐主推 SKU
- 推荐套餐组合
- 推荐淘汰低表现 SKU

### Promotion
Human:
- 配满减、满赠、会员价

AI-ready:
- 推荐今日促销
- 预测促销影响
- 自动生成促销草案

### Staff / Shift
Human:
- 排班
- 调班

AI-ready:
- 预测高峰
- 推荐排班
- 提示人力不足

### Report / Analytics
Human:
- 查看报表

AI-ready:
- 自动生成经营摘要
- 自动标记异常
- 自动输出经营建议

### Platform Admin
Human:
- 开通商户
- 管门店
- 管配置模板

AI-ready:
- 商户异常巡检
- 门店健康评分
- 自动识别高风险配置

---

## API Design Rules for AI-Ready Domains

以后每个核心模块都应优先考虑这 4 类 API：

### 1. Configuration API
用于人工配置。

例如：
- create promotion rule
- update member tier
- save staffing rule

### 2. Recommendation API
用于 AI 输出建议。

例如：
- recommend promotion plan
- recommend member segmentation
- recommend shift arrangement

### 3. Execution API
用于执行动作。

这个动作可以来源于：
- human
- AI

例如：
- apply promotion draft
- apply crm benefit update
- publish recommended menu bundle

### 4. Audit API
用于查询动作来源、审批和原因。

例如：
- list configuration changes
- list ai generated drafts
- get approval history

---

## Data Design Rules for AI-Ready Systems

关键 domain 建议预留或统一这些字段：

- `created_by_type` (`HUMAN`, `AI`)
- `created_by_id`
- `updated_by_type`
- `updated_by_id`
- `decision_source`
- `approval_status`
- `approved_by`
- `approved_at`
- `change_reason`
- `ai_context_json`
- `ai_recommendation_id`

这些字段的作用是：
- 支持 AI 和人混合操作
- 支持审批和追溯
- 支持后续评估 AI 质量

---

## Risk Control Principle

AI 不是所有地方都应该直接自动执行。

建议按风险分层：

### Low Risk
可直接 AI 自动执行：
- 生成摘要
- 生成报表解释
- 生成建议草案

### Medium Risk
AI 可执行，但建议人工确认：
- 促销规则发布
- 会员权益调整
- 商品上下架建议落地

### High Risk
必须人工审批：
- 支付相关配置
- 税务/GTO 相关配置
- 资金相关规则
- 平台级商户状态变更

---

## What This Means for V2

V2 第一阶段虽然不会立即实现完整 AI 功能，但在设计时应明确：

- domain API 不能只面向人工后台页面
- 每个域都要保留未来 AI 调用空间
- 配置与执行必须支持来源标记
- 审计链路必须从底层开始建设

换句话说：

**V2 是传统交易系统底座 + AI-ready domain design。**

---

## Final Position

Restaurant POS 的正确方向不是：
- 先做一套传统系统，未来再硬塞 AI

而是：
- 底层仍然是传统、可靠、可审计的业务系统
- 每个核心域从设计之初就为 AI 协作与 AI 驱动预留结构

这样未来系统才能平滑演进为：

**AI-Powered Restaurant Operating System**
