# Release Re-Review After Compose Wiring Fix Pass

日期：2026-03-27  
Reviewer：Codex  
分支：`codex/reservations-transfer-backend`  
复核基线：`b660934..868ab4e`

## 结论

当前结论仍然是：`BLOCK RELEASE`

这轮修复里，上一次我指出的 4 个问题基本都修对了：

1. `PAYMENT_SERVICE_API_KEY / POS_CALLBACK_SECRET` 已改成 fail-fast
2. bootstrap 已接入 `X-Bootstrap-Secret`
3. callback 失败后会转成 `SETTLEMENT_FAILED`
4. `mvnw` 已恢复成真实脚本

但这次新增了一个新的 production blocker：

- `docker-compose.prod.yml` 现在虽然补了 `pos-frontend / qr-frontend / platform-admin` 三个服务，
  但它们引用的 `Dockerfile.prod` 在仓库里根本不存在，导致 prod compose build 直接失败。

所以当前状态不是“可以上线”，而是“原有安全问题基本修了，但发布配置本身坏了”。

---

## 已确认通过

### 1. Payment Service fail-fast

证据：

- `pos-payment-service/src/main/java/com/developer/pos/payment/config/ProviderConfig.java:20-33`
- `pos-payment-service/src/main/java/com/developer/pos/payment/config/SecurityConfig.java:77-83`

结论：

这项我认可为已修复。

### 2. Bootstrap secret 接线

证据：

- `pos-backend/src/main/java/com/developer/pos/auth/controller/AuthController.java:42-63`
- `pos-backend/src/main/resources/application.yml:25-30`
- `docker-compose.prod.yml:35-37`
- `docker-compose.yml:32-35`
- `.env.example:7-12`

结论：

这项我认可为已修复。

### 3. Payment callback 失败状态

证据：

- `pos-payment-service/src/main/java/com/developer/pos/payment/core/PaymentStatus.java:3-12`
- `pos-payment-service/src/main/java/com/developer/pos/payment/orchestrator/PaymentOrchestrator.java:82-88`
- `pos-payment-service/src/main/java/com/developer/pos/payment/orchestrator/PaymentOrchestrator.java:114-119`

结论：

这项我认可为已修复。

### 4. Maven wrapper 脚本

证据：

- `pos-backend/mvnw`
- `pos-payment-service/mvnw`
- `pos-backend/.mvn/wrapper/maven-wrapper.jar`
- `pos-payment-service/.mvn/wrapper/maven-wrapper.jar`

结论：

wrapper 文件本体已经恢复正常。我实际运行 `./mvnw -v` 时，失败原因已经变成“当前机器缺少 Java Runtime”，不再是 wrapper 文件损坏。

---

## 新的阻断项

### P1. `docker-compose.prod.yml` 引用了不存在的 `Dockerfile.prod`，生产构建当前会直接失败

严重级别：High  
发版影响：阻断

证据：

- `docker-compose.prod.yml:60-85`
  - 新增：
    - `pos-frontend`
    - `qr-frontend`
    - `platform-admin`
  - 它们都指定 `dockerfile: Dockerfile.prod`
- 但仓库内实际存在的 Dockerfile 只有：
  - `pc-admin/Dockerfile`
  - `pc-admin/Dockerfile.prod`
  - `pos-backend/Dockerfile`
  - `pos-payment-service/Dockerfile`
- 我实际执行：
  - `docker compose -f docker-compose.prod.yml build pos-frontend`
  - 结果直接失败：
    - `failed to read dockerfile: open Dockerfile.prod: no such file or directory`

为什么这是阻断：

这不是“可能有风险”，而是 release 配置现在已经明确不可构建。  
只要走标准 prod compose build，前端镜像阶段就会直接挂掉。

必须修复：

1. 要么给 `android-preview-web`、`qr-ordering-web`、`platform-admin` 补真实 `Dockerfile.prod`
2. 要么把 compose 改成引用实际存在的 Dockerfile
3. 修完后至少重新跑一次：
   - `docker compose -f docker-compose.prod.yml build pos-frontend`
   - `docker compose -f docker-compose.prod.yml build qr-frontend`
   - `docker compose -f docker-compose.prod.yml build platform-admin`

---

## 额外说明

### Java 构建验证

我这轮可以确认：

- Maven wrapper 已恢复正常

但我还不能确认：

- `pos-backend` / `pos-payment-service` 的本地 Java 构建是否通过

原因是当前机器没有 Java Runtime。  
我实际执行 `./mvnw -v`，错误已经变成：

- `Unable to locate a Java Runtime`

这属于环境缺口，不是 wrapper 代码缺陷。

### 前端构建验证

我这轮没有重新跑新增三个 prod frontend 的 Docker build；  
但我已经用 `docker compose build pos-frontend` 证明了 production compose 当前会在 Dockerfile 阶段直接失败。

---

## Reviewer 最终判断

这轮不能放行，不是因为前面那 4 个安全问题没修，而是因为：

- 生产 compose 现在引用了不存在的 Dockerfile
- release artifact 无法完成标准构建

所以当前结论仍然是：`BLOCK RELEASE`
