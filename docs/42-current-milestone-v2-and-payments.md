# Current Milestone: V2 and Payments

本文档用于记录截至当前时点的新系统阶段性里程碑，重点覆盖：

- V2 交易主线完成情况
- Web / Merchant Admin / Platform Admin 当前状态
- Android 原生端当前状态
- 统一支付、VibeCash、DCS 当前状态
- 已完成验证与待设备验证项

## 1. 里程碑结论

当前项目已经完成从原型验证到新系统骨架落地的关键跨越。

当前可以明确成立的结论：

- `Ordering` 主线已经完成
- V2 后端已经成为新系统核心交易底座
- Web 侧 POS preview、QR ordering、Merchant Admin 已经开始围绕 V2 工作
- Android 原生端已经完成 V2 支付链和 DCS 接入位点
- Android 原生端已经不再是 `compile-unverified`
- 统一支付架构已经正式建立

当前最重要的阶段性里程碑可以定义为：

**V2 transaction foundation completed, unified payment architecture established, Android DCS path compile-verified.**

## 2. 已完成范围

### 2.1 V2 核心交易

已完成：

- `table session` 模型引入
- `draft order`
- `submitted orders`
- POS 点菜
- QR 点菜
- 多次 `Send to kitchen`
- `Payment`
- `Collect payment`
- 结账后桌台释放

当前状态：

- `Ordering` 按当前 scope 视为 `100%`

### 2.2 Merchant / Platform 侧基础

已完成：

- Merchant Admin 开始接 V2
- 基础 CRM
- 基础 Promotion
- 基础 Reports
- Platform Admin skeleton

当前状态：

- Merchant Admin：已进入可持续完善阶段
- Platform Admin：已建立骨架，但未进入完整运营阶段

### 2.3 Android 原生端

已完成：

- 原生端 V2 code integration
- 原生端统一支付抽象
- `Cash`
- `DCS`
- `VibeCash` 路径接入位点
- `Settlement` 页面接入 DCS 终端动作
- `Refund` 页面接入 DCS card refund / void
- `gradlew` 与 wrapper 已补进仓库

当前状态：

- Android 原生端已从 `compile-unverified` 升级为 `compile-verified`

## 3. 支付里程碑状态

### 3.1 Unified Payment Architecture

已完成：

- `Provider / Method / Scheme` 三层模型
- `Payment Orchestrator` 设计
- `DCS / VibeCash / Cash` 适配器分层

已文档化：

- `39-unified-payment-architecture.md`
- `40-payment-integration-requirements.md`
- `41-payment-adapter-design.md`

### 3.2 VibeCash

已完成：

- 新系统后端 `payment_attempts`
- 发起支付 attempt
- 查询 payment attempt
- webhook 入口
- Android 不再直连 gateway secret，而是改走 backend

当前状态：

- 架构完成
- 最小闭环完成
- 真链路仍等待商户密钥

### 3.3 DCS

已完成：

- DCS AAR 接入
- MID / TID 配置
- `connect`
- `sign`
- `card sale`
- `query by order id`
- `sale void`
- `card refund`
- `terminal settlement`
- `sign off`
- Android 原生页面入口接通

当前状态：

- 代码接入完成
- 原生端 compile 验证完成
- 真机 / 终端验证待执行

## 4. 这轮实际验证结果

### 4.1 Web / Backend

已验证：

- V2 ordering 主线 smoke test
- POS / QR / Payment / Collect payment
- 结账后桌台释放
- 商户后台 V2 基础读接口

### 4.2 Android Build

本轮新增完成：

- 通过容器补齐 `gradlew`
- 通过容器执行真实编译：
  - `./gradlew assembleDebug`
- 编译结果：
  - `BUILD SUCCESSFUL`

APK 产物：

- `android-pos/app/build/outputs/apk/debug/app-debug.apk`

### 4.3 DCS 设备前验证

当前已能确认：

- AAR 已接入
- AIDL 回调签名已按真实 SDK 修正
- Hilt 注入已通过编译验证
- DCS 页面入口已落地

当前仍待确认：

- 设备上是否存在 `com.sunmi.dcspayment.AIDL_SERVICE`
- 终端实际连接是否成功
- 真卡交易是否成功
- 本地查单 / 撤销 / 退款 / 终端结算 / 签退是否可运行

## 5. 当前项目完成度更新

截至当前时点，建议统一按以下口径沟通：

- 整体项目：`55% - 60%`
- `Stage 1 Store Transaction MVP`：`75%`
- `Ordering`：`100%`
- Unified Payment Architecture：已建立
- Android DCS path：已 compile-verified，待真机验证

## 6. 当前阻塞项

### 已解决

- Android 缺少 `gradlew`
- 本机缺少 `gradle`
- Android compile-unverified
- DCS AAR 资源与 AIDL 签名问题

### 未解决

- DCS 真机设备验证
- VibeCash 商户密钥
- 厨房 / KDS
- Refund 全链路 V2 化
- Shift / 交班

## 7. 设备到手后的下一步

设备到手后建议按这条顺序进行：

1. 安装 Android APK
2. 打开 App，确认页面和路由正常
3. 进入 `Settlement` 页面，执行：
   - `Refresh`
   - `Run Terminal Settlement`
   - `Sign Off`
4. 进入支付流程，选择 `Card Terminal`
5. 跑一次最小卡交易
6. 用 `query` 回查
7. 进入 `Refund` 页面验证：
   - `Void Sale`
   - `Submit Refund`

## 8. 当前最推荐的下一阶段方向

在 DCS 真机验证完成后，建议优先进入：

1. `Refund V2`
2. `Kitchen / KDS`
3. `Shift / cashier handover`
4. `Platform Admin real data`
5. `VibeCash real key integration`

