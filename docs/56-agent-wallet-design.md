# Restaurant Agent + Wallet Design

Date: 2026-03-26
Status: Implemented (PR #14)

---

## 1. Purpose

定义每家餐厅的 Agent 身份和 Wallet 架构。每家餐厅 = 1 个 Agent + 1 个 Wallet，具备对外交互能力和财务自主性。

## 2. Core Concept

```
┌─────────────────────────────────┐
│      Restaurant Agent           │
│   "老王烧烤"                     │
│                                 │
│  🧠 AI Operator (内部运营)       │
│  🔧 MCP Tools (操作业务系统)     │
│  💰 Wallet (收款/分账/预算)      │
│  🌐 对外协议 (接受外部 Agent)    │
└─────────────────────────────────┘
```

## 3. Agent Identity

### 3.1 Data Model

```
restaurant_agents {
    agent_id:          VARCHAR(64)   -- 全局唯一 Agent ID
    store_id:          BIGINT        -- 关联门店
    merchant_id:       BIGINT        -- 关联商户
    agent_name:        VARCHAR(128)  -- Agent 显示名（餐厅名）
    agent_type:        VARCHAR(32)   -- RESTAURANT
    capabilities_json: JSON          -- 能力声明
    status:            VARCHAR(32)   -- ACTIVE / SUSPENDED
    description:       TEXT          -- 餐厅描述
    contact_info_json: JSON          -- 联系方式
    business_hours_json: JSON        -- 营业时间
}
```

### 3.2 Capabilities（能力声明）

每家 Agent 声明自己能做什么：

```json
{
    "canAcceptReservation": true,
    "canAcceptPrivateEvent": true,
    "canAcceptDelivery": false,
    "canAcceptCatering": false,
    "maxCapacity": 80,
    "cuisineType": ["Chinese", "Szechuan"],
    "priceRange": "$$"
}
```

外部 Agent 通过能力声明发现和筛选合适的餐厅。

## 4. Wallet

### 4.1 Data Model

```
agent_wallets {
    wallet_id:         VARCHAR(64)   -- 钱包 ID
    agent_id:          VARCHAR(64)   -- 关联 Agent
    balance_cents:     BIGINT        -- 当前余额（分）
    currency_code:     VARCHAR(3)    -- SGD
    daily_limit_cents: BIGINT        -- 日支出限额
    status:            VARCHAR(32)   -- ACTIVE / FROZEN
}

wallet_transactions {
    transaction_id:     VARCHAR(64)  -- 交易 ID
    wallet_id:          VARCHAR(64)  -- 关联钱包
    transaction_type:   VARCHAR(32)  -- INCOME / EXPENSE / TRANSFER
    amount_cents:       BIGINT       -- 金额
    balance_after_cents: BIGINT      -- 交易后余额
    description:        VARCHAR(255) -- 描述
    reference_type:     VARCHAR(64)  -- 关联类型
    reference_id:       VARCHAR(64)  -- 关联 ID
}
```

### 4.2 Wallet 职责

| 功能 | 说明 |
|------|------|
| 收款 | 顾客支付、平台结算到账 |
| 支出 | AI 营销预算消耗、供应商付款 |
| 分账 | 平台抽成、投资者分润（RWA） |
| 预算控制 | AI 操作有日支出上限 |
| 对账 | 自动核对各渠道到账 |

### 4.3 并发安全

Wallet 余额操作使用悲观锁（`SELECT ... FOR UPDATE`），防止并发扣款导致负余额。

## 5. Agent Interaction

### 5.1 交互模型

```
外部 Agent
    │
    ▼ (发起交互请求)
Restaurant Agent
    │
    ▼ (AI Operator 分析)
风险评估
    │
    ├── LOW  → 自动回复
    ├── MEDIUM → 生成方案 → 老板审批
    └── HIGH → 转人工
```

### 5.2 Interaction Data Model

```
agent_interactions {
    interaction_id:    VARCHAR(64)   -- 交互 ID
    agent_id:          VARCHAR(64)   -- 本店 Agent
    external_agent_id: VARCHAR(128)  -- 对方 Agent
    interaction_type:  VARCHAR(32)   -- RESERVATION / INQUIRY / ...
    request_json:      JSON          -- 请求内容
    response_json:     JSON          -- 回复内容
    risk_level:        VARCHAR(16)   -- LOW / MEDIUM / HIGH
    status:            VARCHAR(32)   -- PENDING / AUTO_RESPONDED / RESPONDED / REJECTED
    auto_handled:      BOOLEAN       -- 是否 AI 自动处理
}
```

### 5.3 交互类型

| 类型 | 风险 | 处理方式 |
|------|------|---------|
| RESERVATION | LOW | AI 自动查位、自动确认 |
| INQUIRY | LOW | AI 自动回答（营业时间、菜单、价格） |
| PRIVATE_EVENT | MEDIUM | AI 生成报价方案 → 老板审批 |
| CATERING | MEDIUM | AI 分析需求 → 生成方案 → 老板审批 |
| PARTNERSHIP | HIGH | 转人工处理 |
| SUPPLIER_QUOTE | MEDIUM | AI 比价分析 → 老板审批 |

## 6. API Design

### 6.1 Agent Management

```
POST   /api/v2/agents/register          -- 注册 Agent
GET    /api/v2/agents/{agentId}          -- 查询 Agent
PUT    /api/v2/agents/{agentId}          -- 更新 Agent 信息
```

### 6.2 Wallet

```
GET    /api/v2/agents/{agentId}/wallet              -- 查询余额
POST   /api/v2/agents/{agentId}/wallet/income       -- 收入
POST   /api/v2/agents/{agentId}/wallet/expense      -- 支出
GET    /api/v2/agents/{agentId}/wallet/transactions  -- 交易记录
```

### 6.3 Interactions

```
POST   /api/v2/agents/{agentId}/interactions              -- 发起交互
GET    /api/v2/agents/{agentId}/interactions/pending       -- 待处理
POST   /api/v2/agents/{agentId}/interactions/{id}/respond  -- 回复
```

## 7. Future: Agent-to-Agent Protocol (P3)

当前 Agent 交互通过 REST API。P3 阶段将定义标准化的 Agent-to-Agent 协议：

- **Discovery：** Agent 如何发现其他 Agent（能力注册表）
- **Handshake：** Agent 之间如何建立信任
- **Message Format：** 标准化请求/响应格式
- **Settlement：** 跨 Agent 的 Wallet 结算

## 8. Future: Restaurant Network (P4)

多家 Agent 形成网络后：

- **匿名数据共享：** 经营数据去标识化后贡献给网络
- **集体智慧：** 网络数据反哺每家店的 AI Operator
- **联合采购：** 多家 Agent 联合向供应商 Agent 询价
- **互相导流：** 满客时推荐同类型餐厅
