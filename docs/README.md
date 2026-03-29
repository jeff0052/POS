# FounderPOS Documentation

**Last updated:** 2026-03-28

---

## 文档权威层级

```
Layer 0: README.md（本文件）              ← 导航索引，不做决策
Layer 1: final-executable-spec + 80 + 82  ← 实现基线，口径冲突时这层说了算
Layer 2: 83-system-architecture-v3.md     ← 架构概览，快速理解全貌
Layer 3: user-journeys + state-machines   ← 设计展开
Layer 4: 84-implementation-plan           ← 执行计划（16 session）
```

**快速入口：**
- 想了解全貌 → `83-system-architecture-v3.md`
- 想写代码 → `84-implementation-plan-and-roadmap.md`
- 有口径冲突 → 以 `specs/final-executable-spec.md` + `80-step-0.3-data-model-gaps.md` 为准

---

## Layer 1: 实现基线

| 文件 | 说明 | 权威范围 |
|------|------|---------|
| `specs/final-executable-spec.md` | 设计决策 D1-D10 + Flyway 规划 | 设计冲突以此为准 |
| `80-step-0.3-data-model-gaps.md` | 9 新表 + 8 ALTER DDL (FINAL) | DDL 冲突以此为准 |
| `82-step-0.4-ddl-review.md` | DDL review + agent review (FINAL) | 审查修复记录 |
| `85-api-contract.md` | 47 个 API 端点契约 | API 冲突以此为准 |
| `86-rbac-seed-data.md` | 52 权限 + 8 预置角色 | RBAC 种子数据 |

## Layer 2: 架构概览

| 文件 | 说明 |
|------|------|
| `83-system-architecture-v3.md` | 5 系统 / 125 表 / 12 Journey / 19 Migration 全貌 |

## Layer 3: 设计展开

| 文件 | 说明 |
|------|------|
| `specs/user-journeys.md` | 12 条 User Journey (J01-J12) |
| `specs/state-machines-and-constraints.md` | 12 状态机 + 并发/幂等约束 |

## Layer 4: 执行计划

| 文件 | 说明 |
|------|------|
| `84-implementation-plan-and-roadmap.md` | 6 周 / 16 session 执行清单 + Roadmap |
| `specs/sprint-plan-complete.md` | ~~旧版 Sprint 计划~~ SUPERSEDED，仅参考类名/API |

## 需求

| 文件 | 说明 |
|------|------|
| `65-aurora-restaurant-pos-detailed-prd.md` | 客户原始需求（小航） |
| `74-mrd-v3.md` | V3 MRD |
| `77-updated-prd-v3.md` | V3 PRD |

## 数据模型

| 文件 | 说明 |
|------|------|
| `75-complete-database-schema.md` | 120 表完整 DDL（原始 schema） |
| `76-database-schema-readme.md` | Schema 导读 |
| `81-table-reconciliation.md` | 125 表逐表对账 |

## 其他

| 文件 | 说明 |
|------|------|
| `79-session-2-handoff.md` | Session 交接（进度追踪） |
| `55-payment-service-handoff-for-external-team.md` | 支付外包交接 |

## Archive

`docs/archive/` — 75+ 历史文件（V1/V2 时代），不再有效。
`docs/superpowers/archive/` — 设计迭代过程文件。
