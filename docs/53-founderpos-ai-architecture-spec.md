# FounderPOS AI-Driven Architecture Spec

## Vision

FounderPOS = 给每家餐厅配一个不休息的 AI 运营总监。老板一个人就可以驱动一家餐厅。

## 4-Layer Architecture

```
Layer 4 ─ Agent Identity Layer
           每家餐厅 = 1 Agent + 1 Wallet
           对外：预定/包场/合作/询价
           Agent-to-Agent Protocol

Layer 3 ─ AI Operator Layer
           Restaurant AI Operator（内部运营大脑）
           5 角色：菜单顾问/营销顾问/会员顾问/经营顾问/出品顾问
           模式：AI 分析 → 生成方案 → 人类审批 → 执行

Layer 2 ─ MCP Tool Layer
           每个域暴露为 MCP Tools（14 个工具）
           统一 ActionContext（actor_type / decision_source / approval）
           人类操作和 AI 操作都走同一套 Tools

Layer 1 ─ Transaction Foundation
           Order / Catalog / CRM / Promotion / Settlement / Report
           Staff / Shift / GTO / Platform
           传统 CRUD + 业务逻辑，独立可用
```

## Layer 1 — Transaction Foundation

### Domains

| Domain | Responsibility | Key Service |
|--------|---------------|-------------|
| Order | 活动桌单、提交单、状态流 | ActiveTableOrderApplicationService |
| Catalog | 商品、SKU、分类、菜单 | AdminCatalogReadService, QrMenuApplicationService |
| Member | 会员、充值、积分、等级、余额 | MemberApplicationService, MemberSettlementHookService |
| Promotion | 满减、百分比折扣、赠品、使用限制 | PromotionApplicationService |
| Settlement | 结账、支付、退款 | CashierSettlementApplicationService, VibeCashPaymentApplicationService |
| Report | 日报、销售汇总、订单监控 | ReportReadService |
| Staff | 员工、角色、权限、PIN 登录 | StaffApplicationService |
| Shift | Cashier 班次、现金核对 | CashierShiftApplicationService |
| GTO | 税务导出、GST 计算 | GtoExportApplicationService |
| Platform | 平台管理、门店 CRUD | PlatformAdminApplicationService |
| Store | 门店、桌台 | (entities + repositories) |

### ActionContext

每个写操作携带：

```java
ActionContext {
    actorType:      "HUMAN" | "AI" | "SYSTEM"
    actorId:        "jeff" | "menu-advisor" | "cashier-001"
    decisionSource: "MANUAL" | "AI_RECOMMENDATION" | "AI_AUTO"
    recommendationId: nullable
    changeReason:   "周三客流低，建议满减"
}
```

### Risk Level

| 风险 | AI 可自动 | 举例 |
|------|----------|------|
| Low | 直接执行 | 生成报表、标记沉睡会员、分析出菜速度 |
| Medium | 生成草案→人类审批 | 发布促销、调整价格、会员权益变更 |
| High | 只能建议 | 退款、支付配置、GTO 相关 |

## Layer 2 — MCP Tool Server

### Endpoint

```
GET  /api/v2/mcp/tools     → 发现所有可用工具（含 JSON Schema）
POST /api/v2/mcp/execute   → 调用单个工具
POST /api/v2/mcp/batch     → 批量调用
```

### Tool Catalog (14 tools)

| Domain | Tool | Type | Description |
|--------|------|------|-------------|
| Catalog | catalog.list_products | Read | 列出门店所有商品和 SKU |
| Catalog | catalog.get_menu | Read | 获取顾客菜单 |
| Order | order.get_active | Read | 获取桌台当前活动订单 |
| Order | order.list_all | Read | 列出门店所有订单 |
| Order | order.submit_to_kitchen | Write | 提交订单到厨房 |
| Order | order.move_to_settlement | Write | 推进订单到结账 |
| Member | member.search | Read | 搜索会员 |
| Member | member.get_detail | Read | 获取会员详情 |
| Member | member.recharge | Write | 会员充值 |
| Settlement | settlement.preview | Read | 结账预览 |
| Promotion | promotion.list_rules | Read | 列出促销规则 |
| Promotion | promotion.apply_best | Write | 应用最佳促销 |
| Report | report.daily_summary | Read | 日销售摘要 |
| Report | report.sales_summary | Read | 销售汇总 |
| Report | report.order_state_monitor | Read | 订单状态监控 |

### Design Principle

- Tool 不直接碰数据库 — 必须通过 V2 Application Service
- 所有 Tool 调用携带 ActionContext
- Tool 的 inputSchema 遵循 JSON Schema 标准
- 新增域 = 新增 Tool class + 实现 McpTool 接口 → 自动注册

## Layer 3 — AI Operator (P1, next step)

### Core Pattern: Sense → Think → Propose → Approve → Act

```
Sense:   通过 Read Tools 持续感知经营数据
Think:   LLM 分析，识别问题和机会
Propose: 生成具体方案（草案）
Approve: 推送给老板审批（低风险可跳过）
Act:     通过 Write Tools 执行
```

### 5 Advisor Roles

| 角色 | 感知 | 输出 |
|------|------|------|
| 菜单顾问 | SKU 销量、毛利、退菜率 | 下架亏损菜、推主推套餐 |
| 营销顾问 | 客流趋势、促销效果 | 满减草案、节日活动 |
| 会员顾问 | 消费频次、流失风险 | 召回方案、等级调整 |
| 经营顾问 | 日报、毛利、翻台率 | 经营摘要、异常预警 |
| 出品顾问 | 出菜时间、退菜原因 | 高峰简化建议、瓶颈分析 |

### Implementation: 1 Agent + 5 Role Prompts

不是 5 个独立进程，而是 1 个 AI Operator 根据场景切换角色上下文。

### Trigger Modes

- **定时巡检** — 每天早上经营摘要，每周周报
- **事件驱动** — 出菜超时、会员流失、促销到期
- **老板提问** — "最近生意怎么样""帮我想个促销"

## Layer 4 — Agent + Wallet (P2)

### Restaurant Agent

```
Restaurant Agent "老王烧烤"
├── Identity: 名称 / 地址 / 菜系 / 营业时间
├── Wallet: 收款账户 / 余额 / 分账规则
├── Capabilities: 可接预定 / 可包场 / 可外卖
└── Protocol: 对外暴露的交互能力
```

### External Interactions

| 外部 Agent | 场景 |
|-----------|------|
| 顾客 Agent | 询价/订位/点餐 |
| 企业 Agent | 包场/团建 |
| 供应商 Agent | 报价/采购 |
| 平台 Agent | 外卖上架/促销/结算 |

### Interaction Flow

```
外部 Agent → Restaurant Agent → AI Operator 分析
                                     ↓
                              低风险：自动回复
                              中风险：生成方案→老板审批
                              高风险：转人工
```

### Wallet

- 收款（顾客支付、平台结算）
- 分账（平台抽成、供应商付款）
- 预算控制（AI 营销花费上限）
- 对账（自动核对各渠道到账）

## Layer 5 — Restaurant Network (P4)

### 匿名数据共享

餐厅 Agent opt-in 贡献匿名数据，换取网络洞察：
- 同区域/同菜系经营基准线
- 趋势预警（"你这区域本周外卖普遍下降15%"）
- 最佳实践推荐

### Agent 间协作

- 联合采购（几家店一起向供应商压价）
- 互相导流（"我们满了，推荐隔壁同菜系"）
- 共享排班（临时工在多家店之间调度）

## Delivery Roadmap

| Phase | Content | Depends On |
|-------|---------|-----------|
| P0 | MCP Tool Server | Layer 1 (done) |
| P1 | AI Operator (5 advisors) | P0 |
| P2 | Agent + Wallet | P0 + P1 |
| P3 | Agent-to-Agent Protocol | P2 |
| P4 | Restaurant Network | P3 |

## Technical Stack

| Component | Technology |
|-----------|-----------|
| Backend | Spring Boot 3.3.3, Java 17, Maven |
| Database | MySQL 8, Flyway migrations |
| Android | Kotlin, Jetpack Compose, WebView |
| Frontend | React, TypeScript, Vite |
| AI Integration | MCP Tools over HTTP, LLM via API |
| Payment | DCS (card), VibeCash (QR), Cash |

## Guiding Principles

1. **底层传统，上层 AI** — 交易系统必须稳定可审计，AI 建立在这之上
2. **人和 AI 都是一等公民** — 同一套 Tool 接口，区别只是 ActionContext
3. **AI 不直接碰数据库** — 必须通过 Tool → Service → Repository
4. **高风险必须人类审批** — 支付、退款、GTO 永远不能 AI 自动执行
5. **每家餐厅一个 Agent** — 不是共享的 SaaS，而是独立的 AI 身份
