# POS Developer README

> For AI agents (Codex, Claude) and human developers working on this project.

## Project Overview

FounderPOS — AI-driven restaurant operating system. Not just a POS, but a platform where AI manages restaurant operations and the owner only approves.

**Repo:** https://github.com/jeff0052/POS

## Project Structure

```
POS/
├── pos-backend/          Spring Boot 3.3.3, Java 17, Maven, MySQL
├── android-pos/          Android POS app (Kotlin + Compose + WebView)
├── android-preview-web/  POS frontend running in WebView (React + TypeScript)
├── pc-admin/             Merchant admin console (React)
├── qr-ordering-web/      Customer QR ordering (React)
├── platform-admin/       Platform admin console (scaffold)
└── docs/                 Architecture docs, specs, plans
```

## Backend Architecture

```
pos-backend/src/main/java/com/developer/pos/
├── v2/                   ← V2 code (active, DDD-style)
│   ├── order/            Transaction core — active table orders, submitted orders
│   ├── catalog/          Products, SKUs, categories, store availability
│   ├── member/           CRM — members, accounts, points, recharge, tiers
│   ├── promotion/        Promotion rules, conditions, rewards, evaluation
│   ├── settlement/       Cashier settlement, VibeCash payment, payment attempts
│   ├── store/            Stores, tables
│   ├── report/           Sales reports, daily summary, order monitoring
│   ├── staff/            Staff, roles, permissions (PR #5)
│   ├── shift/            Cashier shifts (PR #4)
│   ├── gto/              GTO tax export (PR #4)
│   ├── platform/         Platform admin (PR #7)
│   ├── mcp/              MCP Tool Server — AI operation layer (PR #11)
│   └── common/           Shared interfaces (UseCase, V2Api)
├── auth/                 Authentication (user table, JWT)
├── order/                ← V1 code (legacy, being phased out)
├── store/                ← V1
├── product/              ← V1
├── category/             ← V1
├── member/               ← V1
└── common/               Shared (ApiResponse, CorsConfig, SecurityConfig)
```

## V2 Domain Layering (per domain)

```
v2/{domain}/
├── interfaces/rest/       Controllers + request DTOs
├── application/
│   ├── service/           Application services (business logic)
│   ├── dto/               Response DTOs
│   └── command/           Write command records
├── domain/
│   ├── model/             Domain models
│   ├── status/            Enums
│   └── policy/            Business rules
└── infrastructure/
    └── persistence/
        ├── entity/        JPA entities
        └── repository/    Spring Data repositories
```

## Key Design Decisions

1. **One active table order per table** — POS and QR share the same order
2. **Money in cents (long)** — no floating point
3. **Snapshot-first** — order items snapshot SKU price at time of ordering
4. **Member discount applied first**, promotion discount applied second
5. **V1 and V2 coexist** — V1 has hardcoded storeId=1001 (default), V2 is multi-store
6. **ActionContext on MCP tools** — every AI operation is auditable

## Database

- MySQL, managed by Flyway migrations
- Migrations in: `pos-backend/src/main/resources/db/migration/v2/`
- Current: V001-V015 (existing), V016-V035 (in PRs)
- Use `spring.profiles.active=v2mysql` to enable Flyway

## How to Run

```bash
cd pos-backend
mvn spring-boot:run -Dspring-boot.run.profiles=v2mysql
```

Requires: Java 17, Maven, MySQL running on localhost:3306 with database `pos_v2_db`.

## API Endpoints

### Public (no auth)
- `POST /api/auth/login` — login
- `GET /api/v2/qr-ordering/**` — QR ordering
- `POST /api/v2/payments/vibecash/webhook` — payment webhook

### Authenticated (JWT Bearer token)
- `/api/v2/stores/{storeId}/tables/{tableId}/**` — table operations
- `/api/v2/admin/**` — merchant admin
- `/api/v2/members/**` — member management
- `/api/v2/reports/**` — reports
- `/api/v2/mcp/**` — MCP Tool Server (AI operations)

### MCP Tool Server
- `GET /api/v2/mcp/tools` — list available tools
- `POST /api/v2/mcp/execute` — invoke a tool
- `POST /api/v2/mcp/batch` — invoke multiple tools

## Open PRs (not yet merged)

| PR | Branch | Content |
|----|--------|---------|
| #3 | fix/critical-review-fixes | Security fixes (JWT, CORS, price validation, concurrency) |
| #4 | feat/cashier-shift | Cashier shifts + GTO tax export |
| #5 | feat/staff-permission | Staff/role/permission with PIN login |
| #6 | feat/real-auth-multistore | Real user table + multi-store support |
| #7 | feat/platform-admin | Platform admin backend |
| #8 | feat/merchant-admin-data | Merchant dashboard with real data |
| #9 | feat/member-lifecycle | Auto tier upgrade, points earning, balance deduction |
| #10 | feat/promotion-enhancement | Percentage discount, usage limits |
| #11 | feat/mcp-tool-server | MCP Tool Server (14 AI tools) |

**Merge order:** #3 → #6 → #5 → #4 → #7 → #8 → #9 → #10 → #11

## Rules for Contributors

1. **Never modify V1 code** unless fixing backward compatibility
2. **Follow V2 domain layering** — don't put logic in controllers
3. **Each domain is independent** — cross-domain calls go through application services
4. **Money is always cents (long)** — never use double/float
5. **New features go in their own branch** — never push directly to main
6. **Flyway migrations are append-only** — never edit existing migrations
7. **Use explicit @Column annotations** — don't rely on implicit naming
