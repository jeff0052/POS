# Session 2.3 Milestone: 退款 + 预约→入座

**Branch:** `session-2.3-refund-reservation`
**Commits:** 18 (7 feat + 8 fix + 1 docs + 1 revert + 1 accidental merge)
**Files Changed:** 36 files, +1369 / -682 lines
**Tests:** 108 pass, 0 failures (including 16 new tests for this session)
**Review Rounds:** 8 轮 code review，修复 ~20 个 P1 + ~6 个 P2

---

## Key Functions Delivered

### 退款引擎

| 能力 | 实现 |
|------|------|
| **Item-level 退款** | `refund_line_items` 表 + `RefundLineItemEntity`；客户端传入 per-item `amountCents`，service 校验 sum 匹配总额，校验 itemId 归属 settlement 的 table_session |
| **审批状态机** | PENDING_APPROVAL → APPROVED / REJECTED → COMPLETED / AWAITING_EXTERNAL_REFUND；阈值从 RBAC `maxRefundCents` 贯通（`PermissionResolver` → `ResolvedPermissions` → `JwtAuthFilter` → `AuthenticatedActor.maxRefundCents()`） |
| **积分/储值实际回退** | 查 `settlement_payment_holds(CONFIRMED)` 找 memberId → 更新 `member_accounts.available_points` / `available_cash_cents`；按比例计算 points-cents → points-count 转换 |
| **券恢复** | 全额退款将 `member_coupons.coupon_status` 从 USED 恢复为 AVAILABLE，清除 usedAt/usedOrderId |
| **外部退款异步闭环** | `RefundApplicationService.completeExternalRefund()` + `PaymentCallbackController.handleRefundCallback()`，HMAC 签名校验，settlement 分段记账（内部立即、外部 webhook 后补记） |
| **累计精度** | 多次 partial refund 的积分/储值回退用 remaining cap 防漂移；内部/外部分摊用累计差值法（一次 round per cumulative total） |
| **悲观锁** | `findByRefundNoForUpdate` (PESSIMISTIC_WRITE) 防并发双重审批 |
| **身份不可伪造** | `operatedBy` / `approvedBy` 从 `AuthContext.current().userId()` 取，不接受客户端传入 |
| **租户隔离** | 所有退款 CRUD 路径加 `StoreAccessEnforcer.enforce(storeId)`，含空结果场景 |

### 预约→入座

| 能力 | 实现 |
|------|------|
| **创建预约** | 支持 `contactPhone`；CONFIRMED 状态自动分配 AVAILABLE 桌并锁为 RESERVED，无可用桌则 throw |
| **状态机守卫** | `create()` 白名单 PENDING / CONFIRMED；`update()` 合法转换 PENDING→{CONFIRMED,CANCELLED}，CONFIRMED→{PENDING,CANCELLED}；CHECKED_IN 只能通过 `seat()`，NO_SHOW 只能通过 scheduler |
| **预约→入座** | `seat()` 创建 `TableSessionEntity(OPEN)` 匹配主链路约定，桌台设为 OCCUPIED；支持使用预分配的 RESERVED 桌或手动指定桌；换桌时释放旧 RESERVED 桌 |
| **过期自动取消** | `ReservationExpiryScheduler` @Scheduled(5min)，超时 30min 标记 NO_SHOW（区分主动取消），释放 RESERVED 桌 |
| **改约释放桌** | `update()` 离开 CONFIRMED 时释放 RESERVED 桌，离开 CHECKED_IN 时释放 OCCUPIED 桌 |
| **租户隔离** | 所有预约路径加 `StoreAccessEnforcer.enforce(storeId)` |

### 安全加固

| 能力 | 实现 |
|------|------|
| **退款权限** | SecurityConfig: POST /refunds → REFUND_SMALL\|REFUND_LARGE, POST /refunds/\*/approve → REFUND_LARGE |
| **预约权限** | SecurityConfig: GET → RESERVATION_VIEW\|RESERVATION_MANAGE, POST/PUT/seat → RESERVATION_MANAGE |
| **active-attempt 保护** | GET /stores/\*/tables/\*/payment/\*/active-attempt → SETTLEMENT_COLLECT |
| **internal callback** | payment-callback 和 refund-callback 统一 secure-by-default：secret 未配 → 503，已配 → 无条件 HMAC 校验 |

---

## Lessons Learned

### 1. 安全属性不能靠 plan 阶段假设，必须在实现时逐行验证

初版实现信任了 `operatedBy`、`approvedBy`、`maxRefundCents`、`reservationStatus` 全部来自客户端。plan 写的是"从 AuthContext 取"，但 subagent 执行时图省事直接用 request body。**教训：安全相关字段必须在 code review 时逐个检查 data flow，从 request 到 service 到 entity，不能信任 plan 的意图描述。**

### 2. 状态机的完整性不能只看 happy path

初版的 `seat()` 写了 `ACTIVE` 而非 `OPEN`，因为 plan 只描述了"创建 TableSession"但没检查下游消费者用什么 status 查询。预约 status 也完全由客户端控制，可以直接跳到 `CHECKED_IN` 绕过 `seat()` 流程。**教训：每个状态值都要 grep 下游消费者确认一致性；状态机要显式声明合法转换，不能靠 "客户端应该传对" 的隐式信任。**

### 3. "记录应该回退多少" ≠ "实际回退了"

前几轮只在 refund record 上记录了 `pointsReversedCents` 等字段但没真正更新 `member_accounts`。这个 gap 在 plan 阶段被标注为 "Phase 5 处理"，但验收标准明确写了 "退款后积分/储值/券正确回退"。**教训：如果验收标准说 "X 正确发生"，代码必须真正执行 X，不能只记录意图然后标 COMPLETED。**

### 4. 外部支付退款需要异步闭环，不能在同步流里标 COMPLETED

结算包含外部支付时，退款记录不能立即标 COMPLETED——外部退款可能失败。内部资产（积分/储值/券）可以立即回退，但 settlement 账务只能记内部部分，外部部分等 webhook 确认后补记。**教训：涉及外部系统的状态变更必须设计异步确认路径（callback endpoint + 状态机），不能假设同步操作一定成功。**

### 5. 多次 partial refund 的比例计算必须用累计差值法

独立对每次 refund 做 `Math.round(total × ratio)` 会在多次 partial refund 后累计漂移。正确做法是 `round(total × cumWithThis / collected) - round(total × cumBefore / collected)`——每次只对累计总额做一次 round，差值精确。**教训：任何涉及"按比例分摊 + 多次操作"的场景都必须用累计差值法，不能独立 round。**

### 6. `permitAll` 端点里的 HMAC 校验必须 secure-by-default

`/api/v2/internal/**` 是 permitAll 的——这意味着 controller 自己就是安全边界。如果 HMAC secret 为空时默认放行，任何没配 secret 的环境都裸奔。**教训：internal callback 的签名校验必须是"secret 为空 → 拒绝"，不能是"secret 为空 → 跳过校验"。**

---

## 跨 Session 遗留问题

### P1 级（必须在后续 session 解决）

| 问题 | 影响 | 建议归属 |
|------|------|----------|
| **外部支付退款 provider 调用未实现** | `completeExternalRefund` 的 webhook 入口已建好，但 pos-backend → pos-payment-service 的"发起退款"请求还没实现。当前只能等 payment service 主动回调，不能主动触发退款。 | Session 6.2 或 pos-payment-service 独立 session |
| **`refundItems` ownership 对普通 settlement 只是 best-effort** | `stackingSessionId == null` 时退化为"同桌最近 session"，桌台复用后可能匹配到错误 session 的 items。根本修复需要在 `settlement_records` 上加 `table_session_id` FK。 | Session 6.1 联调时加 DDL |
| **预约提醒未实现** | spec 要求"预约提醒：临近时间通知"，当前只有过期 NO_SHOW，没有提前提醒。 | Session 5.2 (渠道通知) 或独立小 session |

### P2 级（建议在后续 session 顺带解决）

| 问题 | 影响 | 建议归属 |
|------|------|----------|
| **`settlement_records` 缺少 `table_session_id` 字段** | 退款 item 校验、退款归因、报表关联都依赖 settlement → session 的直接引用，当前只能通过 `tableId` 间接推导。 | 下次 settlement 相关 DDL 时补加 |
| **退款 item 金额仍是客户端传入** | service 校验 sum 和 itemId 归属，但不校验 per-item 金额是否匹配实际 `unit_price_snapshot_cents × quantity`。需要 `JpaSubmittedOrderItemRepository` 按 ID 加载 item 做价格校验。 | Session 6.1 联调 |
| **`/api/v2/stores/**` broad permitAll** | 大量桌台、订单端点仍然是 permitAll（POS tablet WebView 场景），长期需要收口为 device token 认证。 | Phase 6 |
