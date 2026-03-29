# Release Re-Review After 4-Item Fix Pass

日期：2026-03-27  
Reviewer：Codex  
分支：`codex/reservations-transfer-backend`  
复核基线：`fb83c9c..b660934`

## 结论

当前结论仍然是：`BLOCK RELEASE`

你这轮列出来的 4 个点里，代码层面有 3 个已经修到位：

1. `Payment Service fail-open`：已改成 fail-fast
2. `Bootstrap 公开可抢占`：已加 secret gate，并补了并发保护
3. `支付成功但结算失败仍记 SUCCEEDED`：已引入 `SETTLEMENT_FAILED`
4. `Maven wrapper 是 404 文件`：wrapper 脚本本身已恢复正常

但我这轮又核出 2 个新的阻断项，以及 2 个运维/验证缺口。  
也就是说，当前不是“可以放行”，而是“原来的 4 个坑基本补上了，但 release artifact 仍然不完整”。

---

## 已确认修好的

### 1. Payment Service 已改成 fail-fast

证据：

- `pos-payment-service/src/main/java/com/developer/pos/payment/config/ProviderConfig.java:20-33`
  - `POS_CALLBACK_SECRET` 为空时直接抛 `IllegalStateException`
  - `PAYMENT_SERVICE_API_KEY` 为空时直接抛 `IllegalStateException`
- `pos-payment-service/src/main/java/com/developer/pos/payment/config/SecurityConfig.java:77-83`
  - 当 key 未配置时直接返回 `503`
  - 不再把请求自动认证成 `SERVICE`

这项我认可为已修复。

### 2. Bootstrap 已加 secret gate 和并发保护

证据：

- `pos-backend/src/main/java/com/developer/pos/auth/controller/AuthController.java:42-63`
  - bootstrap 需要 `X-Bootstrap-Secret`
  - `auth.bootstrap.secret` 未配置时直接禁用
- `pos-backend/src/main/java/com/developer/pos/auth/service/AuthService.java:53-66`
  - `@Transactional`
  - `synchronized`

这项从“公开可抢占”改成了“受控初始化入口”，方向正确，我认可风险已经显著收口。

### 3. Callback 失败后不再继续标记为 `SUCCEEDED`

证据：

- `pos-payment-service/src/main/java/com/developer/pos/payment/core/PaymentStatus.java:3-12`
  - 新增 `SETTLEMENT_FAILED`
- `pos-payment-service/src/main/java/com/developer/pos/payment/orchestrator/PaymentOrchestrator.java:83-88`
- `pos-payment-service/src/main/java/com/developer/pos/payment/orchestrator/PaymentOrchestrator.java:114-119`
  - callback 重试耗尽后，intent 改成 `SETTLEMENT_FAILED`

这项我认可为已修复。

### 4. Maven wrapper 脚本已恢复正常

证据：

- `pos-backend/mvnw`
- `pos-payment-service/mvnw`

当前文件已恢复为真实 Apache `mvnw` 脚本，不再是 `404: Not Found` 占位内容。

这项我认可为已修复。

---

## 新的阻断项

### P1. `nginx/prod.conf` 引用了 3 个不存在的生产服务，当前 production compose 配置不闭合

严重级别：High  
发版影响：阻断

证据：

- `nginx/prod.conf:5-15`
  - 定义了：
    - `pos_frontend -> pos-frontend:80`
    - `qr_frontend -> qr-frontend:80`
    - `platform_frontend -> platform-admin:80`
- `nginx/prod.conf:35-62`
  - `/pos/`、`/qr/`、`/platform/` 都会代理到这些 upstream
- `docker-compose.prod.yml:1-80`
  - 只定义了：
    - `mysql`
    - `pos-backend`
    - `pos-payment-service`
    - `pc-admin`
    - `nginx`
  - 完全没有：
    - `pos-frontend`
    - `qr-frontend`
    - `platform-admin`

为什么这是阻断：

当前 production nginx 配置和 production compose 配置彼此不一致。  
这不是小问题，而是会直接导致生产部署不完整，甚至可能让 nginx 在加载 upstream 时就报错，或者至少让 `/pos/`、`/qr/`、`/platform/` 全部不可用。

如果 release 要带这三个入口，那么 compose 缺服务定义。  
如果 release 不带这三个入口，那么 nginx 不应该代理它们。

必须修复：

1. 要么在 `docker-compose.prod.yml` 中把对应前端服务补齐
2. 要么从 `nginx/prod.conf` 删除这些并未发布的 upstream 和路由

### P1. 首个管理员 bootstrap 的运维接线没有完成，标准部署路径下实际上不可用

严重级别：High  
发版影响：阻断

证据：

- `pos-backend/src/main/resources/application.yml:25-30`
  - bootstrap 依赖 `AUTH_BOOTSTRAP_SECRET`
- 但仓库内以下标准运维入口没有把它接进去：
  - `.env.example`
  - `docker-compose.prod.yml`
  - `docker-compose.yml`
- 我做了仓库检索，没有找到 `AUTH_BOOTSTRAP_SECRET` 的配置样板或部署文档

为什么这是阻断：

你现在的系统已经没有默认 seed 用户了，这本来是对的。  
但标准部署产物里也没有把 `AUTH_BOOTSTRAP_SECRET` 接进去，意味着：

1. fresh deployment 后 `auth_users` 为空
2. bootstrap 代码存在
3. 但标准 compose / env 样板并不会把 secret 注入容器
4. 所以 bootstrap 实际处于禁用状态

换句话说，repo 当前没有给出一个可复现、可交付的“首个管理员初始化路径”。

必须修复：

1. 在 `.env.example` 中补 `AUTH_BOOTSTRAP_SECRET`
2. 在 `docker-compose.prod.yml` 中把 `AUTH_BOOTSTRAP_SECRET` 传给 `pos-backend`
3. 最好在 `RUN.md` / `DEPLOY_AWS.md` 中写清第一次初始化管理员的步骤

---

## 运维 / 验证缺口

### P2. 本地 `docker-compose.yml` 现在无法正常启动 `pos-payment-service`

严重级别：Medium  
发版影响：建议修复

证据：

- `docker-compose.yml:49-55`
  - `pos-payment-service` 只传了：
    - `PAYMENT_DB_URL`
    - `PAYMENT_DB_USERNAME`
    - `PAYMENT_DB_PASSWORD`
    - `POS_CALLBACK_URL`
    - `POS_CALLBACK_SECRET`
  - 没有传 `PAYMENT_SERVICE_API_KEY`
- 而 `ProviderConfig.java:27-29` 现在要求 `PAYMENT_SERVICE_API_KEY` 必须存在，否则启动失败

影响：

这说明 local compose 已经和代码的新安全要求脱节。  
虽然这不一定阻止 production release，但会阻止本地联调和 reviewer 复现。

### P2. 我无法在当前机器上完成 Java 构建验证，因为缺少 Java Runtime

严重级别：Medium  
发版影响：验证缺口

证据：

- `./mvnw -v` in `pos-backend` -> `Unable to locate a Java Runtime`
- `./mvnw -v` in `pos-payment-service` -> `Unable to locate a Java Runtime`

这说明 wrapper 脚本已经恢复，但当前环境没有 JRE/JDK，所以我还不能在这台机器上签“后端编译和测试已本地通过”。

另外我本轮已确认：

1. `platform-admin` build：通过
2. `pc-admin` build：通过

---

## Open Question

我还看到一个需要你们自己确认的地方：

- 当前仓库里我没有搜到任何调用方会带 `X-Service-Key` 去访问 `pos-payment-service`
- 也没有在 repo 内看到明确的 in-repo create-intent caller

这不一定是 bug，因为你们可能是通过外部系统或手工验证。  
但从代码仓库视角，我还不能证明这个 payment service 已经完成“可被当前系统正常使用”的端到端接线。

---

## 最终判断

如果只问“你列的 4 个问题代码有没有认真改”，我的答案是：

- `3.5 / 4` 基本成立

如果问“现在能不能放行上线”，我的答案还是：

- `不能`

原因不是那 4 个点本身，而是 release artifact 还有两个新的 deployment blocker：

1. `nginx/prod.conf` 和 `docker-compose.prod.yml` 不闭合
2. bootstrap 的标准运维接线没完成

当前结论：`BLOCK RELEASE`
