# FounderPOS System Architecture

## Overview

FounderPOS 是一个四层架构的 AI 驱动餐厅操作系统。底层是传统交易系统，顶层是自主 Agent 网络。每一层都可以独立运行，上层依赖下层但不侵入。

```
┌─────────────────────────────────────────────────────────────────┐
│  Layer 4 — Agent Identity + Network                             │
│  Restaurant Agent / Wallet / A2A Protocol / Network Intelligence│
├─────────────────────────────────────────────────────────────────┤
│  Layer 3 — AI Operator                                          │
│  5 Advisors / Sense-Think-Propose-Approve-Act / Recommendations │
├─────────────────────────────────────────────────────────────────┤
│  Layer 2 — MCP Tool Layer                                       │
│  22 Tools / ActionContext / Audit Log / Risk Control             │
├─────────────────────────────────────────────────────────────────┤
│  Layer 1 — Transaction Foundation                               │
│  Order / Catalog / CRM / Promotion / Settlement / Report / Staff│
└─────────────────────────────────────────────────────────────────┘
         │                    │                    │
    ┌────┴────┐         ┌────┴────┐         ┌────┴────┐
    │ POS App │         │ QR Web  │         │  Admin  │
    │  :5188  │         │  :4183  │         │  :5187  │
    └─────────┘         └─────────┘         └─────────┘
```

---

## Layer 1 — Transaction Foundation

传统交易底座。稳定、可审计、独立可用。没有 AI 也能跑。

### 技术栈

| 组件 | 选型 |
|------|------|
| 框架 | Spring Boot 3.3 + Java 17 |
| 数据库 | MySQL 8.4 (pos_v2_db) |
| ORM | Spring Data JPA + Hibernate |
| 迁移 | Flyway |
| 构建 | Maven + Docker |
| 序列化 | Jackson |

### 后端域划分

```
pos-backend/src/main/java/com/developer/pos/
├── v2/
│   ├── catalog/          # 商品、分类、SKU
│   ├── order/            # 活动桌单、提交订单、桌台 Session
│   ├── settlement/       # 结账、支付记录、VibeCash webhook
│   ├── member/           # 会员、账户、积分、充值
│   ├── promotion/        # 促销规则、条件、奖励、命中
│   ├── report/           # 日报、销售汇总、订单监控
│   ├── store/            # 门店、桌台、终端
│   ├── staff/            # 员工、角色、权限
│   ├── shift/            # Cashier 班次、现金对账
│   ├── gto/              # GTO 税务导出（新加坡 IRAS）
│   ├── mcp/              # Layer 2 — MCP Tool Server
│   ├── ai/               # Layer 3 — AI Operator
│   ├── agent/            # Layer 4 — Agent + Wallet
│   └── common/           # 共享基础设施
└── auth/                 # V1 认证（待统一）
```

### 每个域的内部结构

遵循 DDD 分层：

```
v2/{domain}/
├── interfaces/rest/           # Controller + Request DTO
├── application/
│   ├── service/               # Application Service
│   ├── command/               # Write 命令
│   ├── dto/                   # Read DTO
│   └── query/                 # Query 对象
├── domain/
│   ├── model/                 # 领域模型
│   ├── status/                # 状态枚举
│   ├── source/                # 来源枚举
│   └── policy/                # 业务策略
└── infrastructure/
    └── persistence/
        ├── entity/            # JPA Entity
        └── repository/        # Spring Data Repository
```

### 数据库 Schema

25 个 Flyway migration，43 张表：

| 版本 | 内容 |
|------|------|
| V001-V004 | 商户、门店、桌台、商品、SKU、订单 |
| V008-V015 | 结算、会员、促销、Session、充值、支付 |
| V016-V019 | 商品配置、班次、预订、结算幂等 |
| V020-V032 | GTO、Staff、审计列、ActionLog、AI Proposal |
| V035-V045 | 促销增强、AI 推荐、Agent Wallet |

### 订单生命周期

```
                    POS                          QR
                     │                            │
                     ▼                            ▼
              ┌──────────┐                 ┌──────────┐
              │  DRAFT   │                 │ SUBMITTED│ (QR 直接提交)
              └────┬─────┘                 └────┬─────┘
                   │ Send to Kitchen            │
                   ▼                            │
              ┌──────────┐                      │
              │SUBMITTED │◄─────────────────────┘
              └────┬─────┘
                   │ Move to Payment
                   ▼
              ┌───────────────────┐
              │PENDING_SETTLEMENT │
              └────┬──────────────┘
                   │ Collect Payment
                   ▼
              ┌──────────┐
              │ SETTLED  │ → 清桌 → 桌台变 AVAILABLE
              └──────────┘
```

### 桌台模型

```
Store → StoreTable (T1-T24)
         │
         ├── TableSession (一次用餐 = 一个 session)
         │     ├── SubmittedOrder #1 (round 1 送厨)
         │     ├── SubmittedOrder #2 (round 2 加菜)
         │     └── SubmittedOrder #3 (round 3 QR 点单)
         │
         └── ActiveTableOrder (当前草稿，结账后删除)
```

### API 规范

所有 V2 API 统一返回格式：

```json
{
  "code": 0,
  "message": "ok",
  "data": { ... }
}
```

V2 API 路径规范：`/api/v2/{domain}/{resource}`

---

## Layer 2 — MCP Tool Layer

让 AI 能操作 POS 的每一个模块。人类操作（后台/POS）和 AI 操作都走同一套 Tools。

### 架构

```
        Human (POS/Admin)          AI Operator (Layer 3)
              │                           │
              │  直接调 Service            │  通过 MCP Tools
              │                           │
              ▼                           ▼
        ┌─────────────────────────────────────┐
        │          MCP Tool Registry          │
        │  22 tools across 7 domains          │
        │                                     │
        │  ActionContext → 谁操作/为什么/谁批准  │
        │  ActionLog    → 所有操作留痕          │
        │  RiskLevel    → LOW/MEDIUM/HIGH     │
        └───────────┬─────────────────────────┘
                    │
                    ▼
        ┌─────────────────────┐
        │  Domain Services    │
        │  (Layer 1)          │
        └─────────────────────┘
```

### 22 个 MCP 工具

| 域 | 工具 | 风险 |
|----|------|------|
| **Report** | get_daily_summary, get_sales_summary | LOW |
| **Report** | get_order_state_monitor | LOW |
| **Catalog** | list_categories, list_skus | LOW |
| **Catalog** | create_sku, update_sku, toggle_sku_status | MEDIUM |
| **Order** | get_active_order, list_submitted_orders | LOW |
| **Order** | submit_to_kitchen | MEDIUM |
| **Settlement** | get_settlement_preview, collect_payment | HIGH |
| **Promotion** | list_rules, get_rule, apply_best_promotion | LOW |
| **Promotion** | create_rule, update_rule | MEDIUM |
| **Member** | get_member, search_members | LOW |
| **Member** | create_member, recharge_member | MEDIUM |
| **Store** | list_tables, get_store_info | LOW |

### ActionContext

每次写操作都携带：

```java
ActionContext {
    ActorType actorType;        // HUMAN | AI | EXTERNAL_AGENT
    String actorId;             // "jeff" | "menu-advisor" | "supplier-agent-123"
    DecisionSource source;      // MANUAL | AI_RECOMMENDATION | AI_AUTO
    String recommendationId;    // 关联到 AI 建议（可选）
    ApprovalStatus approval;    // APPROVED | PENDING | REJECTED | NOT_REQUIRED
    String reason;              // "周三客流低，建议满减"
}
```

### 风险分级

| 风险 | AI 可以 | 举例 |
|------|---------|------|
| LOW | 直接执行 | 查报表、查会员、生成摘要 |
| MEDIUM | 生成草案→人类审批 | 发布促销、调价、上下架 |
| HIGH | 只能建议 | 退款、支付配置、税务相关 |

### REST 端点

```
GET  /api/v2/mcp/tools              → 列出所有工具
POST /api/v2/mcp/tools/{name}       → 执行工具
GET  /api/v2/mcp/logs               → 查看操作日志
```

---

## Layer 3 — AI Operator

餐厅的 AI 运营大脑。不是 5 个独立 Agent，是 1 个 Operator + 5 个角色 Prompt。

### 运作模式

```
┌─────────┐     ┌─────────┐     ┌──────────┐     ┌──────────┐     ┌─────────┐
│  Sense  │────▶│  Think  │────▶│ Propose  │────▶│ Approve  │────▶│   Act   │
│ 感知数据 │     │ LLM 分析 │     │ 生成方案  │     │ 人类审批  │     │ 执行动作 │
└─────────┘     └─────────┘     └──────────┘     └──────────┘     └─────────┘
     │                                                                  │
     │              通过 MCP Read Tools                    通过 MCP Write Tools
     └──────────────────────────────────────────────────────────────────┘
```

### 5 个顾问角色

```java
enum AdvisorRole {
    MENU_ADVISOR,       // 菜单顾问：SKU 销量、毛利、退菜率
    MARKETING_ADVISOR,  // 营销顾问：客流趋势、促销效果
    CRM_ADVISOR,        // 会员顾问：消费频次、流失风险
    OPERATIONS_ADVISOR, // 经营顾问：日报、周报、异常预警
    KITCHEN_ADVISOR     // 出品顾问：出菜时间、高峰瓶颈
}
```

### 推荐生命周期

```
PENDING → APPROVED → EXECUTED
    │         ↑
    └→ REJECTED
    │
    └→ AUTO_APPROVED → EXECUTED (LOW risk)
```

### 触发方式

| 方式 | 场景 |
|------|------|
| 定时巡检 | 每天早上生成经营摘要、每周生成周报 |
| 事件驱动 | 出菜超时、会员流失、促销到期 |
| 老板提问 | "最近生意怎么样""帮我想个促销方案" |

### 数据模型

```
ai_recommendations
├── recommendation_id (UUID)
├── merchant_id / store_id
├── advisor_role
├── title / summary / detail_json
├── risk_level (LOW / MEDIUM / HIGH)
├── status (PENDING / APPROVED / REJECTED / AUTO_APPROVED / EXECUTED)
├── proposed_action / proposed_params_json
├── approved_by / approved_at
├── rejected_reason
├── executed_at / execution_result_json
├── created_at / expires_at
```

### REST 端点

```
GET  /api/v2/ai/advisors                         → 列出 5 个顾问
POST /api/v2/ai/advisors/{role}/check             → 触发顾问巡检
POST /api/v2/ai/recommendations                   → 手动创建建议
GET  /api/v2/ai/recommendations                   → 查看所有建议
GET  /api/v2/ai/recommendations/pending            → 待审批建议
GET  /api/v2/ai/recommendations/{id}               → 建议详情
POST /api/v2/ai/recommendations/{id}/approve       → 批准
POST /api/v2/ai/recommendations/{id}/reject        → 拒绝
POST /api/v2/ai/recommendations/{id}/execute       → 执行
```

---

## Layer 4 — Agent Identity + Wallet + Network

每家餐厅对外的 AI 身份和财务自主能力。

### Restaurant Agent

```
restaurant_agents
├── agent_id (UUID)
├── store_id (FK → stores)
├── agent_name / agent_description
├── cuisine_type / price_range
├── capabilities_json (预定/包场/外卖/团购)
├── operating_hours_json
├── agent_status (ACTIVE / INACTIVE / SUSPENDED)
├── public_endpoint_url
```

### Agent Wallet

```
agent_wallets
├── wallet_id (UUID)
├── agent_id (FK → restaurant_agents)
├── balance_cents
├── currency_code (SGD)
├── daily_limit_cents / monthly_limit_cents

wallet_transactions
├── transaction_id (UUID)
├── wallet_id (FK)
├── transaction_type (INCOME / EXPENSE / TRANSFER)
├── amount_cents
├── balance_after_cents
├── counterparty_agent_id
├── description / reference_id
```

### Agent Interactions

```
agent_interactions
├── interaction_id (UUID)
├── agent_id (FK)
├── source_agent_id / source_agent_name
├── interaction_type (RESERVATION / INQUIRY / VENUE_BOOKING / SUPPLY_QUOTE)
├── request_json / response_json
├── risk_level (LOW / MEDIUM / HIGH)
├── status (PENDING / AUTO_RESPONDED / RESPONDED / ESCALATED)
├── auto_handled (boolean)
├── handled_by / handled_at
```

### 交互流程

```
External Agent
      │
      │ POST /api/v2/agents/{agentId}/interact
      │ { type: "VENUE_BOOKING", request: { date, pax, budget } }
      ▼
Restaurant Agent
      │
      ├── LOW risk → Auto-respond (查价、查位)
      │
      ├── MEDIUM risk → AI 生成方案 → 推给老板
      │                 { "建议报价 3800，含 10 道菜，毛利 42%" }
      │                 老板 [批准] → 回复对方
      │
      └── HIGH risk → 转人工
```

### REST 端点

```
POST /api/v2/agents                               → 注册 Agent
GET  /api/v2/agents/{agentId}                      → Agent 详情
PUT  /api/v2/agents/{agentId}                      → 更新 Agent
POST /api/v2/agents/{agentId}/interact             → 接收外部交互
GET  /api/v2/agents/{agentId}/interactions          → 交互历史
GET  /api/v2/agents/{agentId}/interactions/pending   → 待处理
POST /api/v2/agents/{agentId}/interactions/{id}/respond → 回复
GET  /api/v2/agents/{agentId}/wallet                → 钱包详情
POST /api/v2/agents/{agentId}/wallet/income         → 收入
POST /api/v2/agents/{agentId}/wallet/expense        → 支出
GET  /api/v2/agents/{agentId}/wallet/transactions    → 流水
```

---

## Frontend Architecture

### 三端

| 端 | 技术 | 端口 | 用户 |
|----|------|------|------|
| POS (Tablet) | React + TypeScript (WebView) | :5188 | 服务员/收银 |
| QR Ordering | React + TypeScript | :4183 | 顾客 |
| Merchant Admin | React + TypeScript | :5187 | 老板/管理者 |

### POS 前端视图

```
android-preview-web/src/
├── views/
│   ├── tables        # 桌台管理（24 桌网格）
│   ├── ordering      # 点餐（菜单 + 购物车）
│   ├── review        # 订单预览
│   ├── payment       # 支付（Cash / Card / QR）
│   ├── success       # 支付成功
│   ├── reservations  # 预订管理
│   ├── transfer      # 转台
│   └── split         # 分单
```

### Admin 前端路由

```
pc-admin/src/
├── pages/
│   ├── Dashboard      # 经营总览（营收/订单/支付分布）
│   ├── Products       # 商品管理
│   ├── Categories     # 分类管理
│   ├── Orders         # 订单列表
│   ├── Refunds        # 退款管理
│   ├── CRM            # 会员管理
│   ├── Promotions     # 促销管理
│   ├── Reports        # 报表
│   └── GTO Sync       # GTO 税务同步
```

### QR 前端流程

```
扫码 → 查看菜单 → 加购 → 提交订单 → 等待厨房 → 收银结账
```

---

## Infrastructure

### 部署架构

```
┌──────────────────────────────┐
│       Docker Compose         │
│                              │
│  ┌─────────┐  ┌───────────┐ │
│  │ MySQL   │  │ pos-backend│ │
│  │ 8.4     │  │ Spring Boot│ │
│  │ :3306   │  │ :8080     │ │
│  └─────────┘  └───────────┘ │
│                              │
│  ┌─────────┐  ┌───────────┐ │
│  │ POS Web │  │ Admin Web │ │
│  │ :5188   │  │ :5187     │ │
│  └─────────┘  └───────────┘ │
│                              │
│  ┌─────────┐                │
│  │ QR Web  │                │
│  │ :4183   │                │
│  └─────────┘                │
└──────────────────────────────┘
```

### 数据库

- 单库：`pos_v2_db`
- 两个 profile（`mysql` / `v2mysql`）都指向同一个库
- Flyway 管理 schema 迁移
- 43 张表，25 个 migration

### Android

- Android POS 使用 WebView 加载 `:5188` 前端
- Sunmi 设备 + DCS Payment SDK（支付终端集成）
- DCS SDK 通过 AIDL service binding 连接设备上已安装的支付服务

---

## Security Model

### 当前状态

| 层 | 实现 |
|----|------|
| CORS | 限制到已知端口（5187/5188/4183） |
| QR 价格 | 服务端校验，忽略客户端价格 |
| 并发控制 | 桌台操作 `@Lock(PESSIMISTIC_WRITE)` |
| 结算幂等 | DB 唯一约束 + 应用层检查 |
| Webhook | VibeCash HMAC-SHA256 签名验证 |
| PIN | BCrypt 哈希，timing-safe 比较 |
| 审计 | ActionContext 记录每次写操作的来源 |

### 待实现

| 层 | 计划 |
|----|------|
| JWT 认证 | 真实用户表 + Spring Security filter |
| 角色权限 | 基于 staff.role_code 的端点级权限控制 |
| API Rate Limit | PIN 暴力破解防护 |

---

## Data Flow Summary

### 堂食点餐 → 结账

```
POS 选桌 → 选菜 → replaceItems() → ActiveTableOrder(DRAFT)
                                          │
POS 送厨 ────────── submitToKitchen() ────┘
                                          │
                                    SubmittedOrder(UNPAID)
                                          │
POS 结账 ────────── collectForTable() ────┘
                                          │
                              SettlementRecord(SETTLED)
                              SubmittedOrder(SETTLED)
                              TableSession(CLOSED)
                              StoreTable(AVAILABLE)
```

### QR 点餐

```
顾客扫码 → getQrOrderingContext() → 查看菜单
       → submitQrOrdering() → SubmittedOrder(UNPAID)
       → POS 看到 QR 订单 → 结账同上
```

### AI 操作

```
AI Operator → Sense (MCP Read Tools)
           → Think (LLM)
           → Propose (ai_recommendations)
           → Approve (老板审批 / LOW risk 自动)
           → Act (MCP Write Tools + ActionContext)
           → Audit (action_log)
```

### 外部 Agent 交互

```
External Agent → POST /interact → agent_interactions(PENDING)
              → AI Operator 分析 → 生成方案
              → 老板审批 → POST /respond
              → External Agent 收到回复
```

---

## File Tree (Key Files)

```
POS/
├── docker-compose.yml                    # 容器编排
├── pos-backend/
│   ├── Dockerfile                        # Maven 构建 + JRE 运行
│   ├── pom.xml                           # 依赖管理
│   └── src/main/
│       ├── java/com/developer/pos/
│       │   ├── v2/catalog/               # 商品域
│       │   ├── v2/order/                 # 订单域（核心）
│       │   ├── v2/settlement/            # 结算域
│       │   ├── v2/member/                # 会员域
│       │   ├── v2/promotion/             # 促销域
│       │   ├── v2/report/                # 报表域
│       │   ├── v2/store/                 # 门店域
│       │   ├── v2/staff/                 # 员工域
│       │   ├── v2/shift/                 # 班次域
│       │   ├── v2/gto/                   # GTO 域
│       │   ├── v2/mcp/                   # MCP 工具层
│       │   ├── v2/ai/                    # AI Operator 层
│       │   └── v2/agent/                 # Agent + Wallet 层
│       └── resources/
│           ├── application.yml           # 配置（统一 pos_v2_db）
│           └── db/migration/v2/          # 25 个 Flyway migration
├── android-preview-web/                  # POS 前端
├── pc-admin/                             # 商户后台
├── qr-ordering-web/                      # QR 点餐
└── docs/                                 # 52 份设计文档
```
