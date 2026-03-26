# Payment Microservice Execution Plan

Date: 2026-03-27
Decisions: 子模块(非新仓库) / REST回调 / Android→Payment微服务 / 统一部署

---

## 目录结构

```
/Users/ontanetwork/Documents/Codex/
├── pos-backend/                 ← 现有 POS 后端（不动大结构）
├── pos-payment-service/         ← 新增：支付微服务子模块
│   ├── pom.xml
│   ├── Dockerfile
│   ├── src/main/java/com/developer/pos/payment/
│   │   ├── PaymentServiceApplication.java
│   │   ├── config/
│   │   │   ├── SecurityConfig.java          ← 服务间 HMAC 验证
│   │   │   └── ProviderConfig.java          ← Provider 凭证配置
│   │   ├── core/
│   │   │   ├── PaymentIntent.java           ← 核心实体
│   │   │   ├── PaymentStatus.java           ← 状态枚举
│   │   │   └── PaymentProviderAdapter.java  ← Provider 适配器接口
│   │   ├── adapter/
│   │   │   ├── CashAdapter.java             ← 现金（直接成功）
│   │   │   ├── VibeCashAdapter.java         ← QR 网关
│   │   │   └── DcsCardAdapter.java          ← 接收 Android POST
│   │   ├── orchestrator/
│   │   │   ├── PaymentOrchestrator.java     ← 路由 + 状态机
│   │   │   └── CallbackNotifier.java        ← 回调 POS 后端
│   │   ├── webhook/
│   │   │   ├── VibeCashWebhookHandler.java  ← 从 POS 后端搬过来
│   │   │   └── DcsResultHandler.java        ← 接收 Android SDK 结果
│   │   ├── refund/
│   │   │   └── RefundService.java           ← Provider 退款调用
│   │   ├── persistence/
│   │   │   ├── entity/
│   │   │   └── repository/
│   │   └── api/
│   │       ├── PaymentController.java       ← 对外 API
│   │       ├── WebhookController.java       ← Provider 回调入口
│   │       └── InternalController.java      ← Android SDK 结果入口
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/
│           └── V001__payment_service_init.sql
├── docker-compose.yml           ← 加 payment-service 容器
└── docker-compose.prod.yml      ← 加 payment-service 容器
```

---

## 执行步骤

### Phase 1：搭建骨架 + Cash Adapter（验证链路）

**Task 1.1 — 创建子模块项目**
- Spring Boot 3.3 + Java 17
- pom.xml（独立于 pos-backend，不共享 parent）
- application.yml（独立端口 8081，独立数据库 `pos_payment_db`）
- Dockerfile
- 预计改动：5 个新文件

**Task 1.2 — 核心模型**
- `PaymentIntent` entity：intentId, merchantId, storeId, tableId, sessionRef, amountCents, currency, paymentMethod, paymentScheme, status, providerCode, providerTransactionId, callbackUrl, metadata, createdAt, updatedAt, completedAt
- `PaymentStatus` enum：CREATED → PENDING → SUCCEEDED / FAILED / EXPIRED / CANCELLED
- `PaymentTransaction` entity：transactionId, intentId, providerCode, requestPayload, responsePayload, status, createdAt
- `WebhookEvent` entity：eventId, providerCode, rawPayload, processedAt
- Flyway V001 migration
- 预计改动：8 个新文件

**Task 1.3 — Provider Adapter 接口**
```java
public interface PaymentProviderAdapter {
    String providerCode();
    CreatePaymentResult createPayment(PaymentIntent intent);
    QueryPaymentResult queryPayment(String providerTransactionId);
    RefundResult refund(String providerTransactionId, long amountCents, String reason);
    boolean supports(String paymentMethod, String paymentScheme);
}
```
- CashAdapter 实现：无外部调用，直接返回 SUCCEEDED
- 预计改动：3 个新文件

**Task 1.4 — PaymentOrchestrator**
- `createIntent(request)` → 选 adapter → 调 `createPayment` → 保存状态
- `handleProviderResult(intentId, result)` → 更新状态 → 如果 SUCCEEDED 则回调 POS
- `CallbackNotifier` → POST 到 POS 后端的 `/api/v2/internal/payment-callback`，HMAC 签名
- 预计改动：2 个新文件

**Task 1.5 — REST API**
```
POST   /api/v1/payments/intents          ← POS 后端调用，创建支付意图
GET    /api/v1/payments/intents/{id}     ← 查询状态
POST   /api/v1/payments/intents/{id}/cancel  ← 取消
```
- 预计改动：1 个新文件

**Task 1.6 — POS 后端改动**
- 新增 `PaymentCallbackController`：`POST /api/v2/internal/payment-callback`
- HMAC 签名验证
- 收到 SUCCEEDED 回调后调现有 `collectForTable`
- 预计改动：2 个新文件

**Task 1.7 — Docker Compose**
- docker-compose.yml 加 `pos-payment-service` 容器（端口 8081）
- docker-compose.prod.yml 同步
- nginx 加 `/payment-api/` 反代到 8081
- 预计改动：3 个文件修改

**Task 1.8 — 端到端测试（Cash）**
- POS 前端选 CASH → 调 Payment 微服务创建 intent → Cash adapter 直接成功 → 回调 POS → 结账
- 验证全链路跑通

### Phase 2：迁移 VibeCash

**Task 2.1 — VibeCashAdapter**
- 从 `VibeCashPaymentApplicationService` 搬 HTTP 调用逻辑到 adapter
- 凭证（api-url, secret）配在 payment-service 的 application.yml 里
- 预计改动：1 个新文件

**Task 2.2 — Webhook 搬家**
- `VibeCashWebhookHandler` 搬到 payment-service
- webhook-secret 配在 payment-service
- VibeCash 回调地址改为指向 payment-service
- 预计改动：2 个新文件 + 1 个修改

**Task 2.3 — POS 后端清理**
- 删除 `VibeCashPaymentApplicationService`
- 删除 `VibeCashWebhookV2Controller`
- POS 前端改 QR 支付调用路径：从 `/api/v2/.../payment/vibecash` 改为 `/payment-api/v1/payments/intents`
- 预计改动：2 个删除 + 3 个修改

**Task 2.4 — 端到端测试（VibeCash）**
- QR 支付全流程：创建 intent → VibeCash 创建 link → 返回 QR → 模拟 webhook → 回调 POS → 结账

### Phase 3：接入 DCS 刷卡

**Task 3.1 — DcsCardAdapter**
- 接收 Android 端 POST 的 SDK 调用结果
- `POST /api/v1/payments/dcs/result` — Android 调完 DCS SDK 后把结果推给 Payment 微服务
- Payment 微服务记录交易 → 回调 POS → 结账
- 预计改动：2 个新文件

**Task 3.2 — Android 端改动**
- DcsPaymentService.kt 调完 SDK 后，POST 结果到 Payment 微服务
- 不再直接调 POS 后端
- 预计改动：1 个文件修改

**Task 3.3 — 端到端测试（DCS）**
- 需要 Sunmi 设备实测

### Phase 4：退款 + 对账

**Task 4.1 — Provider 退款**
- VibeCashAdapter.refund() → 调 VibeCash 退款 API
- DcsCardAdapter.refund() → 推给 Android 端调 DCS SDK 退款
- CashAdapter.refund() → 直接标记成功
- 预计改动：3 个文件修改

**Task 4.2 — POS 后端退款改造**
- `RefundApplicationService.createRefund()` 改为先调 Payment 微服务退款 → 成功后再更新本地记录
- 预计改动：1 个文件修改

**Task 4.3 — 对账**
- 每日定时任务：拉 Provider 流水 vs 本地 PaymentIntent 记录
- 标记异常（金额不符、状态不一致、缺失记录）
- 预计改动：3 个新文件

---

## 文件改动汇总

| Phase | 新增文件 | 修改文件 | 删除文件 | 总改动 |
|-------|---------|---------|---------|-------|
| Phase 1 | ~22 | ~5 | 0 | ~27 |
| Phase 2 | ~3 | ~4 | ~2 | ~9 |
| Phase 3 | ~2 | ~1 | 0 | ~3 |
| Phase 4 | ~3 | ~2 | 0 | ~5 |
| **总计** | **~30** | **~12** | **~2** | **~44** |

---

## 时间估算

| Phase | 工作量 | 依赖 |
|-------|--------|------|
| Phase 1 | 1 天 | 无 |
| Phase 2 | 0.5 天 | Phase 1 + VibeCash 凭证 |
| Phase 3 | 0.5 天 | Phase 1 + Sunmi 设备 + DCS SDK 修复 |
| Phase 4 | 1 天 | Phase 2 + Phase 3 |

---

## 外部团队交接物

当 Phase 1 骨架搭好后，交给外部团队的是：

1. `pos-payment-service/` 子模块代码
2. `PaymentProviderAdapter` 接口定义
3. CashAdapter 作为参考实现
4. Docker Compose 联调环境
5. 回调协议文档（HMAC 签名格式）
6. 测试用例模板

外部团队的工作就是：**实现 VibeCashAdapter 和 DcsCardAdapter**，不需要碰 POS 后端。

---

## 风险

| 风险 | 缓解 |
|------|------|
| VibeCash API 变更 | Adapter 隔离，只改 adapter 代码 |
| DCS SDK 版本不匹配 | Android 端处理 SDK 差异，Payment 微服务只接收结构化结果 |
| 服务间通信失败 | 回调重试 + 幂等设计 |
| 数据库不一致 | PaymentIntent 和 SettlementRecord 通过 intentId 关联，最终一致 |
| 外部团队不熟悉代码 | Phase 1 骨架 + CashAdapter 示范 + 完整文档 |

---

## 确认后执行顺序

你说"搞"我就从 Phase 1 Task 1.1 开始，一步一步推进。每完成一个 Task 提交一次 commit。
