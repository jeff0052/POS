# Roadmap

## 1. Roadmap Goal

FounderPOS 的路线图分为两条主线：

**主线 A — 传统 POS 交易底座**：让餐厅能正常点餐、结账、管会员、出报表
**主线 B — AI 驱动运营层**：让一个老板通过 AI 合伙人管好一家餐厅

两条主线并行推进，A 是 B 的基础。

## 2. Current Status (Updated 2026-03-27)

### Progress Snapshot

- Overall：**~80%**
- Stage 0 Prototype：**Completed** ✅
- Stage 1 Store Transaction MVP：**~90%** ✅
- Stage 2 Merchant Operations：**~75%** ✅
- Stage 3 Platform Control Center：**~70%** ✅
- Stage 4 Production Hardening：**~60%** ✅
- Stage 5 AI Layer：**~40%** (new)
- Stage 6 Agent Economy：**~10%** (new)

### What's Done

| Module | Status | Notes |
|--------|--------|-------|
| Ordering (POS + QR) | ✅ 100% | Unified table order, send to kitchen |
| QR Ordering | ✅ 95% | Menu loading, order submit, shared backend |
| Cashier Settlement | ✅ 90% | Cash + QR + amount validation + idempotency |
| SKU / Catalog | ✅ 85% | Categories, products, SKUs, server-side pricing |
| CRM / Member | ✅ 80% | Registration, recharge, points, tier auto-upgrade |
| Promotion | ✅ 75% | Amount discount, percent discount, gift SKU, usage limits |
| Staff / Permission | ✅ 80% | Roles, permissions, PIN verification |
| Cashier Shift | ✅ 80% | Open/close shift, cash reconciliation |
| GTO Tax Export | ✅ 70% | Batch generation, GST calculation, retry |
| Refund | ✅ 70% | Pessimistic lock, amount validation, status tracking |
| Report | ⚠️ 60% | Daily summary, sales report (needs deepening) |
| Authentication | ✅ 90% | JWT + BCrypt + role-based access + bootstrap |
| Payment Microservice | ✅ 50% | Skeleton + Cash adapter (VibeCash/DCS by external team) |
| Platform Admin | ✅ 70% | 7 pages, login, dashboard, merchant hierarchy |
| MCP Tool Server | ✅ 80% | 14 tools across 6 domains |
| AI Operator | ✅ 60% | 5 advisor roles, recommendation lifecycle |
| Agent + Wallet | ✅ 40% | Entity model, wallet ledger, interaction routing |
| AWS Deployment | ✅ 90% | 8 containers, nginx, CI pipeline |

### What's NOT Done

| Module | Status | Blocked By |
|--------|--------|------------|
| DCS Card Payment | ❌ | Sunmi SDK version mismatch |
| VibeCash QR Adapter | ❌ | External team (handoff doc ready) |
| Printing | ❌ | Hardware dependent |
| Kitchen / KDS | ❌ | Design done, not implemented |
| Delivery Integration | ❌ | Design done, not implemented |
| Integration Tests | ⚠️ | Only MCP tests exist |
| Agent Protocol (P3) | ❌ | Depends on P2 |
| Restaurant Network (P4) | ❌ | Depends on P3 |
| RWA (P5) | ❌ | Depends on P4 |

## 3. Evolution Stages

### Stage 0: Prototype ✅ COMPLETED
- POS + QR unified table order
- Basic ordering → settlement flow
- Android preview + Merchant admin skeleton

### Stage 1: Store Transaction MVP — ~90%
- ✅ Table management
- ✅ POS + QR ordering
- ✅ Send to kitchen → settlement flow
- ✅ Cashier shift
- ✅ Staff / permission
- ✅ Refund
- ❌ Printing (receipts + kitchen tickets)
- ❌ Kitchen / KDS display

### Stage 2: Merchant Operations — ~75%
- ✅ CRM member lifecycle
- ✅ Recharge + points + tier
- ✅ Promotion rule center
- ✅ SKU / Catalog management
- ✅ GTO tax export
- ✅ Merchant admin real data + dashboard
- ⚠️ Reports (needs deepening)
- ⚠️ CRM (needs detail page + tier rules backend)

### Stage 3: Platform Control Center — ~70%
- ✅ Platform admin 7 pages
- ✅ Login + auth guard
- ✅ Merchant hierarchy drill-down
- ⚠️ Store-level drill-down (partial)
- ⚠️ Device management (mock data)
- ⚠️ Configuration templates (static)

### Stage 4: Production Hardening — ~60%
- ✅ JWT authentication + authorization
- ✅ Payment microservice isolation
- ✅ Docker prod compose (8 services)
- ✅ CI pipeline (6 jobs)
- ✅ Nginx reverse proxy + CSP
- ✅ Fail-fast secrets
- ⚠️ Integration tests (minimal)
- ❌ Monitoring / alerting
- ❌ Backup / recovery
- ❌ SSL / HTTPS

### Stage 5: AI Layer — ~40% (NEW)
- ✅ MCP Tool Server (14 tools)
- ✅ AI Operator (5 advisors + recommendation lifecycle)
- ✅ Agent + Wallet (entity model)
- ❌ AI Operator connected to real LLM
- ❌ Scheduled advisor checks (daily/weekly)
- ❌ Agent external interaction protocol
- ❌ Agent-to-Agent communication

### Stage 6: Agent Economy — ~10% (NEW)
- ✅ Concept design + architecture spec
- ❌ Restaurant Agent Network
- ❌ Anonymous data sharing
- ❌ Agent marketplace
- ❌ RWA / restaurant bonds

## 4. Milestones

### Milestone A: Unified Table Order ✅ ACHIEVED
- POS + QR don't overwrite each other
- One table one order
- Table status is clear

### Milestone B: Store Transaction MVP ✅ ACHIEVED
- POS + QR full flow works
- Cashier settlement with amount validation
- Order lifecycle: draft → submitted → pending → settled

### Milestone C: Merchant Operations ✅ ACHIEVED (2026-03-27)
- CRM member lifecycle working
- SKU / Catalog management working
- Promotion engine with 3 reward types
- Reports and GTO basics
- Merchant admin with real data

### Milestone D: Production Deployment ✅ ACHIEVED (2026-03-27)
- 8 containers on AWS
- Real JWT auth
- Payment microservice
- CI pipeline
- 6 rounds of security audit passed

### Milestone E: AI Layer Foundation ✅ ACHIEVED (2026-03-27)
- MCP Tool Server operational
- AI Operator with 5 advisors
- Agent + Wallet model

### Milestone F: Payment Integration — NEXT
- VibeCash QR adapter (external team)
- DCS card adapter (pending SDK fix)
- End-to-end payment flow

### Milestone G: Kitchen & Printing — NEXT
- Kitchen display system
- Receipt printing
- Kitchen ticket printing

### Milestone H: AI Operational — FUTURE
- Connect AI Operator to LLM
- Daily/weekly advisor reports
- Owner approval workflow

### Milestone I: Agent Economy — FUTURE
- Restaurant Agent Network
- Agent-to-Agent protocol
- RWA integration

## 5. Next Priorities

### Immediate (next sprint)
1. **VibeCash adapter** — external team implements, we integrate
2. **DCS SDK fix** — resolve version mismatch, test on device
3. **Printing** — receipt + kitchen ticket on Sunmi
4. **Kitchen/KDS** — display submitted orders, status transitions

### Short-term (next month)
5. **Reports deepening** — finance-grade output
6. **CRM deepening** — member detail, tier rules
7. **Integration tests** — order + settlement + refund paths
8. **SSL/HTTPS** — production security

### Medium-term (next quarter)
9. **AI Operator + LLM** — connect to Claude API, daily advisor runs
10. **Delivery integration** — third-party platform orders
11. **Multi-store** — real multi-tenant operations
12. **Agent Protocol** — external agent interactions

## 6. Architecture (Current)

```
Layer 4 — Agent Identity (P2: 40%)
           Restaurant Agent + Wallet + External Interactions

Layer 3 — AI Operator (P1: 60%)
           5 Advisors: Menu / Marketing / CRM / Operations / Kitchen
           Recommendation lifecycle: Propose → Approve → Execute

Layer 2 — MCP Tool Layer (P0: 80%)
           14 tools across 6 domains
           ActionContext audit trail

Layer 1 — Transaction Foundation (90%)
           Order / Catalog / CRM / Promotion / Settlement / Report / Staff / Shift / GTO
           Payment Microservice (independent)

Infrastructure — AWS / Docker / Nginx / CI / JWT Auth
```

## 7. Delivery Guideline

Each iteration should complete one closed loop:

1. ~~Unified table order~~ ✅
2. ~~Cashier shift + settlement~~ ✅
3. ~~Member identification + benefits~~ ✅
4. ~~SKU / pricing authority~~ ✅
5. ~~Settlement + reporting~~ ✅
6. ~~GTO export~~ ✅
7. ~~Platform admin~~ ✅
8. **Payment provider integration** ← CURRENT
9. Kitchen / KDS / Printing
10. AI Operator + LLM connection
11. Agent economy
