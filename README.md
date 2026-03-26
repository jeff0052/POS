# FounderPOS

AI-Driven Restaurant Operating System

---

## What Is This

FounderPOS 不是传统 POS。它是一个给每家餐厅配一个 AI 运营合伙人的系统。老板只管审批，AI 管运营。

```
Layer 4 ─ Agent Identity (每家餐厅 = 1 Agent + 1 Wallet)
Layer 3 ─ AI Operator (5 个顾问角色)
Layer 2 ─ MCP Tool Server (14 个工具，AI 操作业务系统的接口)
Layer 1 ─ Transaction Foundation (传统 POS 后端)
```

## Project Structure

```
POS/
├── pos-backend/           # Spring Boot 后端 (Java 17)
│   └── src/main/java/com/developer/pos/
│       ├── v1/            # V1 legacy (逐步废弃)
│       └── v2/            # V2 主力代码
│           ├── order/     # 订单域 (核心)
│           ├── catalog/   # 商品/SKU 域
│           ├── member/    # 会员/CRM 域
│           ├── promotion/ # 促销域
│           ├── settlement/# 结算域
│           ├── report/    # 报表域
│           ├── staff/     # 员工/权限域
│           ├── shift/     # Cashier 班次域
│           ├── gto/       # GTO 税务导出域
│           ├── platform/  # 平台管理域
│           ├── mcp/       # MCP Tool Server (Layer 2)
│           ├── ai/        # AI Operator (Layer 3)
│           └── agent/     # Restaurant Agent + Wallet (Layer 4)
│
├── android-pos/           # Android POS 端 (Kotlin + Compose)
├── android-preview-web/   # POS 前端 (React, 运行在 WebView)
├── pc-admin/              # 商户后台 (React)
├── qr-ordering-web/       # 顾客扫码点餐 (React)
├── platform-admin/        # 平台管理后台 (scaffold)
└── docs/                  # 设计文档
```

## Tech Stack

| 组件 | 技术 |
|------|------|
| Backend | Java 17, Spring Boot 3, Spring Data JPA, MySQL 8 |
| Android | Kotlin, Jetpack Compose, Hilt, Retrofit |
| Frontend | React, TypeScript, Vite |
| Database | MySQL 8 + Flyway migration |
| Payment | DCS SDK (card), VibeCash (QR), Cash |
| Container | Docker + docker-compose |

## Backend Domain Architecture

```
11 个域，按微服务边界设计，模块化单体交付：

Order (交易核心) ──> Settlement (结算) ──> Report (报表) ──> GTO (税务)
  ↑                     ↑
Catalog/SKU (商品) ──> Promotion (促销)
  ↑
Member/CRM (会员) ──> Staff (员工) ──> Shift (班次)
  ↑
Platform Admin (平台管理)
```

**关键设计决策：**
- 订单域是交易中枢，POS + QR + 外卖三入口共享
- 一桌一单（Active Table Order）
- 钱用 cents (long)，不用浮点
- V2 采用 DDD 四层：interfaces / application / domain / infrastructure
- 每个域有自己的 controller / service / dto / entity / repository

## MCP Tools (Layer 2)

AI 通过 14 个标准化工具操作业务系统：

| 工具 | 域 | 类型 |
|------|-----|------|
| catalog.list_skus | Catalog | Read |
| catalog.get_sku | Catalog | Read |
| catalog.update_sku_status | Catalog | Write (MEDIUM) |
| order.list_orders | Order | Read |
| order.get_order | Order | Read |
| member.list_members | CRM | Read |
| member.get_member | CRM | Read |
| member.update_tier | CRM | Write (MEDIUM) |
| report.daily_summary | Report | Read |
| report.sku_performance | Report | Read |
| shift.current | Shift | Read |
| promotion.list_rules | Promotion | Read |
| promotion.create_rule | Promotion | Write (MEDIUM) |
| promotion.update_rule | Promotion | Write (MEDIUM) |

## AI Operator (Layer 3)

5 个顾问角色，通过 MCP Tools 感知数据、生成建议：

| 角色 | 职责 |
|------|------|
| MENU_ADVISOR | 菜单优化（下架亏损菜、推套餐组合） |
| PROMOTION_ADVISOR | 营销策划（客流低时自动生成活动方案） |
| MEMBER_ADVISOR | 会员运营（识别流失、触发召回） |
| BUSINESS_ADVISOR | 经营分析（日报周报、异常预警） |
| KITCHEN_ADVISOR | 出品优化（出菜时间、退菜分析） |

**运行模式：** Sense → Think → Propose → Approve → Act

## Agent + Wallet (Layer 4)

每家餐厅注册为一个 Agent，拥有独立身份和钱包：

- **Agent：** 对外交互（接受预定、包场、询价）
- **Wallet：** 收款、支出、分账、预算控制
- **风险分级：** LOW 自动处理 / MEDIUM 老板审批 / HIGH 转人工

## Database

MySQL 8, schema 通过 Flyway 管理。Migration 文件在：
```
pos-backend/src/main/resources/db/migration/
├── V001__merchants.sql
├── V002__stores.sql
├── ...
└── V045__agent_wallet.sql
```

## Running Locally

```bash
# 启动 MySQL
docker-compose up -d mysql

# 启动后端
cd pos-backend && ./mvnw spring-boot:run

# 启动商户后台
cd pc-admin && npm install && npm run dev

# 启动 QR 点餐
cd qr-ordering-web && npm install && npm run dev
```

## Design Documents

| # | 文档 | 内容 |
|---|------|------|
| 02 | Market Requirements (V1) | 传统 POS 市场需求 |
| 15 | Service-Oriented Architecture | 微服务边界设计，模块化单体交付 |
| 16 | Backend Domain Breakdown | 11 个后端域拆分 |
| 17 | Backend Module Refactor Plan | 重构优先级和策略 |
| 22 | AI-Ready Design Principle | AI-ready 产品和 API 设计原则 |
| 25 | Delivery Integration | 外卖接入架构 |
| 39 | Unified Payment Architecture | 统一支付域 + 多适配器 |
| 52 | Architecture Review Report | 全栈审查报告 |
| **53** | **MRD V2 (AI Vision)** | **AI 驱动餐饮 OS 市场需求** |
| **54** | **MCP Tool Server Design** | **14 个 MCP 工具架构** |
| **55** | **AI Operator Design** | **5 个顾问角色设计** |
| **56** | **Agent + Wallet Design** | **Agent 身份 + 钱包架构** |

## PR Status

| PR | 内容 | 状态 |
|----|------|------|
| #3 | 8 个 CRITICAL 安全修复 | Review 通过，待合并 |
| #4 | Cashier 班次 + GTO 税务导出 | Review 通过，待合并 |
| #5 | 员工/角色/权限 | Review 通过，待合并 |
| #6 | 真实认证 + 多商户多门店 | Review 通过，待合并 |
| #7 | 平台管理后台 | Review 通过，待合并 |
| #8 | 商户后台真数据 + Dashboard | Review 通过，待合并 |
| #9 | 会员充值/积分/等级 | Review 通过，待合并 |
| #10 | 促销引擎增强 | Review 通过，待合并 |
| #11 | MCP Tool Server | Review 通过，待合并 |
| #13 | AI Operator | Review 通过，待合并 |
| #14 | Agent + Wallet | Review 通过，待合并 |

## For Codex / Agent Programmers

如果你是 AI agent 接手这个项目：

1. **先读 docs/52 审查报告** — 知道哪些坑
2. **V2 代码是主力** — V1 在逐步废弃
3. **每个域独立目录** — 改 order 不用看 member
4. **所有写操作需要 ActionContext** — 标记来源（HUMAN/AI）
5. **PR 有合并顺序** — #3 先合（安全），#11 在 #13 之前（MCP 是 AI 的依赖）
6. **钱用 cents** — `long amountCents`，不要用 double
7. **新功能建分支** — 不要直接推 main
