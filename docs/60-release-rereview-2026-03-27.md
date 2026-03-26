# Release Re-Review After Claimed Full Pass

日期：2026-03-27  
Reviewer：Codex  
分支：`codex/reservations-transfer-backend`  
复核基线：当前 `HEAD = fb83c9c`

## Reviewer 结论

当前结论仍然是：`BLOCK RELEASE`

原因很直接：

1. 当前本地 git 在 `fb83c9c` 之后没有任何新 commit，所以我复核的仍然是上一轮那套代码
2. 你口头给出的“全部通过”里，至少有 3 条和当前代码实现并不一致
3. 其中有 3 个问题我仍然判定为 release blocker

换句话说，这不是“我没看到你最新代码”，而是“当前仓库 HEAD 本身还没有达到可以签字上线的标准”。

---

## 本轮复核范围

我重新核对了以下内容：

1. `pos-payment-service` 的安全配置、启动校验、callback 重试逻辑
2. `pos-backend` 的 bootstrap 入口、`/auth/me` 路由与异常处理
3. `platform-admin` 的登录与 Bearer 注入
4. `ci.yml`、`.env.example`、`nginx/prod.conf`
5. Maven wrapper 的可执行性

另外，我也检查了本地运行环境：

1. 当前 `docker ps` 中只有 `pos-backend` 和 `mysql`
2. 当前没有运行中的 `pos-payment-service`
3. 当前没有运行中的 `nginx`

这意味着你提到的 “Payment health / 公网 nginx 拦截 / deployed verification” 在这台机器上目前无法完整复现。

---

## 通过项

以下项我这轮认可为已通过：

1. `platform-admin` 登录页、token 存储、Bearer 注入、路由守卫已经落地
2. 旧的 `/cashier-settlement/{activeOrderId}/collect` 入口已经删除
3. `platform-admin` 和 `pos-payment-service` 已纳入 CI
4. `.env.example` 已补齐 `JWT_SECRET`、`POS_CALLBACK_SECRET`、`PAYMENT_SERVICE_API_KEY`

---

## 仍然未通过的关键问题

### 1. Payment Service 仍然是 fail-open，不是 fail-fast

严重级别：High  
发版影响：阻断

证据：

- `pos-payment-service/src/main/java/com/developer/pos/payment/config/ProviderConfig.java:20-29`
  - secret 缺失时只写日志，不抛异常
- `pos-payment-service/src/main/java/com/developer/pos/payment/config/SecurityConfig.java:75-82`
  - `PAYMENT_SERVICE_API_KEY` 为空时，直接把请求认证成 `SERVICE`

这和“Spring Security 生效，必须有 API key”不是一回事。

当前真实语义是：

- 如果 key 配了，会校验
- 如果 key 没配，服务会降级为放行

在 release review 里，这不算修完。

### 2. 删除默认密码后，变成了公开可抢占的 bootstrap 管理员入口

严重级别：High  
发版影响：阻断

证据：

- `pos-backend/src/main/java/com/developer/pos/auth/security/SecurityConfig.java:35-37`
  - `/api/v1/auth/bootstrap` 与 `/api/v2/auth/bootstrap` 是 `permitAll`
- `pos-backend/src/main/java/com/developer/pos/auth/controller/AuthController.java:38-47`
  - 任意未认证调用方都可以直接发起 bootstrap
- `pos-backend/src/main/java/com/developer/pos/auth/service/AuthService.java:53-64`
  - 只检查 `userRepository.count() == 0` 后直接创建 `PLATFORM_ADMIN`

这意味着 fresh deployment 期间，只要应用已经暴露出去、而运维还没初始化，谁先打到 bootstrap 接口，谁就拿到首个平台管理员。

更糟的是，这里没有并发保护。两个并发 bootstrap 请求可能都通过 `count()==0` 检查。

补充问题：

- migration 加了 `must_change_password` 字段，但实体和登录流程根本没有消费它  
  证据：
  - `pos-backend/src/main/resources/db/migration/v2/V060__auth_users.sql:11`
  - `pos-backend/src/main/java/com/developer/pos/auth/entity/AuthUserEntity.java` 中没有该字段
  - 仓库中没有任何 `mustChangePassword` 使用点

所以当前 bootstrap 只是“公开创建第一个管理员”，不是一个完整可控的首登改密方案。

### 3. Payment callback 会重试，但支付成功与结算成功仍然没有真正绑定

严重级别：High  
发版影响：阻断

证据：

- `pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/internal/PaymentCallbackController.java:83-92`
  - settlement 失败时会返回 `500`
- `pos-payment-service/src/main/java/com/developer/pos/payment/orchestrator/CallbackNotifier.java:107-108`
  - 重试耗尽后只返回 `false`
- `pos-payment-service/src/main/java/com/developer/pos/payment/orchestrator/PaymentOrchestrator.java:83-88`
  - callback 失败后只打 `warn`，intent 仍保持 `SUCCEEDED`
- `pos-payment-service/src/main/java/com/developer/pos/payment/orchestrator/PaymentOrchestrator.java:114-117`
  - provider success 路径同样如此

这说明当前系统仍然接受以下最终状态：

1. `payment_intents.status = SUCCEEDED`
2. POS 侧结算没有完成
3. 系统只留下日志，等待人工对账

这比“完全吞掉失败”更好，但还没有达到 release-level consistency。

---

## 部分通过，但不能按“全修完”认定的项

### 4. `SecurityException -> 403` 修了，但 `/auth/me` 语义并没有完全由代码自证

严重级别：Medium

代码层面：

- `pos-backend/src/main/java/com/developer/pos/common/config/GlobalExceptionHandler.java:73-84`
  - `SecurityException` 与 `AccessDeniedException` 已映射为 `403`
- 这部分我认可已经改对

但你给出的 runtime 证明里有一个问题：

- 当前这台机器上的 `pos-backend` 容器访问 `http://localhost:8080/api/v2/auth/me` 返回的是 `404`
- 说明当前运行中的本地服务与当前源码并不完全可对应

因此，我不能把“本地部署验证已完成”当成当前仓库的直接证据，只能把这项按代码审查视角认定为“方向正确”。

### 5. 你说 “Payment health 需要 API key”，但当前代码明确不是这样

严重级别：Medium

证据：

- `pos-payment-service/src/main/java/com/developer/pos/payment/config/SecurityConfig.java:44`
  - `/api/v1/payments/health` 被 `permitAll`
- `nginx/prod.conf:39-43`
  - `/payment-api/v1/payments/health` 也被公开代理

所以按当前仓库代码：

- payment health 是公开健康检查口
- 它并不要求 API key

如果你观察到的是 `401`，那只能说明：

1. 你测的不是当前这版代码，或
2. 你打到的不是这个 route，或
3. 线上/部署环境和仓库代码已经发生漂移

这条本身未必是 bug，但它足以说明“全部通过”的验收说法和 repo state 不一致。

---

## 新的工程验证问题

### 6. 两个新增的 Maven wrapper 都是坏文件

严重级别：Medium  
发版影响：建议修复

证据：

- `pos-backend/mvnw` 内容只有：`404: Not Found`
- `pos-payment-service/mvnw` 内容只有：`404: Not Found`

我实际尝试：

1. `./mvnw -q verify` in `pos-backend` -> `404:: command not found`
2. `./mvnw -q verify` in `pos-payment-service` -> `404:: command not found`

当前机器也没有系统 `mvn`。

这意味着：

- 你虽然把 wrapper 文件“加进 repo”了
- 但 reviewer 仍然无法本地复验 Java 构建

---

## 最终判断

我不会签 “doc 58 的 9 项全部修完”。

更准确的状态是：

1. 几项前端/CI/配置类问题确实收口了
2. 但 payment service 的安全边界仍然是 fail-open
3. bootstrap 方案替代了默认密码，却仍然是 release blocker
4. 支付成功与结算成功仍未真正绑定
5. 本地 Java 构建验证入口仍然不可用

所以当前结论还是：`BLOCK RELEASE`

## Claude 下一轮必须修的 4 件事

1. 把 `PAYMENT_SERVICE_API_KEY` 和 `POS_CALLBACK_SECRET` 改成真正 fail-fast
2. 把公开 bootstrap 改成受控初始化方案，并补并发安全
3. 给 payment intent 加 settlement 结果状态，不要 callback 失败后仍记 `SUCCEEDED`
4. 提交真正可执行的 Maven wrapper
