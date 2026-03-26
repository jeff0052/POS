# Release Audit Re-Review

日期：2026-03-27  
Reviewer：Codex  
分支：`codex/reservations-transfer-backend`  
审计范围：自上次复核基线 `87390f2` 之后到当前 `HEAD (89ff480)` 的代码，以及当前仓库 HEAD 的可发版状态

## 总结结论

当前结论：`BLOCK RELEASE`

这次 Claude 的新增工作，核心是把 `pos-payment-service` 这个独立支付微服务拉起来，并把 POS backend 接上了内部 callback 闭环。从工程推进角度看，这一步是有价值的；但从发版审计角度看，这次改动新增了至少 2 个新的高危面，并且上次复核中留下的若干阻断项仍未关闭。

我对当前 HEAD 的判断是：

1. 项目相比 `57-release-rereview-2026-03-27.md` 更接近“可联调”
2. 但距离“可上线”反而又多出了一层新的支付面风险
3. 支付微服务现在还不具备 production release 的安全边界和一致性保障

结论不变：当前分支不得上线。

---

## 审计方法

本轮我重点检查了以下范围：

1. `git log 87390f2..HEAD`
2. `8cceead` 与 `89ff480` 的新增支付微服务代码
3. `pos-backend` 内部 payment callback 接口与安全配置
4. `docker-compose.prod.yml`、`nginx/prod.conf`、数据库初始化与生产入口
5. 上一轮审计中仍可能残留的 release blocker

本轮实际验证：

1. `git status --short`：工作树干净
2. `pc-admin` 生产构建：通过
3. 后端 Maven 编译/测试：当前环境仍无法执行，因为机器没有 `mvn`，仓库里也仍然没有 `mvnw`
4. `pos-payment-service` 没有测试目录，CI 也没有覆盖它

---

## 本轮新增的 Critical Findings

### C1. `pos-payment-service` 是一个公开暴露、无鉴权、可直接驱动结算的外部入口

严重级别：Critical  
发版影响：硬阻断

证据：

- `pos-payment-service/pom.xml:23-65` 只有 `spring-boot-starter-web` / `data-jpa` / `validation` / `actuator`，没有任何 Spring Security 依赖
- `pos-payment-service/src/main/java/com/developer/pos/payment/api/PaymentController.java:24-49` 直接暴露：
  - `POST /api/v1/payments/intents`
  - `GET /api/v1/payments/intents/{intentId}`
  - `POST /api/v1/payments/intents/{intentId}/cancel`
- `nginx/prod.conf:28-36` 把 `/payment-api/` 公开反代到 `pos-payment-service:8081`
- `pos-payment-service/src/main/java/com/developer/pos/payment/adapter/CashAdapter.java:16-25` 对 `CASH` 直接返回同步成功
- `pos-payment-service/src/main/java/com/developer/pos/payment/orchestrator/PaymentOrchestrator.java:65-84` 对同步成功支付会立刻调用 callback 通知 POS
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/internal/PaymentCallbackController.java:36-80` 收到成功 callback 后会直接触发 `collectForTable(...)` 关单结算

为什么这是硬阻断：

当前链路等价于：

1. 公网调用 `/payment-api/v1/payments/intents`
2. 指定任意 `storeId` / `tableId` / `amountCents`
3. `paymentMethod = CASH`
4. 微服务直接判定支付成功
5. 微服务以内部身份回调 POS backend
6. POS backend 依据回调内容触发表结算

也就是说，`pos-payment-service` 现在本质上是一个“公开的结算触发器”。只要知道门店和桌号，并给一个足够大的 `amountCents`，就可以绕过正常 cashier / payment 流程去关台。

这不是“权限没收紧”这么简单，而是把结算能力直接暴露到了公网入口。

必须修复：

1. `pos-payment-service` 必须加真实鉴权，不允许匿名访问 create/get/cancel 接口
2. 不允许前端或任意客户端直接构造支付 intent 的业务上下文
3. payment intent 必须由 POS backend 或受信服务端创建，并绑定已存在的订单/结算上下文
4. callback 不能仅凭 `storeId/tableId/amount` 就触发结算，必须校验 intent 与后端已登记支付记录的一致性

### C2. 支付成功与 POS 结算成功之间没有一致性保障，失败会被“伪成功”吞掉

严重级别：Critical  
发版影响：硬阻断

证据：

- `pos-payment-service/src/main/java/com/developer/pos/payment/orchestrator/PaymentOrchestrator.java:67-84`
  - 先把 intent 置为 `SUCCEEDED`
  - 然后才调用 `callbackNotifier.notifyPaymentResult(intent)`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/internal/PaymentCallbackController.java:74-95`
  - settlement 抛异常时不会返回 4xx/5xx
  - 仍然返回 `ApiResponse.success(...)`
  - 只是把 `settlementTriggered=false` 和 `error` 放进 body
- `pos-payment-service/src/main/java/com/developer/pos/payment/orchestrator/CallbackNotifier.java:72-80`
  - 只要 HTTP 状态码是 `2xx` 就记为 callback 成功
  - 不检查 body 里的 `settlementTriggered`
  - 失败时只打日志，没有重试、没有 outbox、没有 dead-letter、没有状态补偿

为什么这是硬阻断：

这意味着以下场景一定会发生数据分叉：

1. payment service 认为支付已经成功
2. callback 到了 POS backend
3. POS backend 因金额校验、桌台状态、重复结算、数据库异常等原因没有完成结算
4. 但 callback 仍然返回 HTTP 200
5. payment service 记录“回调成功”，不会再补偿

最终结果就是：

- 支付数据库：`SUCCEEDED`
- POS 订单 / 桌台 / settlement：可能仍然未结算

对支付域来说，这已经不是日志问题，而是账务一致性问题。

必须修复：

1. callback controller 在结算失败时必须返回非 2xx
2. payment service 不能把“HTTP 到达”当成“业务成功”
3. 对 callback 失败必须有 retry / outbox / 补偿策略
4. 只有在 POS backend 明确确认 settlement 成功后，才可认为支付闭环成功

---

## 当前仍未关闭的 High Findings

### H1. `platform-admin` 仍然没有接上后端要求的鉴权模型

严重级别：High  
发版影响：阻断

证据：

- `pos-backend/src/main/java/com/developer/pos/auth/security/SecurityConfig.java:47-50` 要求 `/api/v2/platform/**` 必须是 `PLATFORM_ADMIN`
- `platform-admin/src/api/client.ts:9-16` 没有附带 `Authorization`
- `platform-admin/src/router/AppRouter.tsx:11-25` 只有页面路由，没有登录、鉴权守卫、session bootstrap
- 我对 `platform-admin/src` 做全文检索，没有看到 login/token/auth 相关实现

影响：

后端已经锁起来了，但平台前端没有跟上。也就是说，平台端当前在真实环境下仍然不可用。

必须修复：

1. 补 platform admin 登录页
2. 补 token 存储与 Bearer 注入
3. 补 `/auth/me` bootstrap 和路由守卫

### H2. `auth_users` migration 仍然在种固定默认密码 `admin123`

严重级别：High  
发版影响：阻断

证据：

- `pos-backend/src/main/resources/db/migration/v2/V060__auth_users.sql:16-29`

当前 seeded 账号：

1. `admin / admin123`
2. `store_admin / admin123`
3. `cashier / admin123`

影响：

这会把已知凭据直接带进 release artifact。任何接触代码、文档、数据库初始化逻辑的人都能知道初始密码。

必须修复：

1. 不要在 release migration 里写死默认密码
2. 如果确实需要 bootstrap 用户，应改成环境变量注入或一次性初始化脚本
3. 必须要求首次改密或默认禁用

### H3. 旧的 cashier collect 接口仍然可以绕过新金额校验

严重级别：High  
发版影响：阻断

证据：

- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java:193-209`
  - `collectForTable(...)` 已加入金额校验
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java:267-277`
  - `collect(...)` 仍然直接写入 `command.collectedAmountCents()`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/CashierSettlementV2Controller.java:33-48`
  - 旧接口仍然公开暴露

影响：

只要旧入口还在，对金额完整性的修复就没有真正完成。

必须修复：

1. 删除旧入口，或强制复用同一套校验逻辑
2. 补测试覆盖两个入口

### H4. `pos-payment-service` 仍然没有进入 CI，也没有任何测试

严重级别：High  
发版影响：阻断

证据：

- `.github/workflows/ci.yml:37-44` 只跑 `pos-backend` 的 Maven verify
- `.github/workflows/ci.yml:46-86` 只跑三个前端 build
- CI 没有任何 `pos-payment-service` job
- `pos-payment-service/src/test` 目录不存在

影响：

这次新增的是独立服务和独立数据库，但当前 CI 对它是完全失明的。换句话说，payment service 现在既没有单测，也没有集成测试，也没有构建校验。

必须修复：

1. 至少给 `pos-payment-service` 加构建 job
2. 至少补 create-intent / callback / cancel 的基础用例
3. 最好加 callback 成功/失败契约测试

---

## Medium Findings

### M1. `POS_CALLBACK_SECRET` 仍然是“可选安全项”，没有 fail-fast

严重级别：Medium  
发版影响：建议在下次复核前修复

证据：

- `pos-backend/src/main/resources/application.yml:22-23` 默认 `POS_CALLBACK_SECRET` 为空
- `pos-payment-service/src/main/resources/application.yml:31-33` 默认 `POS_CALLBACK_SECRET` 为空
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/internal/PaymentCallbackController.java:41-52`
  - 只有在 secret 非空时才验签
- `pos-payment-service/src/main/java/com/developer/pos/payment/orchestrator/CallbackNotifier.java:67-70`
  - 只有在 secret 非空时才签名

影响：

虽然 `docker-compose.prod.yml` 已经把 `POS_CALLBACK_SECRET` 接进去了，但应用本身仍然把它当成“可选项”。只要部署漏配一次，这个内部 callback 入口就会退化成一个匿名可写接口。

建议修复：

1. 在 production profile 下强制要求 `POS_CALLBACK_SECRET`
2. secret 缺失时直接启动失败

### M2. 生产部署文档还没跟上 payment service 的新增依赖

严重级别：Medium  
发版影响：建议在下次复核前修复

证据：

- `docker-compose.prod.yml` 已经引入：
  - `pos-payment-service`
  - `PAYMENT_DB_URL`
  - `POS_CALLBACK_SECRET`
  - `JWT_SECRET`
- 但我在以下文件里没有搜到对应的运维说明：
  - `DEPLOY_AWS.md`
  - `RUN.md`
  - `.env.example`

影响：

部署配置和部署文档再次脱节，容易出现“代码以为上线条件已经具备，但运维流程根本没跟上”的问题。

建议修复：

1. 补 `.env.example`
2. 补 payment service 的部署步骤
3. 补 payment DB 初始化和 secret 配置说明

### M3. 安全失败的 HTTP 语义仍然不对

严重级别：Medium  
发版影响：建议修复

证据：

- `pos-backend/src/main/java/com/developer/pos/common/config/GlobalExceptionHandler.java:31-35`
  - `IllegalStateException` 仍映射为 `409`
- `pos-backend/src/main/java/com/developer/pos/common/config/GlobalExceptionHandler.java:89-94`
  - 仍没有 `SecurityException` 的明确映射
- `pos-backend/src/main/java/com/developer/pos/auth/controller/AuthController.java:37-38`
  - `/auth/me` 未登录时仍抛 `IllegalStateException("Not authenticated")`

影响：

未登录、鉴权失败、签名失败这些情况在 API 层仍然不能准确表达成 `401/403`，会继续给前端、日志和监控制造噪音。

---

## 已确认做对了的地方

这次也有几项值得明确肯定，否则审计会失真：

1. `pc-admin` 的 token 存储和 Bearer 注入已经补上
2. backend 的 JWT 鉴权边界已经建立
3. VibeCash webhook 的空 secret 直通问题已经修掉
4. POS 改单已经改成服务端重新查 SKU 价格
5. 退款流程已经加了悲观锁
6. `docker-compose.prod.yml` 里 `JWT_SECRET` / `POS_CALLBACK_SECRET` 已接入 `pos-backend`

这些修复说明 Claude 的方向并不是乱改，而是已经开始具备系统性修补能力。问题在于，这次 payment service 新增的风险级别太高，直接抵消了这轮前进带来的发版收益。

---

## Claude 下一轮必须执行的清单

我建议 Claude 下一轮严格按以下顺序处理：

1. 给 `pos-payment-service` 加服务端鉴权，禁止匿名访问 create/get/cancel
2. 禁止客户端直接构造支付业务上下文，payment intent 必须由受信后端创建
3. 重构 payment callback 契约：结算失败必须返回非 2xx，payment service 必须识别业务失败
4. 给 callback 增加 retry/outbox/补偿机制
5. 删除或合并旧的 `cashier-settlement/{activeOrderId}/collect` 入口
6. 给 `platform-admin` 补全登录与 token 流程
7. 移除 `V060__auth_users.sql` 中的固定默认密码
8. 把 `pos-payment-service` 纳入 CI，并补最基本的测试
9. 更新 `.env.example`、`RUN.md`、`DEPLOY_AWS.md`

---

## 本轮 Reviewer 最终判断

当前 HEAD 比上一轮多了“支付微服务雏形”，但也新增了“支付面直接暴露到公网”的问题。

如果只看工程进度，这次是推进。  
如果按上线审计标准，这次仍然不能通过。

最终结论：`BLOCK RELEASE`

在下列条件全部满足前，我不会给 release approval：

1. payment service 不再对匿名调用开放
2. payment success 与 settlement success 的一致性问题被补上
3. 旧 collect 入口的金额绕过被关掉
4. platform admin 完成鉴权闭环
5. 默认密码 seeded 账号被移除
6. payment service 进入 CI，并至少有基础测试
