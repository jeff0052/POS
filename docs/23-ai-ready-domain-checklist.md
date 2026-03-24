# AI-Ready Domain Checklist

## Goal

为 Restaurant POS 的每个核心 domain 提供一份统一检查清单，确保未来设计新模块或新接口时，既满足传统业务系统要求，也满足 AI-ready 要求。

本清单适用于：
- 产品需求评审
- 领域建模
- API 设计
- 数据库设计
- 运营/审批流程设计

---

## How to Use

每当要新建一个 domain，或者给现有 domain 增强能力时，都先用这份清单过一遍。

如果以下大部分问题无法回答，说明该设计还不够稳。

---

## Section 1: Domain Purpose

- 这个 domain 的核心职责是什么？
- 它管理的核心对象是什么？
- 它的 source of truth 是什么？
- 它和哪些其他 domain 有关系？
- 哪些职责不应该放进这个 domain？

---

## Section 2: Human Workflow

- 人是通过哪个端来操作这个 domain？
- 哪些角色会使用它？
- 哪些动作是高频动作？
- 哪些动作是高风险动作？
- 页面和接口是否支持人工直接完成关键任务？

---

## Section 3: AI-Ready Workflow

- AI 在这个 domain 里未来可能扮演什么角色？
- AI 是只给建议，还是可以执行？
- 哪些动作可以 AI 自动做？
- 哪些动作必须人工审批？
- AI 输出的内容是 recommendation、draft 还是 final execution？

---

## Section 4: API Design

- 这个 domain 是否至少考虑了以下 API 类型？
  - configuration
  - recommendation
  - execution
  - approval
  - audit
- 当前一期是否至少预留了 recommendation / audit 的扩展空间？
- API 是否支持 `actorType` / `actorId` / `decisionSource` 等元信息？
- 错误码是否考虑了人工与 AI 两种调用来源？

---

## Section 5: Data Design

- 是否有独立主表？
- 是否有变更历史或流水表？
- 是否保留了必要快照？
- 是否支持区分 `HUMAN` 和 `AI` 创建/更新来源？
- 是否支持审批字段：
  - `approval_status`
  - `approved_by`
  - `approved_at`
- 是否支持：
  - `change_reason`
  - `ai_context_json`
  - `ai_recommendation_id`

---

## Section 6: Risk Control

- 这个 domain 的动作属于低风险、中风险还是高风险？
- 是否定义了哪些动作可以自动执行？
- 是否定义了哪些动作必须人工确认？
- 是否存在资金、税务、配置级高风险动作？
- 是否保留了完整审计链？

---

## Section 7: Reporting and Observability

- 这个 domain 的关键结果如何被报表消费？
- 是否有关键事件日志？
- 是否能追踪“是谁在什么时候做了什么”？
- 是否能解释 AI 建议最终有没有被采纳？
- 是否有后续可用于 AI 评估的数据沉淀？

---

## Section 8: V2 Minimal Standard

如果一个 domain 想进入 V2 正式实现，至少应满足：

- 职责边界清晰
- source of truth 清晰
- human workflow 清晰
- API contract 清晰
- 数据模型清晰
- 审计链路可扩展
- AI-ready 扩展点已考虑

---

## Suggested Use per Domain

### CRM
重点看：
- recommendation
- approval
- audit

### Promotion
重点看：
- rule execution
- approval
- change history

### Catalog / SKU
重点看：
- recommendation
- execution
- snapshot safety

### Staff / Shift
重点看：
- recommendation
- human override
- accountability

### Report
重点看：
- explanation
- anomaly detection
- AI summary output

### Platform Admin
重点看：
- AI monitoring
- AI recommendation governance
- high-risk approval

---

## Final Position

以后 Restaurant POS 的每个新模块，不应只问：

- 页面怎么做？
- 接口怎么写？

还要同时问：

- 人怎么用？
- AI 怎么介入？
- 怎么审批？
- 怎么审计？

这份 checklist 就是未来每个 domain 的最小 AI-ready 设计门槛。
