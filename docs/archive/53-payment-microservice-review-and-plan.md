# Payment Module Review & Microservice Extraction Plan

Date: 2026-03-27
Author: Claude

---

## Part 1: Current State Review

### 现在支付代码在哪里

所有支付逻辑都在 `pos-backend` 的 `v2/settlement/` 包里，和订单、桌台、会员混在同一个 Spring Boot 进程中。

```
pos-backend/src/main/java/com/developer/pos/v2/settlement/
├── application/
│   ├── service/
│   │   ├── CashierSettlementApplicationService.java    ← 结账核心（520行）
│   │   ├── VibeCashPaymentApplicationService.java      ← VibeCash QR 集成（230行）
│   │   └── RefundApplicationService.java               ← 退款（127行）
│   ├── command/
│   │   ├── CollectCashierSettlementCommand.java
│   │   └── CreateRefundCommand.java
│   └── dto/ (8个DTO)
├── infrastructure/persistence/
│   ├── entity/
│   │   ├── SettlementRecordEntity.java
│   │   ├── PaymentAttemptEntity.java
│   │   └── RefundRecordEntity.java
│   └── repository/ (3个Repository)
└── interfaces/rest/
    ├── TableSettlementV2Controller.java
    ├── CashierSettlementV2Controller.java
    ├── VibeCashWebhookV2Controller.java
    └── RefundV2Controller.java
```

### 三条支付链路

| 方式 | 当前状态 | 流程 |
|------|---------|------|
| **现金 CASH** | ✅ 可用 | POS 选 CASH → `collectForTable` 直接结账 |
| **VibeCash QR** | ⚠️ 缺凭证 | POS 发起 → HTTP 创建支付链接 → 前端显示 QR → 顾客扫码 → Webhook 回调 → 自动结账 |
| **DCS 刷卡** | ❌ 未接通 | SDK 版本不匹配，Android AIDL 绑定失败 |

### 当前的问题

1. **VibeCash 逻辑直接嵌在 settlement service 里** — HTTP 调用、webhook 处理、状态机全在一个类里
2. **没有 provider 抽象层** — 加新支付方式要直接改 settlement 代码
3. **DCS 是 Android 端 SDK** — 和后端逻辑在不同进程，当前没有桥接
4. **退款只改本地数据库** — 没有调 VibeCash/DCS 的退款 API
5. **支付凭证直接放在 application.yml** — 没有安全隔离

---

## Part 2: Microservice Extraction Plan

### 为什么要独立微服务

1. **外部团队开发** — 支付团队需要独立仓库、独立部署、独立测试
2. **安全隔离** — 支付凭证（DCS merchant key, VibeCash secret）不应该出现在主后端
3. **独立扩缩** — 支付高峰期可以单独扩容
4. **合规** — 支付审计范围缩小到一个服务
5. **多 provider 演进** — DCS、VibeCash、Stripe、PayNow 各自独立迭代

### 架构

```
┌─────────────┐     ┌───────────────┐     ┌──────────────────┐
│  POS 后端    │────▶│  Payment API  │────▶│  Provider Adapters│
│ (订单/桌台)  │◀────│  (微服务)      │◀────│  DCS / VibeCash  │
└─────────────┘     └───────┬───────┘     │  / Cash / Stripe │
                            │             └──────────────────┘
                            │
                     ┌──────▼──────┐
                     │  Payment DB │
                     │ (独立数据库)  │
                     └─────────────┘
```

### Payment 微服务的职责

| 职责 | 说明 |
|------|------|
| 创建支付意图 | POS 后端说"这桌要付 $50"，Payment 服务创建 PaymentIntent |
| 路由到 Provider | 根据 paymentMethod/paymentScheme 选择 DCS/VibeCash/Cash |
| 管理支付状态机 | CREATED → PENDING → SUCCEEDED / FAILED / EXPIRED |
| 接收 Webhook | VibeCash/Stripe 的回调统一由 Payment 服务处理 |
| 通知 POS | 支付成功后回调 POS 后端触发结账 |
| 退款 | 调 Provider 退款 API + 记录退款 |
| 对账 | 每日拉 Provider 流水和本地记录比对 |

### POS 后端的职责（不变）

| 职责 | 说明 |
|------|------|
| 订单管理 | 活动桌单、提交、状态流转 |
| 结账 | 收到"支付成功"通知后执行结账逻辑 |
| 报表 | 读取结算记录生成报表 |
| 桌台管理 | 清台、状态更新 |

### 对接协议

#### POS → Payment（创建支付）

```
POST /api/v1/payments/intents
{
  "merchantId": 1,
  "storeId": 1001,
  "tableId": 42,
  "sessionRef": "SESS-001",
  "amountCents": 5000,
  "currency": "SGD",
  "paymentMethod": "QR",        // CARD | QR | CASH
  "paymentScheme": "WECHAT_QR", // VISA | ALIPAY_QR | PAYNOW_QR | ...
  "callbackUrl": "http://pos-backend:8080/api/v2/internal/payment-callback",
  "metadata": { ... }
}
```

#### Payment → POS（回调通知）

```
POST /api/v2/internal/payment-callback
X-Payment-Signature: HMAC(payload, shared_secret)
{
  "paymentIntentId": "PI-xxx",
  "status": "SUCCEEDED",
  "providerTransactionId": "vibecash-tx-123",
  "amountCents": 5000,
  "paidAt": "2026-03-27T14:30:00+08:00",
  "paymentMethod": "QR",
  "paymentScheme": "WECHAT_QR"
}
```

POS 后端收到此回调后执行 `collectForTable`，和现在的 webhook 处理逻辑一样，但来源变了。

### 数据库拆分

**Payment 微服务独立数据库：**
- `payment_intents` — 支付意图（替代现在的 `payment_attempts`）
- `payment_transactions` — Provider 交易记录
- `refund_requests` — 退款请求
- `webhook_events` — 所有 Provider 回调原始数据

**POS 后端保留：**
- `settlement_records` — 结算记录（支付成功后才写）
- 其他业务表不变

### Provider Adapter 接口

```java
public interface PaymentProviderAdapter {
    String providerCode();                              // "DCS", "VIBECASH", "CASH"
    CreatePaymentResult createPayment(CreatePaymentRequest request);
    QueryPaymentResult queryPayment(String providerTransactionId);
    RefundResult refund(RefundRequest request);
    boolean supportsMethod(String paymentMethod);       // CARD, QR, CASH
    boolean supportsScheme(String paymentScheme);       // VISA, WECHAT_QR, etc.
}
```

每个 Provider 实现一个 Adapter：
- `DcsCardAdapter` — 通过 HTTP 调 Android 端中转服务（或直接 AIDL）
- `VibeCashQrAdapter` — HTTP 调 VibeCash API
- `CashManualAdapter` — 无外部调用，直接标记成功
- `StripeAdapter`（未来）

### 技术栈

| 组件 | 选型 | 理由 |
|------|------|------|
| 语言 | Java 17 / Spring Boot 3.3 | 和主后端一致，外部团队上手快 |
| 数据库 | MySQL 8 | 和主后端一致 |
| 通信 | REST + Webhook | 简单可靠，不需要消息队列（V1） |
| 安全 | 服务间 HMAC 签名 | 轻量级，不需要 OAuth |
| 部署 | Docker，独立容器 | 和主后端并排跑 |

### 迁移策略

#### Phase 1：搭建微服务骨架（外部团队）
- 独立仓库 `pos-payment-service`
- PaymentIntent CRUD + 状态机
- CashManualAdapter（最简单的，验证链路）
- POS 后端加 `/internal/payment-callback` 接收通知
- 本地 docker-compose 联调

#### Phase 2：迁移 VibeCash（外部团队）
- VibeCashQrAdapter 搬到微服务
- Webhook 接收搬到微服务
- POS 后端删除 `VibeCashPaymentApplicationService`
- 端到端测试

#### Phase 3：接入 DCS（外部团队 + DCS 厂商）
- DcsCardAdapter
- Android 端 SDK 调用结果通过 HTTP 传给 Payment 微服务
- Payment 微服务记录交易后回调 POS

#### Phase 4：退款 + 对账
- 各 Provider 退款 API 对接
- 每日自动对账
- 异常告警

### 对接文档交付物

外部团队需要的文档：

1. **Payment Service API Spec** — OpenAPI 3.0，包含所有端点
2. **Provider Adapter Interface** — Java 接口 + 文档
3. **Callback Protocol** — POS 后端期望的回调格式
4. **状态机定义** — PaymentIntent 的完整状态流转图
5. **测试用例** — 每种 Provider 的正常/异常场景
6. **Docker Compose** — 本地联调环境配置

---

## Part 3: 对 POS 后端的改动

POS 后端需要改的很少：

| 改动 | 说明 |
|------|------|
| 加 `/internal/payment-callback` 接口 | 接收 Payment 微服务的支付成功通知 |
| 加 HMAC 签名验证 | 验证回调来源 |
| 前端改 QR 支付调用路径 | 从直接调 POS 后端改为调 Payment 微服务 |
| 删除 `VibeCashPaymentApplicationService` | Phase 2 完成后 |
| 保留 `CashierSettlementApplicationService` | 结账逻辑不动 |

---

## 决策点（需要你确认）

1. **Payment 微服务是新仓库还是放在 POS 仓库的子模块？** — 建议新仓库（外部团队独立工作）
2. **通信方式：REST 回调 vs 消息队列？** — 建议 V1 用 REST 回调，V2 再考虑 Kafka/RabbitMQ
3. **DCS 适配器放在 Payment 微服务还是 Android 端？** — 建议 Android 端调 DCS SDK → 结果 POST 给 Payment 微服务
4. **外部团队直接在 AWS 上部署还是你来部署？** — 建议你统一部署，外部团队只管代码

请 review 后告诉我哪些决策需要调整，我再出执行方案。
