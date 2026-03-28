# FounderPOS V3 User Journeys

审计文档

- 审计对象: `docs/superpowers/specs/2026-03-28-user-journeys.md`
- 审计日期: `2026-03-28`
- 对照基线: `docs/superpowers/specs/2026-03-28-final-executable-spec.md` + 当前 `pos-backend` schema / entity / service
- 结论: `NOT APPROVED FOR MIGRATION DERIVATION`

这份 journey 文档的覆盖面明显比前面的设计稿健康得多。J01/J02/J04/J05/J06/J08 基本已经具备“能反推接口和验收”的骨架，尤其是每条都带了 alternative flow，这个方向是对的。

这轮没有大面积结构性冲突，但仍有 3 个 blocker 会直接影响“先写 journey，再反推 migration”的可执行性。

---

## Findings

### 1. `[P1]` J11 把“并台多会员时取第一个 memberId”重新写回来了，和 final spec 正面冲突

**证据**

- final spec 已经明确要求:
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:560`
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:562`
- J11 现在却写成:
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:651`
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:654`
- 当前代码确实还是“取第一个非空 memberId”:
  - `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java:107`
  - `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java:115`

**为什么挡实现**

这不是一个描述风格问题，而是会直接把错误业务规则固化进代码。并台后如果 `sessionChain` 里混入两个不同会员，当前 journey 的写法会把整单的积分、储值、券归到第一个会员名下，属于权益归属错误和财务错误。

**必须修改**

- 删掉 “取第一个非 null 的 member_id”。
- J11 要回归 final spec 的口径:
  - 先对 `sessionChain` 内所有 `UNPAID submitted_orders.member_id` 去重。
  - 去重结果 `0` 个: 只允许外部支付。
  - 去重结果 `1` 个: 允许会员权益。
  - 去重结果 `>1` 个: 明确失败策略，推荐禁用 points/coupon/cash，仅允许纯外部支付或要求先拆台/拆单。

---

### 2. `[P1]` J03 外卖 journey 引入了主 spec 之外的 schema 字段和表，当前不能直接反推 migration

**证据**

- J03 直接使用了这些承载:
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:160`
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:161`
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:188`
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:199`
- 但当前 `submitted_orders` 的真实 DDL / entity 只有 `source_order_type`，没有 `dining_mode / external_platform / external_order_no / delivery_status`:
  - `pos-backend/src/main/resources/db/migration/v2/V012__table_session_and_submitted_orders.sql:19`
  - `pos-backend/src/main/resources/db/migration/v2/V012__table_session_and_submitted_orders.sql:26`
  - `pos-backend/src/main/java/com/developer/pos/v2/order/infrastructure/persistence/entity/SubmittedOrderEntity.java:43`
  - `pos-backend/src/main/java/com/developer/pos/v2/order/infrastructure/persistence/entity/SubmittedOrderEntity.java:70`
- final executable spec 也没有把这些 delivery/channel 扩展纳入 `V070-V095`:
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:32`
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:356`
- 这些字段目前只出现在旧的完整 schema 文档里:
  - `docs/75-complete-database-schema.md:3016`
  - `docs/75-complete-database-schema.md:3025`

**为什么挡实现**

J03 现在看起来像一条完整 journey，但它实际依赖了一组不在 final spec 里的 delivery/channel schema。结果是开发如果按“journey -> migration”落地，会立刻遇到几个悬空问题:

- `submitted_orders` 是否真的要扩展 delivery 字段？
- 这些字段是挂在 `submitted_orders`，还是强制走单独的 `delivery_orders`？
- `order_channel_attribution` 属于这轮 migration，还是已有基础设施？

在这几个问题没被写回 final spec 之前，J03 还不能当 migration 输入。

**必须修改**

- 二选一:
  - 把外卖承载模型正式收敛进 final spec，明确字段和归属表。
  - 或者把 J03 标成 “依赖 legacy delivery/channel schema，当前不参与 V070-V095 推导”。
- `DDL Impact` 不能再写成模糊的 “现有 delivery_orders/submitted_orders”，要明确哪张表负责哪段状态。

---

### 3. `[P1]` J12 把 `queue_tickets` 当成现有能力，但当前 repo 和 final spec 都没有可执行来源

**证据**

- J12 当前写法:
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:713`
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:750`
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:752`
- 当前 repo 里 `reservations` 确实存在:
  - `pos-backend/src/main/resources/db/migration/v2/V018__reservations.sql:1`
- 但 `queue_tickets` 只存在于旧文档，不在当前 Flyway / entity 中:
  - `docs/66-aurora-data-model-design.md:595`
  - `docs/75-complete-database-schema.md:2287`
- Journey 交叉矩阵还把 J12 只标到了 `V071`:
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:777`

**为什么挡实现**

J12 现在把 “预约” 和 “候位叫号” 串成一条端到端 journey 是对的，但它把 `queue_tickets` 说成“已有”，而当前仓库里并没有这部分可执行迁移或实体。这样后续如果直接按 journey 推 migration，会误判为“J12 无需新增建模”，但实际上候位这半段根本没有落库基础。

**必须修改**

- 明确 J12 的边界:
  - 如果本轮只覆盖 `reservations -> seat`，那就把候位/叫号拆成后续 journey。
  - 如果候位必须本轮实现，就要把 `queue_tickets` 正式纳入 final spec 和 migration 规划。
- `Journey -> Migration` 矩阵要反映真实依赖，不能让 J12 看起来只依赖 `V071`。

---

## 建议放行边界

如果你现在是要用这份文档指导下一轮 migration/代码，我建议按下面方式放行:

- 可以继续推进:
  - J01 单点堂食
  - J02 自助餐
  - J04 会员
  - J05 收银员
  - J06 厨房
  - J08 库存
- 需要先修正文档再推进:
  - J11 并台
  - J03 外卖
  - J12 预约/候位

J07/J09/J10 更像管理和经营侧流程，文档表达已经有价值，但它们依赖的部分数据对象仍然分散在历史 schema 文档里，不适合作为“下一步先生成 Flyway”的直接输入。

---

## 结论

这份 journey 文档已经足够证明 “先写 journey” 这条路是对的，但还没达到“可以直接作为全量 migration 推导基线”的程度。真正的缺口已经缩小到 3 个非常具体的问题，不再是大面积重写。

先修 J11 的会员规则，再把 J03/J12 的 schema 归属写回 final spec，下一轮我会更倾向于给出 `APPROVED FOR DERIVATION`。
