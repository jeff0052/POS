# Release Re-Review After Frontend Dockerfile Pass

日期：2026-03-27  
Reviewer：Codex  
分支：`codex/reservations-transfer-backend`  
复核基线：`868ab4e..32d69fd`

## 结论

当前结论仍然是：`BLOCK RELEASE`

这轮修复里，有一个很重要的正向结果：

- `docker-compose.prod.yml` 现在已经能成功 build `pos-frontend`、`qr-frontend`、`platform-admin`

我实际执行了：

```bash
docker compose -f /Users/ontanetwork/Documents/Codex/docker-compose.prod.yml build pos-frontend qr-frontend platform-admin
```

结果：3 个镜像都成功构建。

但 release 仍然不能放行，因为我确认到一个新的运行时 blocker：

- 这 3 个前端虽然被部署在 `/pos/`、`/qr/`、`/platform/` 子路径下，
  但构建产物里的 JS/CSS 仍然使用根路径 `/assets/...`。
- 这样一来，浏览器请求静态资源时会打到主 nginx 的根路由，而不是对应前端容器。
- 结果大概率是资源 404 或页面白屏。

---

## 已确认通过

### 1. 三个前端的 `Dockerfile.prod` / `nginx.prod.conf` 已补齐，prod compose build 可以通过

证据：

- `android-preview-web/Dockerfile.prod`
- `qr-ordering-web/Dockerfile.prod`
- `platform-admin/Dockerfile.prod`
- `android-preview-web/nginx.prod.conf`
- `qr-ordering-web/nginx.prod.conf`
- `platform-admin/nginx.prod.conf`

实际验证：

- `docker compose -f docker-compose.prod.yml build pos-frontend qr-frontend platform-admin` -> 成功

这说明上一轮我指出的“prod compose 会因为缺少 Dockerfile.prod 直接失败”已经被修复。

---

## 新的阻断项

### P1. 三个前端仍然使用根路径 `/assets/...`，与 `/pos/`、`/qr/`、`/platform/` 子路径部署方式不兼容

严重级别：High  
发版影响：阻断

证据：

#### Platform Admin

- `platform-admin/vite.config.ts:1-17`
  - 没有 `base: "/platform/"`
- `platform-admin/dist/index.html`
  - `<script ... src="/assets/index-78wZpI98.js"></script>`
  - `<link ... href="/assets/index-CAWsOSN1.css">`

#### POS Frontend

- `android-preview-web/vite.config.ts:1-20`
  - 没有 `base: "/pos/"`
- `android-preview-web/dist/index.html`
  - `<script ... src="/assets/index-CuXZIFJx.js"></script>`
  - `<link ... href="/assets/index-CfPnpYV-.css">`

#### QR Frontend

- `qr-ordering-web/vite.config.ts:1-13`
  - 没有 `base: "/qr/"`
- `qr-ordering-web/dist/index.html`
  - `<script ... src="/assets/index-ColAos1F.js"></script>`
  - `<link ... href="/assets/index-Cj_-3-N8.css">`

#### 反向代理侧

- `nginx/prod.conf:35-62`
  - `/pos/`、`/qr/`、`/platform/` 分别代理到不同前端容器
- `nginx/prod.conf:97-105`
  - `/` 根路由仍然代理到 `pc-admin`

为什么这是阻断：

当前部署方式是“子路径挂载多个独立前端”：

1. `/` -> `pc-admin`
2. `/pos/` -> `pos-frontend`
3. `/qr/` -> `qr-frontend`
4. `/platform/` -> `platform-admin`

但这 3 个子应用的 build 产物仍把静态资源写成绝对根路径 `/assets/...`。

这意味着：

1. 用户访问 `/platform/`
2. HTML 从 `platform-admin` 容器返回
3. 浏览器继续请求 `/assets/index-78wZpI98.js`
4. 这个请求会落到主 nginx 的根路由 `/`
5. 根路由当前代理的是 `pc-admin`，而不是 `platform-admin`

同样的问题也会发生在 `/pos/` 和 `/qr/`。

换句话说：

- 构建能过
- 容器能起
- 但浏览器加载静态资源时路径错了
- 运行态仍然会坏

必须修复：

1. 给三个 Vite 项目分别设置正确的 `base`
   - `android-preview-web` -> `/pos/`
   - `qr-ordering-web` -> `/qr/`
   - `platform-admin` -> `/platform/`
2. 重新构建后确认 `dist/index.html` 中的资源路径变成：
   - `/pos/assets/...`
   - `/qr/assets/...`
   - `/platform/assets/...`
3. 然后再重新做一次 docker compose build / runtime smoke test

---

## Reviewer 最终判断

这轮不是“没修”，而是“修到了下一层问题”。

当前状态：

1. production compose 构建问题已解决
2. 但子路径部署的静态资源基路径仍然错误

所以当前结论仍然是：`BLOCK RELEASE`
