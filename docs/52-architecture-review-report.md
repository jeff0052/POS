# POS Architecture Review Report

Date: 2026-03-26
Reviewer: Claude
Scope: Full stack — Backend, Android, Frontend (pc-admin, qr-ordering), Infrastructure

---

## Executive Summary

系统作为原型验证价值很高，核心点餐→结账链路已跑通。但有 **8 个 CRITICAL 级问题**必须在上线前解决，否则会导致资金损失或安全事故。

| 严重级别 | 数量 | 说明 |
|---------|------|------|
| CRITICAL | 8 | 安全漏洞、资金风险、数据竞争 |
| HIGH | 12 | 业务逻辑错误、架构耦合 |
| MEDIUM | 17 | 性能、一致性、可维护性 |
| LOW | 5 | 代码规范、命名 |

---

## CRITICAL Issues（上线前必须修）

### 1. 没有真实认证 [SEC-1]

`AuthService.java` 返回硬编码 mock JWT token。所有 API 无鉴权，任何人都能调用结账、支付、会员、商品管理接口。

**修复：** 至少实现 JWT 验证 + Spring Security filter chain。

### 2. Webhook 签名未校验 [SEC-3]

`VibeCashPaymentApplicationService.java:151` — `handleWebhook` 接收 signature 参数但从不验证。攻击者可以伪造 `payment.succeeded` webhook，触发未付款的结账。

**修复：** 用 `vibecash.webhook-secret` 验证 HMAC 签名。

### 3. QR 点单不校验价格 [SEC-5]

`QrOrderingV2Controller.java:48` — 公开接口接受客户端传来的 `unitPriceCents`，服务端不校验。顾客可以改价格为 0。

**修复：** 从 SKU Catalog 查询真实价格，忽略客户端传值。

### 4. CORS 全部打开 [SEC-2]

`CorsConfig.java` — `allowedOrigins("*")`。配合无认证，等于裸奔。

**修复：** 限制到具体域名。

### 5. 桌台并发竞争 [ORD-1]

POS 和 QR 同时操作同一桌，无悲观锁。两个线程同时读到空 → 同时创建 → 唯一约束异常。

**修复：** 加 `@Lock(PESSIMISTIC_WRITE)` 或 `SELECT ... FOR UPDATE`。

### 6. 双重结账风险 [PAY-1]

`collectForTable()` 无幂等校验。VibeCash webhook 重试可能触发重复结算。

**修复：** 结账前检查 session 状态是否已 SETTLED。

### 7. V1/V2 双实体映射同一张表 [V1V2-1]

V1 和 V2 的 `StoreEntity` 都映射 `stores` 表，字段集不同。V1 写入可能置空 V2 字段。

**修复：** 删除 V1 代码或将 V1 表重命名。

### 8. 零测试 [TEST-1]

`pos-backend/src/test/` 目录为空。所有业务逻辑无自动化验证。

**修复：** 至少补订单生命周期、结账流程、促销计算的集成测试。

---

## HIGH Issues（尽快修）

| # | 问题 | 位置 | 说明 |
|---|------|------|------|
| ORD-2 | 订单号碰撞 | ActiveTableOrderApplicationService:326 | `"ATO" + currentTimeMillis()` 并发下重复 |
| ORD-3 | QR 提交金额累计错误 | ActiveTableOrderApplicationService:191 | 每次 QR 提交记录的是累计总额而非增量 |
| ORD-4 | submitToKitchen 删除活动订单 | ActiveTableOrderApplicationService:292 | 提交后无法追加，打断追溯链 |
| PAY-4 | VibeCash 金额单位疑问 | VibeCashPaymentApplicationService:94 | 发送 cents 但字段名是 `amount`，可能被当做 dollars |
| CS-1 | God class 517 行 | ActiveTableOrderApplicationService | 应拆分为 3 个 service |
| CS-3 | Member 直接操作 Order 实体 | MemberApplicationService:22 | 跨域耦合 |
| CS-4 | Settlement 直接操作 Order/Store | CashierSettlementApplicationService | 跨域耦合 |
| CS-5 | Report 吞掉所有数据库异常 | ReportReadService:261 | `catch (DataAccessException ignored)` |
| V1V2-2 | V1 硬编码 storeId=1001 | OrderService:27 | 多店支持不可能 |
| DB-3 | settlement_records 无外键 | V008 migration | 引用完整性缺失 |
| SEC-4 | SQL 日志开启 + 敏感数据暴露 | application.yml:19 | 生产环境风险 |
| Android | VibeCash 支付状态存内存 | VibeCashPaymentService.kt | 进程重启丢失支付引用 |

---

## MEDIUM Issues

| 领域 | 问题 | 说明 |
|------|------|------|
| Order | 状态被重置为 DRAFT | replaceItems() 无条件设 DRAFT |
| Order | POS 直接结账无 submitted order | moveToSettlement 前未 submit |
| Order | MerchantOrderReadService 加载全部桌台 | findAll() 应改 findByStoreId() |
| Member | 手机号唯一约束不含 merchant_id | 多商户同号冲突 |
| Member | 会员折扣硬编码 | Gold=10%, Silver=5%, VIP=15% |
| Member | 加载全部会员到内存 | findAll() 两处 |
| Promotion | 内存过滤活动规则 | 应数据库 WHERE 过滤 |
| Settlement | active_order_id 存的是 session_id | 字段语义混乱 |
| Settlement | 无 created_at 映射 | 报表查不到结算时间 |
| Report | 无 @Transactional | 多次查询可能不一致 |
| DB | DATETIME vs TIMESTAMP 不统一 | 时区处理不同 |
| DB | Flyway 默认关闭 | 错误 profile 会跑裸库 |
| DB | 种子数据硬编码 ID | 不应放 Flyway migration |
| DB | settlement_records 无索引 | 报表全表扫描 |
| Frontend | QR 点单客户端计算折扣 | 应服务端计算 |
| Frontend | android-preview-web 2362行单文件 | 不可维护 |
| Android | 原生 Compose 和 WebView 并存 | 架构混乱 |

---

## Architecture Strengths（做得好的）

1. **V2 DDD 分层清晰** — interfaces/application/domain/infrastructure 四层，每个域独立
2. **统一桌台模型** — POS + QR 共享 active_table_order，设计方向正确
3. **钱用 cents (long)** — 避免浮点精度问题
4. **Flyway 管理 schema** — 版本化迁移，可追溯
5. **Command/Query 分离** — V2 service 层有 Command 和 Query 对象
6. **促销引擎可扩展** — PromotionRule + Condition + Reward 三表结构灵活

---

## Recommended Fix Order

### Week 1: 安全 + 资金（CRITICAL）
1. 实现 JWT 认证
2. VibeCash webhook 签名校验
3. QR 点单服务端价格校验
4. CORS 限制

### Week 2: 订单核心（CRITICAL + HIGH）
5. 桌台并发锁
6. 订单号生成改 UUID 或 snowflake
7. QR 提交金额修正（增量 vs 累计）
8. submitToKitchen 不删活动订单

### Week 3: 结算 + 多店
9. 结账幂等校验
10. VibeCash 金额单位确认
11. 删除/隔离 V1 代码
12. 去掉硬编码 storeId

### Week 4: 质量
13. 拆分 ActiveTableOrderApplicationService
14. 解耦跨域依赖
15. 补核心路径集成测试
16. Report 异常处理修正

---

## For Codex / Agent Programmer

如果你在看这份报告，优先级是：

1. **不要碰 Week 1 的安全修复** — 这些需要架构决策（JWT secret 管理、webhook secret 来源），等 Jeff 定方向
2. **Week 2 的订单修复可以做** — 都是纯代码修复，不需要外部决策
3. **Week 3-4 可以边做业务边修** — 不要一次性重构，跟着业务需求逐步收

**最安全的起手：** 订单号生成改 UUID（ORD-2），零风险，5 分钟搞定。
