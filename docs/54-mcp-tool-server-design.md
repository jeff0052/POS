# MCP Tool Server Design

Date: 2026-03-26
Status: Implemented (PR #11)

---

## 1. Purpose

定义 POS 后端的 MCP（Model Context Protocol）Tool Server 架构。这是 AI 层的地基 — 让 AI Operator 和外部 Agent 能够通过标准化工具接口操作 POS 的每个业务域。

## 2. Design Principle

- **Layer 2 是枢纽：** 人类操作（后台/POS）和 AI 操作都走同一套 MCP Tools
- **AI 不直接碰数据库：** 必须通过 Tools 操作，权限和审计天然统一
- **统一 ActionContext：** 每个操作都标记来源（HUMAN/AI）、决策类型、审批状态

## 3. Architecture

```
┌─────────────────────────────────┐
│      AI Operator / Agent        │
│     (Layer 3 / Layer 4)         │
└──────────────┬──────────────────┘
               │ MCP Protocol
               ▼
┌─────────────────────────────────┐
│        MCP Tool Server          │
│  ┌──────────────────────────┐   │
│  │    McpToolRegistry       │   │
│  │    (tool discovery)      │   │
│  └──────────┬───────────────┘   │
│             │                   │
│  ┌──────────▼───────────────┐   │
│  │    McpTool Interface     │   │
│  │    execute(params, ctx)  │   │
│  └──────────┬───────────────┘   │
│             │                   │
│  ┌──────────▼───────────────┐   │
│  │   ActionContext          │   │
│  │   (audit trail)          │   │
│  └──────────────────────────┘   │
└──────────────┬──────────────────┘
               │ Spring Service calls
               ▼
┌─────────────────────────────────┐
│     POS V2 Backend (Layer 1)    │
│  Order / Catalog / CRM / ...    │
└─────────────────────────────────┘
```

## 4. Core Components

### 4.1 McpTool Interface

```java
public interface McpTool {
    String name();
    String description();
    McpResponse execute(Map<String, Object> params, ActionContext ctx);
}
```

### 4.2 ActionContext

```java
ActionContext {
    actorType:       HUMAN | AI
    actorId:         "jeff" | "menu-advisor"
    decisionSource:  MANUAL | AI_RECOMMENDATION | AI_AUTO
    recommendationId: nullable
    approvalStatus:  APPROVED | PENDING | REJECTED
    changeReason:    "周三客流低，建议满减"
}
```

### 4.3 McpToolRegistry

所有 Tool 通过 Spring `@Component` 自动注册。Registry 提供：
- `get(toolName)` — 按名称获取
- `listAll()` — 列出所有可用工具
- `listByDomain(domain)` — 按域筛选

## 5. Tool Inventory（14 个）

### 5.1 Read Tools（查询类）

| Tool | Domain | 说明 |
|------|--------|------|
| `catalog.list_skus` | Catalog | 查询 SKU 列表（含销量、状态） |
| `catalog.get_sku` | Catalog | 查询单个 SKU 详情 |
| `order.list_orders` | Order | 查询订单列表（按日期/状态） |
| `order.get_order` | Order | 查询单个订单详情 |
| `member.list_members` | CRM | 查询会员列表（按等级/活跃度） |
| `member.get_member` | CRM | 查询单个会员详情（含消费历史） |
| `report.daily_summary` | Report | 查询日报（营收/客流/客单价） |
| `report.sku_performance` | Report | 查询 SKU 表现（销量/毛利/退菜率） |
| `shift.current` | Shift | 查询当前班次信息 |
| `promotion.list_rules` | Promotion | 查询促销规则列表 |

### 5.2 Write Tools（操作类）

| Tool | Domain | 风险 | 说明 |
|------|--------|------|------|
| `catalog.update_sku_status` | Catalog | MEDIUM | 上下架 SKU |
| `promotion.create_rule` | Promotion | MEDIUM | 创建促销规则 |
| `promotion.update_rule` | Promotion | MEDIUM | 修改促销规则 |
| `member.update_tier` | CRM | MEDIUM | 调整会员等级 |

### 5.3 风险分级

| 风险 | AI 权限 | 举例 |
|------|---------|------|
| LOW | 直接执行 | 所有 Read Tools、生成报表摘要 |
| MEDIUM | 生成草案 → 人类审批 | 创建促销、调整价格、上下架 |
| HIGH | 只能建议 | 退款、支付配置、GTO 税务 |

## 6. API Endpoint

```
POST /api/v2/mcp/tools/{toolName}/execute
```

Request:
```json
{
    "params": { "storeId": 1001, "date": "2026-03-26" },
    "context": {
        "actorType": "AI",
        "actorId": "menu-advisor",
        "decisionSource": "AI_RECOMMENDATION"
    }
}
```

Response:
```json
{
    "success": true,
    "data": { ... },
    "toolName": "report.daily_summary",
    "executedAt": "2026-03-26T10:00:00+08:00"
}
```

## 7. Security

- 所有 Write Tools 需要 JWT 认证
- ActionContext 强制记录到审计日志
- AI 操作的 Write Tools 默认需要人类审批（除非 LOW risk）
- Tool 执行结果不包含敏感数据（密码、支付凭证）

## 8. Extension

新增 Tool 只需：
1. 实现 `McpTool` 接口
2. 加 `@Component` 注解
3. Registry 自动发现

无需改路由、改配置、改注册表。
