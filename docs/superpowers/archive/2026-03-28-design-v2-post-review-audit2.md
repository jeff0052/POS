# Design V2 Second-Pass Audit

## Verdict

Current verdict: `NOT APPROVED FOR IMPLEMENTATION`

结论不是“修复版没进步”，而是：

- `2026-03-28-design-v2-post-review.md` 已经修掉了上一轮不少核心 blocker
- 但 `2026-03-28-sprint-plan-complete.md` 仍然大量保留旧设计
- 两份文档现在不是一个一致的实现源

如果 Claude 现在同时参考这两份文档开工，极大概率会把已经修好的设计又写回旧问题。

## What Is Fixed

修复版主文档已经明显改善的点：

- 并台回到 `session` 指针聚合，不再移动 `order_items`
- 支付叠加回到 table/session 结算模型，不再漂到 `orderId`
- buffet 价格语义开始扩展到 `submitted_order_items`
- 审计日志从 `action_log` 拆到 `audit_trail`
- 动态 QR 改成 DB lookup + session token，不再硬写错误的 HMAC 方案
- buffet/start 改成 table-based，不再要求前端先拿 sessionId

这些方向我认可。

## Remaining Issues

### 1. P0: 两份文档仍然是“分叉状态”，Sprint plan 重新引入了上一轮 blocker

Evidence:

- 修复版：
  - `docs/superpowers/specs/2026-03-28-design-v2-post-review.md:355-418`
  - `docs/superpowers/specs/2026-03-28-design-v2-post-review.md:528-557`
  - `docs/superpowers/specs/2026-03-28-design-v2-post-review.md:571-589`
  - `docs/superpowers/specs/2026-03-28-design-v2-post-review.md:646-709`
- Sprint plan：
  - `docs/superpowers/specs/2026-03-28-sprint-plan-complete.md:138-162`
  - `docs/superpowers/specs/2026-03-28-sprint-plan-complete.md:303-320`
  - `docs/superpowers/specs/2026-03-28-sprint-plan-complete.md:768-789`
  - `docs/superpowers/specs/2026-03-28-sprint-plan-complete.md:991-1017`
  - `docs/superpowers/specs/2026-03-28-sprint-plan-complete.md:1358-1369`
  - `docs/superpowers/specs/2026-03-28-sprint-plan-complete.md:1473-1499`

What is wrong:

- 修复版说：
  - 动态 QR 用 DB token + JWT
  - buffet/start 用 `tables/{tableId}`
  - `action_log` 不动，新增 `audit_trail`
  - 支付状态机保留现有 `PENDING_CUSTOMER/SUCCEEDED/SETTLED/...`
- Sprint plan 却还在写：
  - HMAC QR + `?t=...&ts=...`
  - `POST /api/v2/sessions/{sessionId}/buffet/start`
  - 直接改 `action_log`
  - 把 `payment_attempts` 改成 `PENDING/PROCESSING/SUCCESS/...`
  - `confirmStacking(calculationId)` 继续使用缓存 calculation

Why this blocks implementation:

- Claude 无法知道哪份才是当前权威。
- 这不是小漂移，而是把上一轮已经拦下来的设计再次写回实现计划。

Required fix:

- 明确宣布 `2026-03-28-design-v2-post-review.md` 为新的权威设计源。
- Sprint plan 必须整体回写，至少把 QR、buffet/start、payment_attempt、audit、stacking 持久化这几段全部同步。

### 2. P1: 结算明细 migration 仍然引用了当前不存在的列 `settlement_records.total_amount_cents`

Evidence:

- `docs/superpowers/specs/2026-03-28-design-v2-post-review.md:272-283`
- `docs/superpowers/specs/2026-03-28-sprint-plan-complete.md:1335-1353`
- `pos-backend/src/main/resources/db/migration/v2/V008__settlement_and_payment.sql:1-18`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/infrastructure/persistence/entity/SettlementRecordEntity.java:45-58`

What is wrong:

- 两份新文档都把 stacking 字段加在 `AFTER total_amount_cents`。
- 但当前真实表 `settlement_records` 只有：
  - `payable_amount_cents`
  - `collected_amount_cents`
  - `refunded_amount_cents`
- 根本没有 `total_amount_cents`。

Why this matters:

- 这不是抽象层问题，是 migration 会直接失败的问题。

Required fix:

- 把 DDL 对齐当前真实列名，通常应基于 `payable_amount_cents` 或明确新增一个总额字段后再引用。

### 3. P1: 动态 QR 方案仍然没有说明“空桌扫码时 sessionId 从哪来”

Evidence:

- `docs/superpowers/specs/2026-03-28-design-v2-post-review.md:379-386`
- `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java:79-101`
- `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java:396-406`
- `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java:425-443`

What is wrong:

- 修复版 QR 设计在扫码成功后签发 `ordering_session_token`，payload 里包含 `sessionId`。
- 但当前 QR ordering 真实链路里，空桌扫码时不一定已有 open session。
- 现有实现是：
  - `getQrOrderingContext()` 可以在没有 session 的情况下工作
  - `findOrCreateOpenSession()` 是在提交到厨房 / 持久化 submitted order 时才创建 session

Why this matters:

- 如果空桌扫码时没有 session，JWT payload 的 `sessionId` 就没有来源。
- Claude 实现时会卡在“扫码即创建 session”还是“token 不带 sessionId，后续懒创建”。

Required fix:

- 二选一并写死：
  - 扫码成功即创建 open session
  - 或 `ordering_session_token` 只绑定 `storeId/tableId`，session 仍然懒创建

### 4. P1: coupon 的“冻结态”仍然没有被设计成一个明确的一致模型

Evidence:

- `docs/superpowers/specs/2026-03-28-design-v2-post-review.md:304-315`
- `docs/superpowers/specs/2026-03-28-design-v2-post-review.md:334-338`
- `docs/75-complete-database-schema.md:1487-1505`

What is wrong:

- 修复版引入了 `settlement_payment_holds`，其中可以记录 `coupon_id`。
- 但文档没有定义 coupon 自身的 hold 语义，只写了：
  - collect 时“标记券已使用”
  - 失败时“恢复 AVAILABLE”
- 当前 `member_coupons` 只有一个通用 `coupon_status`，没有明确的 `LOCKED` 设计。

Why this matters:

- “已使用但可恢复”会把 `USED` 同时当成“冻结中”和“最终已消费”。
- 并发结算时，coupon 防重依赖什么约束、什么行锁、什么状态转换，文档还没有写清楚。

Required fix:

- 明确 coupon 的一种冻结表达方式：
  - 给 `member_coupons` 增加 `LOCKED`
  - 或完全不改 coupon 状态，只由 `settlement_payment_holds` + 唯一约束 / 行锁承载占用
- 无论哪种，都要写清楚并发防重策略。

### 5. P1: settlement API 允许传入新的 `memberId`，但没有定义“结算时换绑会员”语义

Evidence:

- `docs/superpowers/specs/2026-03-28-design-v2-post-review.md:289-308`
- `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java:112-145`
- `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java:425-442`

What is wrong:

- 修复版 `preview-stacking` / `collect-stacking` body 都接收 `memberId`。
- 但当前系统里，table settlement 的会员信息是从 `submitted_orders.member_id` 汇总来的。
- `persistSubmittedOrder()` 也是在订单提交时就把 `memberId` 写进 `submitted_orders`。

Why this matters:

- 如果结算时允许换绑一个新的会员，那么必须同时定义：
  - 原有 `submitted_orders.member_id` 是否回写
  - 会员等级折扣是否重算
  - coupon / points / cash owner 是否允许与历史 order member 不一致

Required fix:

- 明确规则：
  - 要么结算时不允许传新的 `memberId`，只能用 session 已绑定会员
  - 要么允许结算时重新绑定，但必须定义回写与重算策略

## Bottom Line

如果只看修复版主文档，方向已经接近可以实现。

但只要 Sprint plan 还是旧口径，当前状态就仍然不能放行实现。下一步最划算的做法不是立刻写代码，而是先做一轮“文档收敛”：

1. 以修复版主文档为准，回写 Sprint plan。
2. 修正 `settlement_records` migration 的列名。
3. 补完 QR 的 `sessionId` 生成策略。
4. 补完 coupon hold 并发语义。
5. 明确 settlement 阶段 `memberId` 的绑定规则。
