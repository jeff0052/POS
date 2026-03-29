# MCP Tool Server — Developer Guide

> For: Codex / AI agent programmer working on the POS codebase
> Branch: `feat/p0-mcp-tool-server`
> PR: https://github.com/jeff0052/POS/pull/2

---

## What This PR Does

This PR adds an **MCP (Model Context Protocol) Tool Server** to the POS V2 backend. It wraps every backend domain (Catalog, Order, CRM, Promotion, Settlement, Report, Store) as callable **Tools**, so that an AI Agent (FounderOS) can query data and take actions through a standard protocol.

Think of it as: **every backend service now has a remote control that AI can use.**

---

## Architecture Overview

```
AI Agent (FounderOS)
     │
     ▼  HTTP
┌─────────────────────────┐
│ McpEndpointController    │  GET  /api/v2/mcp/tools
│ (REST layer)             │  POST /api/v2/mcp/tools/{name}/execute
├─────────────────────────┤
│ McpToolRegistry          │  In-memory registry of all tools
├─────────────────────────┤
│ Domain Tool Classes      │  CatalogTools, OrderTools, MemberTools...
│ (7 classes)              │  Each registers tools in @PostConstruct
├─────────────────────────┤
│ Existing V2 Services     │  AdminCatalogReadService, PromotionApplicationService...
│ + Repositories           │  (unchanged — tools just call them)
└─────────────────────────┘
```

**Key principle:** Tools call existing services. We don't duplicate business logic.

---

## File Map

All new files are under `pos-backend/src/main/java/com/developer/pos/v2/`:

```
mcp/
├── model/
│   ├── ActionContext.java         # Who did what: HUMAN/AI, decision source, reason
│   ├── RiskLevel.java             # LOW / MEDIUM / HIGH
│   └── ToolResponse.java          # Generic response wrapper (reserved for future use)
├── infrastructure/
│   ├── ActionLogEntity.java       # JPA entity for action_log table
│   └── JpaActionLogRepository.java
├── interfaces/
│   └── McpEndpointController.java # REST controller — tool listing + execution
├── tools/
│   ├── CatalogTools.java          # list_products, list_categories, toggle_sku_availability
│   ├── PromotionTools.java        # list_promotions, get_promotion_detail, create_promotion_draft
│   ├── MemberTools.java           # list_members, get_member_profile, get_churn_risk_members, update_member_tier
│   ├── OrderTools.java            # get_active_orders, get_order_history, get_table_status
│   ├── SettlementTools.java       # get_daily_revenue, get_payment_breakdown, get_refund_history
│   ├── ReportTools.java           # get_daily_summary, get_product_ranking, compare_periods
│   └── StoreTools.java            # get_store_list, get_table_layout, get_store_config
├── ActionContextHolder.java       # @RequestScope bean — holds context for current request
├── ActionContextAuditListener.java # JPA listener — auto-fills audit columns on save
├── ActionLogService.java          # Logs tool executions to action_log table
├── McpToolRegistry.java           # Central registry of all tools
└── McpServerConfig.java           # Spring @Configuration

common/entity/
└── BaseAuditableEntity.java       # @MappedSuperclass with 4 audit columns
```

---

## Database Changes (3 Flyway Migrations)

### V016: Audit columns on 7 core tables

Adds `actor_type`, `actor_id`, `decision_source`, `change_reason` to:
- active_table_orders, submitted_orders, promotion_rules, members
- settlement_records, skus, store_sku_availability

**Impact on existing data:** None. All columns have defaults (`HUMAN` / `MANUAL` / NULL).

### V017: action_log table

Stores every MCP tool execution (who called what, with what params, what result).

### V018: ai_proposal table

Schema-only, no Java code yet. For P1 AI Operator approval flow.

---

## How Tools Work

### 1. Tool Registration

Each domain tool class is a `@Component` that registers tools in `@PostConstruct`:

```java
@Component
public class CatalogTools {
    private final McpToolRegistry registry;
    private final AdminCatalogReadService readService;

    // constructor injection...

    @PostConstruct
    public void registerTools() {
        registry.register(new ToolDefinition(
            "list_products",                    // tool name
            "List all products for a store",    // description (shown to AI)
            "catalog",                          // domain
            "QUERY",                            // category: QUERY / ANALYZE / ACTION
            null,                               // risk level (null for non-ACTION)
            this::listProducts                  // handler function
        ));
    }

    private Object listProducts(Map<String, Object> params) {
        Long storeId = toLong(params.get("store_id"));
        return readService.getProductsByStore(storeId);
    }
}
```

### 2. Tool Execution

AI agent calls `POST /api/v2/mcp/tools/list_products/execute`:

```json
{
  "params": { "store_id": 1 },
  "context": {
    "actorType": "AI",
    "actorId": "menu-advisor",
    "decisionSource": "AI_RECOMMENDATION",
    "reason": "Checking product performance"
  }
}
```

The controller:
1. Looks up tool in registry
2. Sets ActionContext into request-scoped holder
3. Calls the handler function
4. Logs the execution to `action_log` (for ACTION tools)
5. Returns the result

### 3. Audit Trail

Every entity that extends `BaseAuditableEntity` automatically gets audit columns filled by `ActionContextAuditListener` during JPA persist/update.

---

## Tool Categories

| Category | Can AI auto-execute? | Example |
|----------|---------------------|---------|
| **QUERY** | Yes, freely | list_products, get_member_profile |
| **ANALYZE** | Yes, freely | get_churn_risk_members, get_product_ranking |
| **ACTION** | Depends on risk level | toggle_sku_availability (MEDIUM), create_promotion_draft (MEDIUM) |

Risk levels for ACTION tools:
- **LOW**: AI can auto-execute (e.g., generate report summary)
- **MEDIUM**: AI creates draft, owner approves (e.g., promotions, price changes)
- **HIGH**: AI can only suggest, never execute (e.g., refunds, payment config)

**Note:** Risk enforcement is NOT implemented in P0. The controller executes all tools regardless of risk level. Risk-based approval flow is P1 scope.

---

## Known Limitations (to fix in P1)

| Issue | Location | What to do |
|-------|----------|-----------|
| `get_churn_risk_members` returns ALL members | MemberTools.java | Need `last_visit_date` column + query to find actually churning members |
| `get_refund_history` is a placeholder | SettlementTools.java | Need a real refund entity/table |
| `compare_periods` compares stores, not time periods | ReportTools.java | Rename or rewrite to compare date ranges |
| Settlement/Report tools load full tables | SettlementTools, ReportTools | Add repository query methods with storeId + date filters |
| No auth on MCP endpoints | McpEndpointController | Add API key auth before exposing to external callers |
| No input validation | All tool handlers | Add null checks on required params |
| No pagination | Query tools | Add limit/offset params |
| `ai_proposal` table has no Java code | V018 migration | Build entity + repository + service in P1 |
| Duplicate `get_table_status` in Order and Store | OrderTools, StoreTools | Remove one |

---

## How to Add a New Tool

1. Find or create a `*Tools.java` file under `mcp/tools/`
2. Inject the domain service(s) you need
3. Register tools in `@PostConstruct`:

```java
registry.register(new ToolDefinition(
    "your_tool_name",           // unique name, snake_case
    "What this tool does",      // description for AI
    "your_domain",              // catalog/order/member/promotion/settlement/report/store
    "QUERY",                    // QUERY / ANALYZE / ACTION
    null,                       // RiskLevel.LOW/MEDIUM/HIGH (only for ACTION)
    this::yourHandler           // method reference
));
```

4. Write the handler:

```java
private Object yourHandler(Map<String, Object> params) {
    Long id = toLong(params.get("id"));
    return someService.getData(id);
}
```

5. For mutation handlers (ACTION), extract to a `@Transactional` method:

```java
@Transactional
public Object doYourMutation(Map<String, Object> params) {
    // read-then-write safely in a transaction
}
```

6. Test: `GET /api/v2/mcp/tools` should list your new tool.

---

## How to Test

```bash
# Compile
cd pos-backend
docker run --rm -v "$(pwd)":/app -w /app maven:3.9.9-eclipse-temurin-17 mvn compile -Dspring.profiles.active=mock

# Run unit tests
docker run --rm -v "$(pwd)":/app -w /app maven:3.9.9-eclipse-temurin-17 mvn test -Dspring.profiles.active=mock

# With Docker Compose (full stack + MySQL)
docker compose up -d
curl http://localhost:8094/api/v2/mcp/tools
curl -X POST http://localhost:8094/api/v2/mcp/tools/list_products/execute \
  -H "Content-Type: application/json" \
  -d '{"params": {"store_id": 1}}'
```

---

## Entity Modifications

7 existing entities now `extends BaseAuditableEntity`:

- `SkuEntity`
- `StoreSkuAvailabilityEntity`
- `ActiveTableOrderEntity`
- `SubmittedOrderEntity`
- `PromotionRuleEntity`
- `MemberEntity`
- `SettlementRecordEntity`

**If you're modifying these entities:** Keep the `extends BaseAuditableEntity` and `@EntityListeners(ActionContextAuditListener.class)`. These add 4 audit columns but don't change any existing behavior.

**If you're creating new entities** that represent core business data: extend `BaseAuditableEntity` too.

---

## Merge Notes

This PR creates **26 new files** and modifies **7 entity files**. If you have conflicts on the entity files, the resolution is simple: keep both your changes AND the `extends BaseAuditableEntity` + `@EntityListeners` additions.
