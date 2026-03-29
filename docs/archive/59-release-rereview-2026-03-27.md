# Release Re-Review After Doc 58 Fix Pass

日期：2026-03-27  
Reviewer：Codex  
分支：`codex/reservations-transfer-backend`  
复核范围：`89ff480..fb83c9c`

## 结论

当前结论仍然是：`BLOCK RELEASE`

Claude 这轮修复里，确实有几项是有效收口的：

1. `platform-admin` 登录页、token 存储、Bearer 注入、路由守卫已经补上
2. 旧的 `/cashier-settlement/{id}/collect` 入口已经删除
3. CI 已新增 `platform-admin` 和 `pos-payment-service`
4. `.env.example` 已补齐核心 secrets

但“9 项全部修完”这个判断我不能通过。当前至少还有 5 个需要继续修改的问题，其中 3 个仍然是 release blocker，另外 1 个是新引入的工程验证问题。

---

## 验收总表

### 通过

1. `H1 Platform Admin 无 auth`：通过
2. `H3 旧 collect 绕过金额校验`：通过
3. `H4 Payment Service 不在 CI`：通过
4. `M2 .env.example 缺`：通过

### 部分通过

1. `C1 Payment Service 无鉴权`：只做到“加了门”，还没做到“门没锁时必须启动失败”
2. `C2 结算失败被吞`：只做到“会重试”，还没做到“支付状态与结算状态真正一致”
3. `M3 SecurityException 返回 409`：只修了一半，`/auth/me` 未登录仍然返回 `409`

### 未通过 / 变成新风险

1. `H2 写死 admin123 密码`：虽然默认密码删掉了，但被替换成了一个公开可抢占的 bootstrap 管理员入口
2. 新问题：两个新增 `mvnw` 文件都是坏的，占位内容是 `404: Not Found`

---

## Findings

### P1. Payment Service 仍然是 fail-open，不是 fail-fast

严重级别：High  
发版影响：阻断

证据：

- `pos-payment-service/src/main/java/com/developer/pos/payment/config/ProviderConfig.java:20-29`
  - `POS_CALLBACK_SECRET` 缺失时只 `log.error`
  - `PAYMENT_SERVICE_API_KEY` 缺失时只 `log.warn`
  - 没有抛异常，没有阻止启动
- `pos-payment-service/src/main/java/com/developer/pos/payment/config/SecurityConfig.java:75-82`
  - 当 `expectedKey` 为空时，代码直接把请求认证成 `SERVICE`
  - 这不是 fail-safe，而是显式 fail-open

为什么这条仍然阻断：

用户声称 `ProviderConfig 启动时检查` 已经修完 `C1/M1`，但当前实现只是打日志，不是强制约束。  
也就是说，只要部署漏配一次 `PAYMENT_SERVICE_API_KEY` 或 `POS_CALLBACK_SECRET`，服务仍然会带着降级安全边界启动。

这在 release 审计里不能算修复完成。

建议修复：

1. 对 `PAYMENT_SERVICE_API_KEY` 和 `POS_CALLBACK_SECRET` 直接 fail-fast
2. `SecurityConfig` 不允许在 key 为空时自动注入 `ROLE_SERVICE`
3. 如果确实需要 dev fail-open，必须明确绑定到 dev profile，不能靠通用默认值

### P1. 删除默认密码后，新引入了一个公开可抢占的 bootstrap 管理员入口

严重级别：High  
发版影响：阻断

证据：

- `pos-backend/src/main/java/com/developer/pos/auth/security/SecurityConfig.java:35-37`
  - `/api/v1/auth/bootstrap` 和 `/api/v2/auth/bootstrap` 是 `permitAll`
- `pos-backend/src/main/java/com/developer/pos/auth/controller/AuthController.java:38-47`
  - 任何未认证调用者都可以 POST 用户名和密码
- `pos-backend/src/main/java/com/developer/pos/auth/service/AuthService.java:53-64`
  - 只检查 `userRepository.count() == 0`
  - 然后直接创建 `PLATFORM_ADMIN`

为什么这条仍然阻断：

这相当于把“默认 admin123”换成了“谁先打到 bootstrap 接口，谁拿第一个平台管理员”。  
在 fresh deployment 场景下，如果应用已经对外暴露而运维还没初始化，首个外部访问者就能抢占管理员身份。

而且这里还有并发窗口：

1. 没有事务边界
2. 没有锁
3. 只做 `count()` 再 `save()`

两个并发 bootstrap 请求完全可能创建多个 `PLATFORM_ADMIN`。

建议修复：

1. bootstrap 不能公开匿名暴露
2. 至少需要一次性安装令牌、环境变量 gate、IP allowlist 或离线初始化脚本
3. bootstrap 创建必须做并发安全控制

### P1. Payment callback 重试加上了，但支付成功与结算成功仍然没有真正绑定

严重级别：High  
发版影响：阻断

证据：

- `pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/internal/PaymentCallbackController.java:83-92`
  - settlement 失败时现在会返回 `500`
  - 这一步是对的
- `pos-payment-service/src/main/java/com/developer/pos/payment/orchestrator/CallbackNotifier.java:107-108`
  - 3 次重试耗尽后只返回 `false`
- `pos-payment-service/src/main/java/com/developer/pos/payment/orchestrator/PaymentOrchestrator.java:83-88`
  - createIntent 路径下 callback 失败后只 `log.warn`
  - intent 依然保持 `SUCCEEDED`
- `pos-payment-service/src/main/java/com/developer/pos/payment/orchestrator/PaymentOrchestrator.java:114-117`
  - provider success 路径同样如此

为什么这条仍然阻断：

这说明当前系统仍然接受下面这种最终状态：

1. `payment_intents.status = SUCCEEDED`
2. POS 侧 settlement 并没有成功
3. 系统只留下 warn 日志，等待人工对账

这比之前“完全吞掉失败”确实进步了，但还没有达到 doc 58 要求的 release bar。  
你现在拥有的是“更可见的不一致”，不是“已消除的不一致”。

建议修复：

1. 至少新增一个显式状态，例如 `SETTLEMENT_PENDING` / `SETTLEMENT_FAILED`
2. 不要把 callback 失败后的 intent 留在 `SUCCEEDED`
3. 补偿策略不能只靠人工日志

### P2. `SecurityException -> 403` 修了，但 `/auth/me` 未登录仍然返回 `409`

严重级别：Medium  
发版影响：建议修复后再放行

证据：

- `pos-backend/src/main/java/com/developer/pos/common/config/GlobalExceptionHandler.java:73-84`
  - `SecurityException` 和 `AccessDeniedException` 已映射到 `403`
- `pos-backend/src/main/java/com/developer/pos/auth/controller/AuthController.java:53-55`
  - `/auth/me` 未登录时仍抛 `IllegalStateException("Not authenticated")`
- `pos-backend/src/main/java/com/developer/pos/common/config/GlobalExceptionHandler.java:31-35`
  - `IllegalStateException` 仍映射到 `409`

为什么这条没有完全通过：

用户汇报的是“SecurityException 返回 409 已修”，这件事本身是对的。  
但审计项本质上是“安全失败的 HTTP 语义要正确”。当前 `/auth/me` 仍然把未登录表示成 `409 Conflict`，所以这个问题只能算部分修复。

建议修复：

1. `/auth/me` 未登录直接返回 `401`
2. 或引入专门的 `UnauthenticatedException`

### P2. 两个新加的 Maven wrapper 文件都是坏的，当前仓库仍然无法用 wrapper 做本地验证

严重级别：Medium  
发版影响：建议修复

证据：

- `pos-backend/mvnw` 文件内容只有一行：`404: Not Found`
- `pos-payment-service/mvnw` 文件内容只有一行：`404: Not Found`

我本轮实际尝试：

1. 运行 `./mvnw -q verify` in `pos-backend` -> 失败，`404:: command not found`
2. 运行 `./mvnw -q verify` in `pos-payment-service` -> 失败，`404:: command not found`
3. 本机也没有系统 `mvn`

为什么这条重要：

这意味着当前仓库虽然“看起来加了 wrapper”，但实际上仍然没有可用的本地 Java 构建入口。  
对 release 审计来说，这会直接降低可复验性。

建议修复：

1. 提交真正的 Maven wrapper 脚本
2. 至少保证 reviewer 可以在同一台机器上直接运行 `./mvnw verify`

---

## 已确认修好的点

这些我这轮确认是成立的：

1. `platform-admin` 登录页、Bearer 注入、401/403 跳登录已经落地
2. 旧 `cashier-settlement/{activeOrderId}/collect` 已删除
3. `payment-service` CI job 已新增
4. `.env.example` 已经包含 `JWT_SECRET`、`POS_CALLBACK_SECRET`、`PAYMENT_SERVICE_API_KEY`
5. `nginx/prod.conf` 已经阻断公网访问大多数 `/payment-api/` 路由

另外，本轮我跑过：

1. `platform-admin` build：通过
2. `pc-admin` build：通过

---

## Reviewer 最终判断

这轮不能算“9 项全部修完”。更准确的说法是：

- 4 项通过
- 3 项部分通过
- 1 项替换成了新的高风险方案
- 1 项引入了新的工程验证问题

所以我的结论还是：`BLOCK RELEASE`

如果 Claude 要继续改，我建议下一轮优先顺序是：

1. 把 payment service 的 secret / API key 改成真正 fail-fast
2. 把公开 bootstrap 改成受控初始化方案
3. 给 payment intent 增加 settlement 结果状态，而不是 callback 失败后仍记为 `SUCCEEDED`
4. 修 `/auth/me` 的 401 语义
5. 提交真正可执行的 Maven wrapper
