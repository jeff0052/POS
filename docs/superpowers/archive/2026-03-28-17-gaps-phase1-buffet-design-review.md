# Phase 1 Buffet Design Review

## Review Verdict

Current verdict: `NOT APPROVED FOR IMPLEMENTATION`

这份设计文档有不少方向是对的，但它还不能直接交给 Claude 开始实现。当前最严重的问题不是“细节没补完”，而是几个核心设计在下面三层之间没有对齐：

- 文档内部自洽性
- 当前代码和数据库真实形态
- `docs/66` / `docs/75` 里已经声明过的目标数据模型

如果现在按这份文档直接开工，最可能出现的结果是：

- Flyway 编号和字段口径再次漂移
- buffet / settlement / QR 三条链路做到一半就发现主键与状态机选错
- 前端接了新 API，但后端和现有表无法稳定承接
- Claude 在“是改旧模型还是重做新模型”之间反复返工

## Review Scope

本次 review 不是纯文档挑字眼，而是把设计稿和当前仓库做了交叉核对：

- 设计稿：
  - `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md`
- 现有 Flyway / 实体 / 服务 / 控制器：
  - `pos-backend/src/main/resources/db/migration/v2/V003__catalog_and_sku.sql`
  - `pos-backend/src/main/resources/db/migration/v2/V004__active_table_order.sql`
  - `pos-backend/src/main/resources/db/migration/v2/V008__settlement_and_payment.sql`
  - `pos-backend/src/main/resources/db/migration/v2/V009__member_core.sql`
  - `pos-backend/src/main/resources/db/migration/v2/V012__table_session_and_submitted_orders.sql`
  - `pos-backend/src/main/resources/db/migration/v2/V013__member_recharge_and_points.sql`
  - `pos-backend/src/main/resources/db/migration/v2/V015__payment_attempts.sql`
  - `pos-backend/src/main/resources/db/migration/v2/V031__create_action_log.sql`
  - `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java`
  - `pos-backend/src/main/java/com/developer/pos/v2/order/interfaces/rest/ActiveTableOrderV2Controller.java`
  - `pos-backend/src/main/java/com/developer/pos/v2/order/interfaces/rest/QrOrderingV2Controller.java`
  - `pos-backend/src/main/java/com/developer/pos/v2/catalog/application/service/QrMenuApplicationService.java`
  - `pos-backend/src/main/java/com/developer/pos/v2/catalog/infrastructure/persistence/repository/JpaQrMenuRepository.java`
  - `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java`
  - `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/VibeCashPaymentApplicationService.java`
  - `pos-backend/src/main/java/com/developer/pos/v2/settlement/infrastructure/persistence/entity/SettlementRecordEntity.java`
  - `pos-backend/src/main/java/com/developer/pos/v2/store/application/service/TableTransferApplicationService.java`
  - `pos-backend/src/main/java/com/developer/pos/v2/mcp/infrastructure/ActionLogEntity.java`
- 已存在的目标模型文档：
  - `docs/66-aurora-data-model-design.md`
  - `docs/75-complete-database-schema.md`

## Blocker Findings

### 1. P0: G01 并台设计同时违背当前 schema、自己的 ADR、和当前状态机

Severity: `P0 / implementation blocker`

Spec evidence:

- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:117-145`
- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:1556-1567`

Current code/schema evidence:

- `pos-backend/src/main/resources/db/migration/v2/V004__active_table_order.sql:34-53`
- `pos-backend/src/main/resources/db/migration/v2/V012__table_session_and_submitted_orders.sql:1-17`
- `pos-backend/src/main/java/com/developer/pos/v2/order/infrastructure/persistence/entity/TableSessionEntity.java:20-39`
- `pos-backend/src/main/java/com/developer/pos/v2/store/application/service/TableTransferApplicationService.java:53-72`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java:178-250`

What is wrong:

- 文档第 120-122 行说要把 `active_table_order_items.session_id` 指向 `masterSession`，但当前 `active_table_order_items` 根本没有 `session_id` 列，只有 `active_order_id`。
- 文档第 118、135 行使用 `mergedSession.guest_count`，但 G01 自己只给 `table_sessions` 增加了 `merged_into_session_id` 和 `total_guest_count`，没有 `guest_count`。`guest_count` 是 Phase 1 buffet 后面才加的字段。
- 文档第 128、145 行使用 `session_status = SETTLED`，但当前 `table_sessions.session_status` 实际是 `OPEN/CLOSED`，结账完成逻辑也确实是把 session 置为 `CLOSED`，不是 `SETTLED`。
- ADR D01 明确说“并台用 merge_records 表 + session 指针，不拷贝 order_items”，但 G01 业务逻辑又写了“把 order_items 指向 masterSession”以及“拆台按 snapshot 还原 order_items”，等于同时在写两套互斥方案。

Why this blocks implementation:

- Claude 无法知道应该实现“指针聚合”还是“物理搬迁 item”。
- 如果选错，结账、拆台、历史单据、厨房单、报表都会出现二义性。
- 并台是 P0 场景，不能用一份内部互相打架的 spec 开工。

Required change for Claude:

- 只保留一套并台模型，并在文档中明确写死。
- 我建议保留 ADR D01 的方向：`table_sessions` 自引用指针 + `table_merge_records` 记录操作轨迹，结账时聚合 session，不搬运 item 所有权。
- 明确“聚合对象”到底是什么：
  - active draft 订单
  - submitted orders
  - settlement preview
- 把 `session_status` 统一成当前系统真实使用的 `OPEN/CLOSED`，不要再出现 `SETTLED`。
- 如果确实需要人数合并，先把 `guest_count` 纳入统一的 `table_sessions` 扩展模型，再在 G01 中引用。

Acceptance criteria:

- G01 文档中不再出现不存在的列名或状态值。
- “并台”和“拆台”都只基于一套一致的数据归属模型。
- 结账、报表、厨房、桌转移的 session 语义能统一解释。

### 2. P0: G02 支付叠加设计没有承接 `freeze/confirm` 的持久化模型，而且引用了错误的会员余额模型

Severity: `P0 / implementation blocker`

Spec evidence:

- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:235-301`

Current code/schema evidence:

- `pos-backend/src/main/resources/db/migration/v2/V009__member_core.sql:17-30`
- `pos-backend/src/main/resources/db/migration/v2/V013__member_recharge_and_points.sql:16-30`
- `pos-backend/src/main/resources/db/migration/v2/V008__settlement_and_payment.sql:1-18`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/TableSettlementV2Controller.java:23-87`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java:178-309`
- `pos-backend/src/main/java/com/developer/pos/v2/member/application/service/MemberApplicationService.java:316-345`

What is wrong:

- 文档里有完整的“两段式流程”：
  - `calculate-stacking`
  - `confirm`
  - 中间还引入了 `stackingCalculationId`
- 但设计里没有任何表或实体去保存：
  - calculation snapshot
  - 冻结中的积分/储值
  - confirm 的幂等 token
  - confirm/release 的状态
- 业务伪代码用了 `member.available_points` 和 `member.available_cash_cents`，这跟当前真实模型不一致。当前余额在 `member_accounts.points_balance` / `cash_balance_cents`，而积分流水在 `member_points_ledger`。文档开头自己的设计原则也写了“积分分批过期 FIFO，走 `points_batches`”，结果 G02 又回到了一个不存在的 `member.available_points`。
- API 也选错了聚合对象：文档写的是 `/api/v2/settlements/{orderId}/...`，但当前系统结账主链路是“按 table/session 聚合 submitted orders”，`TableSettlementV2Controller` 和 `CashierSettlementApplicationService` 都是 table-centric / session-centric。

Why this blocks implementation:

- `stackingCalculationId` 没地方落库，就没有真正的“先算、再确认、失败再释放”。
- 外部支付失败时无法准确回滚冻结额度。
- 当前系统是“桌台/桌次结账”，新文档却引入“orderId 结账”，Claude 会被迫同时改控制器、实体和前端主路径。

Required change for Claude:

- 先补齐一套明确的持久化模型，再写 API：
  - settlement intent / stacking calculation
  - member points hold
  - member cash hold
  - coupon reservation / consume record
- 明确主聚合键到底是：
  - table session
  - active order
  - submitted order batch
- 我的建议是延续当前系统：以 `table_session` 为结账聚合根，`calculate` 和 `confirm` 也按 session 或 table 来走。
- G02 里所有余额字段名必须对齐真实模型，至少不要再出现 `member.available_points` 这种当前不存在的概念。

Acceptance criteria:

- `calculate`、`confirm`、`release` 都有清晰持久化承载。
- 外部支付失败、用户取消、接口重试时不会多扣积分/储值。
- API contract 与当前 table/session 结账主路径统一。

### 3. P0: G10 动态二维码方案在密码学和端到端接入上都不成立

Severity: `P0 / implementation blocker`

Spec evidence:

- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:803-828`
- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:1567`

Current code/frontend evidence:

- `pos-backend/src/main/java/com/developer/pos/v2/order/interfaces/rest/QrOrderingV2Controller.java:35-67`
- `pos-backend/src/main/java/com/developer/pos/v2/catalog/application/service/QrMenuApplicationService.java:43-100`
- `qr-ordering-web/src/App.tsx:326-329`
- `qr-ordering-web/src/App.tsx:447-514`

What is wrong:

- 文档第 804 行说 `qr_token = HMAC-SHA256(storeSecret, tableId + timestamp + random)`。
- 但 QR URL 只传 `t={qr_token}`，没有把 `timestamp/random` 带出去，也没有说明服务端持久化这两个输入。因此第 811 行“用 storeSecret 重算对比”在当前描述下做不到。
- 更关键的是，即使扫码入口验签做成了，当前公开点单 API 仍然只吃 `storeCode + tableCode`：
  - `/api/v2/qr-ordering/context`
  - `/api/v2/qr-ordering/menu`
  - `/api/v2/qr-ordering/submit`
- `qr-ordering-web` 当前也是直接从 URL query 里读 `storeCode` 和 `table`，然后后续所有请求都不再带 token。

Why this blocks implementation:

- 现在的设计最多只能“保护第一跳扫码入口”，无法保护后续菜单、上下文、提交接口。
- 一旦入口页把桌号翻译成普通 URL 参数，用户仍然可以把最终链接转发出去，动态 QR 的安全收益接近于 0。
- Claude 如果只照 spec 实现一个 `/qr/{storeId}/{tableId}?t=...`，最后只是做出一个看起来更安全、实际并没有收口的系统。

Required change for Claude:

- 先选定真正可执行的一种方案，再往下写：
  - 方案 A：opaque token，服务端表里存 token -> table/session 映射
  - 方案 B：token 自带签名负载（table、issuedAt、expiresAt、nonce），服务端只做验签
- 无论哪种，都必须补上“扫码后如何把后续 QR ordering API 绑定到一次已验证会话”的设计：
  - server-side exchange 成短期 session token
  - 或所有公开接口都带并校验同一个 QR token
- 明确兼容策略：当前 `storeCode/tableCode` URL 如何过渡，老二维码是否继续可用。

Acceptance criteria:

- 服务端能根据请求里的信息真正完成 token 校验。
- 菜单、上下文、提交接口与已验证的扫码会话绑定，而不是只保护入口路由。
- 复制最终链接不能绕过动态 QR 的防护目标。

### 4. P0: Phase 1 buffet 价格语义会在 `submit-to-kitchen` 之后丢失

Severity: `P0 / implementation blocker`

Spec evidence:

- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:1331-1380`
- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:1466-1478`

Current code/schema evidence:

- `pos-backend/src/main/resources/db/migration/v2/V012__table_session_and_submitted_orders.sql:19-64`
- `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java:409-455`
- `pos-backend/src/main/java/com/developer/pos/v2/order/infrastructure/persistence/entity/SubmittedOrderItemEntity.java:21-44`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java:188-249`

What is wrong:

- 文档把 buffet 标识和差价只加到了 `active_table_order_items`：
  - `is_buffet_included`
  - `buffet_surcharge_cents`
- 但当前系统只把 active draft 当作临时态，真正用于后续结账的，是 `submitted_orders` / `submitted_order_items`。
- `ActiveTableOrderApplicationService.persistSubmittedOrder()` 在提交到厨房时，会复制 active items 到 `submitted_order_items`，但当前复制逻辑只带：
  - sku snapshot
  - quantity
  - unit price
  - line total
  - remark
- 也就是说，一旦提交到厨房，buffet inclusion / surcharge / package 内外价差这些语义就丢了。

Why this blocks implementation:

- 结账、票据、报表、退款都需要知道某个 item 是：
  - 套餐内免费
  - 套餐内加价
  - 套餐外原价
- 如果这些信息只存在 active draft，系统进入 submitted/settlement 阶段后就无法可靠重建账单。

Required change for Claude:

- buffet 的价格语义必须持久化到“提交后仍可追踪”的层：
  - `submitted_order_items`
  - 或独立的 buffet pricing snapshot
- 自助餐总价的组成部分也要有明确落点：
  - package total
  - surcharge total
  - extra total
  - overtime fee
- 收据打印、结账预览、报表口径必须都基于同一份持久化快照，而不是依赖运行时重算猜测。

Acceptance criteria:

- 从点单到 submit 到 settlement 到 receipt，buffet 价格语义不丢失。
- 历史订单离开 active 状态后，仍能 100% 重建账单分区。

## High-Risk Findings

### 5. P1: 自助餐 session API 存在循环依赖，而且与现有 table-based 前端契约不一致

Severity: `P1 / high`

Spec evidence:

- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:1311-1321`
- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:1466-1478`

Current code/frontend evidence:

- `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java:396-406`
- `pos-backend/src/main/java/com/developer/pos/v2/order/interfaces/rest/ActiveTableOrderV2Controller.java:22-93`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/TableSettlementV2Controller.java:23-87`
- `qr-ordering-web/src/App.tsx:452-459`

What is wrong:

- 文档 19.3 写的是“开台选模式 -> 创建 table_session -> 带 buffet_package_id / guest_count / buffet_started_at”。
- 但 19.8 API 又写成 `POST /api/v2/sessions/{sessionId}/buffet/start`。
- 这意味着“开始 buffet”需要先有 sessionId，但 sessionId 本身又是在 buffet 开台流程里才产生。
- 同时，当前前后端主交互几乎都是 table-based，不是 sessionId-based。前端现在也没有一个稳定的 sessionId 作为路由主键。

Why this matters:

- 实现时一定会出现 controller/service 分层摇摆：
  - 是 table 先开 session，再 start buffet
  - 还是 buffet start 本身创建 session
- Claude 如果不先统一这件事，前端接口会很快来回重构。

Required change for Claude:

- 二选一并写死：
  - 方案 A：`POST /api/v2/stores/{storeId}/tables/{tableId}/session/open`，body 直接带 `diningMode/packageId/guestCount`
  - 方案 B：显式先创建 session，再对 session 调用 buffet-specific API
- 我更建议 A，因为它更符合当前系统的 table-based 交互路径。

Acceptance criteria:

- 文档中不再同时出现“创建 session”与“必须已存在 sessionId 才能开始 buffet”的循环依赖。
- 前端拿到的主键和后端主链路一致。

### 6. P1: G04 支付失败重试设计与当前 `payment_attempts` 生命周期和状态枚举冲突

Severity: `P1 / high`

Spec evidence:

- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:347-393`

Current code/schema evidence:

- `pos-backend/src/main/resources/db/migration/v2/V015__payment_attempts.sql:1-24`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/VibeCashPaymentApplicationService.java:82-225`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/TableSettlementV2Controller.java:67-87`

What is wrong:

- 当前支付链路真实使用的 attempt 状态包括：
  - `PENDING_CUSTOMER`
  - `FAILED`
  - `SUCCEEDED`
  - `SETTLED`
  - `EXPIRED`
- G04 文档却要把 `attempt_status` 改写成：
  - `PENDING`
  - `PROCESSING`
  - `SUCCESS`
  - `FAILED`
  - `CANCELLED`
  - `REPLACED`
- 这不是简单“多加几个值”，而是会直接和 `VibeCashPaymentApplicationService.handleWebhook()` 里的既有状态流转冲突。
- 同时，当前支付 API 是 table-scoped 的：
  - `/api/v2/stores/{storeId}/tables/{tableId}/payment/...`
- 设计稿改成了 attempt-scoped 的 `/api/v2/payments/{attemptId}/...`，但没有交代哪个 controller/service 拥有这个新主路径。

Why this matters:

- Claude 如果按文档改状态注释，不同步改 webhook / settlement 触发逻辑，就会把现有数字支付流程弄坏。
- 重试/换方式是必要功能，但不能通过“推翻既有状态机而不写迁移策略”来做。

Required change for Claude:

- 先保留当前 attempt lifecycle 作为基线，再在其上扩展 retry/switch。
- 文档必须明确：
  - 哪些状态保留
  - 哪些状态新增
  - webhook 到 settlement 的状态迁移怎么改
  - retry/switch 由哪个 controller 暴露
- 如果要转为 attempt-centric API，也要把 table-scoped 当前路径的兼容/迁移计划写出来。

Acceptance criteria:

- 文档里的状态机可以直接映射到现有支付服务代码，不需要开发者猜。
- 重试/换方式不会破坏现有 webhook -> settlement 成功链路。

### 7. P1: G17 审计日志设计把 MCP 工具日志、人工审计、审批工作流三件事混进了一张表

Severity: `P1 / high`

Spec evidence:

- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:1213-1275`

Current code/schema evidence:

- `pos-backend/src/main/resources/db/migration/v2/V031__create_action_log.sql:1-16`
- `pos-backend/src/main/java/com/developer/pos/v2/mcp/infrastructure/ActionLogEntity.java:13-44`
- `pos-backend/src/main/java/com/developer/pos/v2/common/entity/BaseAuditableEntity.java:7-27`

What is wrong:

- `action_log` 当前并不是一张“空白待设计”的业务审计表，它已经明确服务于 MCP / AI 工具调用日志，字段语义包括：
  - `tool_name`
  - `decision_source`
  - `approval_status`
  - `params_json`
  - `result_json`
- G17 又往里面塞：
  - 人工操作审计
  - before/after snapshot
  - store 维度查询
  - pending approval 队列
- 但 ALTER 里没有补 `store_id` / `merchant_id`，文档 API 却写了 `/api/v2/stores/{storeId}/audit-logs`，当前表结构根本无从按门店过滤。
- AOP 伪代码也自相矛盾：
  - 一边说“执行方法后写 after_snapshot”
  - 一边又说“需要审批时先生成审批请求，审批通过后才实际执行”
- 这说明当前设计其实把“审计记录”和“待审批命令”混成了一个概念，但没有给出可靠的数据模型。

Why this matters:

- 审计日志应当是 append-only evidence。
- 审批工作流需要的是 pending command / approval request。
- MCP 工具日志又是另一类查询维度。
- 三者混在一起，后续查询、权限、归档、性能都会很差。

Required change for Claude:

- 先做架构决策，不要直接改表：
  - 方案 A：保留 `action_log` 给 MCP/AI，新增 `audit_events` + `approval_requests`
  - 方案 B：彻底重塑 `action_log`，但必须补 `store_id/merchant_id/resource_type/resource_id/action_type`
- 无论哪种，审批待办都不能只靠一条“尚未执行”的审计记录来承载。

Acceptance criteria:

- 审计事件、审批请求、AI/MCP 工具日志三者边界清晰。
- `GET /stores/{storeId}/audit-logs` 能被底层 schema 真正支持。

### 8. P1: buffet 菜单过滤和多场景定价需要 query/service 重写，文档现在写得太轻了

Severity: `P1 / high`

Spec evidence:

- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:1323-1335`
- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:1472-1478`
- `docs/66-aurora-data-model-design.md:170-184`

Current code evidence:

- `pos-backend/src/main/java/com/developer/pos/v2/catalog/infrastructure/persistence/repository/JpaQrMenuRepository.java:11-48`
- `pos-backend/src/main/java/com/developer/pos/v2/catalog/application/service/QrMenuApplicationService.java:43-100`
- `pos-backend/src/main/resources/db/migration/v2/V003__catalog_and_sku.sql:15-57`

What is wrong:

- 当前 QR 菜单 SQL 直接返回 `skus.base_price_cents`，并且只按：
  - category active
  - product active
  - sku active
  - store_sku_availability
  来过滤。
- 它完全不知道：
  - `dining_mode`
  - `menu_time_slots`
  - `menu_time_slot_products`
  - `sku_price_overrides`
  - buffet package inclusion / surcharge
- 但当前 buffet 设计只在流程图里描述了“菜单过滤”和“价格标注”，没有把“必须替换当前 query layer”的工作明确成设计产物。

Why this matters:

- 这不是一个 controller 加参的小改动，而是菜单解析主链路重写。
- 如果 Claude 低估这件事，很容易做成：
  - schema 加完了
  - API 也加了
  - 但前台菜单还是按 base price 出，完全没走新逻辑

Required change for Claude:

- 明确新增一个统一的菜单解析服务，例如 `MenuResolutionService`，把 dining mode / time slot / price override / buffet package 一次解析完。
- 写清楚现有 `QrMenuApplicationService` 和 admin menu read path 如何复用或迁移这套逻辑。

Acceptance criteria:

- 文档不只是“表能装数据”，而是明确谁来解析和输出最终菜单。
- QR 端和 POS 端拿到的是同一套菜单裁剪和定价结果。

## Medium-Risk Findings

### 9. P2: 这份文档不是单一可信源，统计、迁移编号、引用边界都有漂移

Severity: `P2 / medium`

Evidence:

- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:1500-1513`
- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:1387-1392`
- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:1528-1533`
- `docs/66-aurora-data-model-design.md:11-13`
- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:13`

What is wrong:

- 文档写“改造表（8 张）”，实际列了 10 张。
- 19.5 里说：
  - `V072 = ALTER products`
  - `V075 = ALTER active_table_order_items`
- 最终 migration summary 又变成：
  - `V072 = ALTER products + ALTER active_table_order_items`
  - `V075 = ALTER store_tables`
- Phase 1 还宣称“完整实现设计”，但 4 张 Phase 1 新表 DDL 实际没写在这份文档里，而是引用 `docs/66`。
- 更大的问题是原则层也漂移：
  - `docs/66` 说“扩展现有表，不推翻”
  - 本文第 13 行说“不迁就旧表结构，该删就删”

Why this matters:

- Claude 最怕这种“多份文档都像权威，但口径不一样”的场景。
- 一旦编号和原则都漂移，迁移脚本、实现范围、review 标准都会失焦。

Required change for Claude:

- 先做文档整理，不要一边实现一边猜：
  - 指定哪份文档是 buffet/菜单/价格的最终权威
  - 重新编号 V070-V090
  - 把数量统计修正成真实数目
- 如果 Phase 1 新表确实以 `docs/66` 为准，就在本文写清楚“authoritative source = docs/66”，不要既说完整设计，又把关键 DDL 外链出去。

Acceptance criteria:

- Claude 可以只看一组清晰的权威文档就开始写 migration。
- 同一张表、同一个 migration version、同一个原则不会在多处冲突。

### 10. P2: 设计原则没有被自己后面的 DDL 落实

Severity: `P2 / medium`

Evidence:

- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:15-18`
- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:74-90`
- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:173-216`
- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:405-440`
- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:841-943`
- `docs/superpowers/specs/2026-03-28-17-gaps-phase1-buffet-design.md:988-1020`

What is wrong:

- 设计原则第 15 行写“新表全部带 `created_at`, `updated_at`, `created_by`, `updated_by`”。
- 但这份文档里列出的新表 DDL，几乎都没有 `created_by` / `updated_by`。
- 一部分表甚至连 `updated_at` 都没有，比如 `external_integration_logs`。
- 第 16 行写“每张业务表必须有 `store_id`”，但 `action_log` 改造方案仍然没有 store 维度。

Why this matters:

- 这会让 Claude 不知道到底应该“遵守原则补齐字段”，还是“以 DDL 为准”。
- 审计字段和多租隔离是架构基线，不该在设计稿里先立再破。

Required change for Claude:

- 重新对齐“原则”和“DDL 实际内容”。
- 如果不要求所有新表都有 `created_by/updated_by`，那就删掉这条原则。
- 如果要求，就逐表补齐。

Acceptance criteria:

- 文档原则不是口号，而是能在后续每张表 DDL 里看到对应落点。

## Directionally Good Decisions

下面这些方向我认为是可以保留的，不需要 Claude 全盘推翻：

- `table_merge_records` 作为并台操作轨迹表，这个方向是对的。
- G15 复用 `report_snapshots` 做多店对比，而不是再造一张横向对比表，这个方向是对的。
- G16 用 heartbeat + fallback mode 做 KDS 回退，方向合理。
- buffet package 与 package items 分表建模，方向合理。
- `sku_price_overrides` 作为多场景定价承载体，方向合理，但必须把解析层补齐。

## Minimum Rework Before Re-Review

Claude 下一轮修改，我要求至少完成下面 6 件事，才值得我做二审：

1. 重写 G01，并统一“并台的真实聚合根、item 归属模型、session 状态机”。
2. 重写 G02，并补齐 settlement intent / hold / confirm / release 的持久化设计。
3. 重写 G10，并给出动态 QR 的端到端闭环，而不是只保护扫码入口。
4. 重写 Phase 1 buffet 的落库链路，确保 buffet 价格语义能跨过 `submit-to-kitchen`。
5. 决定 `action_log` 是否继续作为 MCP 日志；如果是，就把人工审计/审批拆表。
6. 统一 V070-V090 migration 规划、表数量统计、以及本文与 `docs/66` 的权威边界。

## Re-Review Standard

我接受二审的最低标准是：

- 文档内部不再引用不存在的字段、状态、主键或 API 上下文。
- buffet / settlement / QR / audit 四条主链路都能和当前仓库真实结构接起来。
- migration plan 是单义的，Claude 不需要自己猜版本号或表归属。
- 关键设计不是“看起来完整”，而是能直接指导实体、Flyway、controller、service、frontend contract 同步落地。

在这 4 个标准满足之前，这份设计文档不应进入实现阶段。
