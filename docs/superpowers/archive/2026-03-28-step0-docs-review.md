# FounderPOS V3 Step 0 Docs Review

- 审计对象:
  - `docs/superpowers/specs/2026-03-28-user-journeys.md`
  - `docs/superpowers/specs/2026-03-28-state-machines-and-constraints.md`
  - `docs/superpowers/specs/2026-03-28-figma-diagrams.md`
- 对照基线:
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md`
  - 当前 `pos-backend` Flyway / entity / service
- 审计日期: `2026-03-28`
- 结论: `NOT APPROVED AS NEXT SINGLE SOURCE`

这轮文档工作是有明显进步的。`journey -> state machine -> module boundary -> diagram` 这个链条终于成形了，J01/J02/J05/J06/J08 的可执行感也比上一轮强很多。

问题不在“没写”，而在于几份文档里又开始长出新的口径，导致它们不能直接取代 final spec 继续向下推 migration / code。

---

## Findings

### 1. `[P1]` 并台和 buffet 状态机重新偏离 final spec，已经不是同一套实现口径

**证据**

- final spec 的并台模型是纯指针聚合:
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:381`
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:386`
- final spec 的会员规则是“必须同一个 member”:
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:560`
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:562`
- 但 state machine 文档又写成:
  - `docs/superpowers/specs/2026-03-28-state-machines-and-constraints.md:56`
  - `docs/superpowers/specs/2026-03-28-state-machines-and-constraints.md:60`
- user journey 里 J11 仍然保留了“取第一个非 null member_id”:
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:686`
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:687`

**为什么挡实现**

这里已经不是单个 wording 问题，而是三份文档在三个点上互相打架:

- final spec: 并台时只打 `merged_into_session_id` 指针，结账时聚合 `sessionChain`
- state machine: 并台时源桌 session 直接 `CLOSED`
- state machine: `dining_mode` 还扩成了 `MIXED`
- J11: 多会员并台又退回“取第一个 member”

Claude 如果按这三份文档任何一份继续写代码，都会把我们前面已经收口过的并台设计重新写散。

**必须修改**

- 并台相关文档全部回到 final spec 口径:
  - 不关闭被并桌 session
  - 不引入 `dining_mode = MIXED`
  - 多会员并台不允许“取第一个 member”
- J11 / SM02 / 状态机图必须一起改，不然图和文档会再次分叉。

---

### 2. `[P1]` J04 V2 新增了 final spec 之外的会员注销范围，已经长出 `V096`

**证据**

- final spec 当前 migration 范围只到 `V095`:
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:32`
- final spec 对 coupon 状态的定义只有:
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:260`
- J04 V2 现在新增:
  - 注销账户 / 余额退款 / 软删除 / 券作废 / 重新注册
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:237`
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:244`
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:283`
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:299`

**为什么挡实现**

J04 V2 本身不是坏方向，但它已经不再是“对现有 final spec 的行为展开”，而是在引入一组新的需求:

- `coupon_status = CANCELLED`
- `members.member_status = DEACTIVATED`
- `member_accounts` 软删除
- `refund-balance` API
- 新 migration `V096`

这组需求目前既没有写回 final spec，也没有纳入现有迁移编号计划。继续往下推，只会让单一真相再次裂成 “journey truth” 和 “final spec truth”。

**必须修改**

- 二选一:
  - 把 J04 V2 缩回当前 `V070-V095` 范围内
  - 或者正式扩版 final spec，把注销能力、状态集、DDL、编号计划一起纳入
- 在没扩版前，不要在 `DDL Impact` 写 `V096`。

---

### 3. `[P1]` J12 V2 仍然依赖 doc-only schema，而且这次又给 `reservations` 加了一个仓库里没有的字段

**证据**

- J12 V2 现在要求“创建即锁桌”:
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:737`
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:743`
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:799`
  - `docs/superpowers/specs/2026-03-28-user-journeys.md:814`
- 当前 `reservations` 真实 migration 只有这些字段:
  - `pos-backend/src/main/resources/db/migration/v2/V018__reservations.sql:1`
  - `pos-backend/src/main/resources/db/migration/v2/V018__reservations.sql:16`
- `queue_tickets` 仍然只在旧文档里有:
  - `docs/66-aurora-data-model-design.md:595`

**为什么挡实现**

J12 V2 修了“预约未锁桌”的业务问题，这个方向是对的；但实现承载仍然没被正式收进去:

- `reservations.reserved_table_id` 当前不存在
- `queue_tickets` 当前仍然没有 repo 内的可执行 migration / entity
- 交叉引用矩阵和最终 spec 也没有把这条链路正式纳入

也就是说，J12 现在是“业务更合理了”，但仍然不是“可直接推 DDL”。

**必须修改**

- 先明确 J12 是否进入当前实现范围。
- 如果进入:
  - 把 `reserved_table_id` 和 `queue_tickets` 正式纳入 final spec / migration 计划
- 如果不进入:
  - 保留 journey 作为 future scope，但不能当本轮 migration 输入

---

### 4. `[P2]` state machine / constraints 文档又发明了当前 schema 没有的字段与约束，图表会把错误口径固化

**证据**

- state machine 文档写了:
  - `audit_trail.actor_type`
  - `docs/superpowers/specs/2026-03-28-state-machines-and-constraints.md:368`
  - `stores.timezone`
  - `docs/superpowers/specs/2026-03-28-state-machines-and-constraints.md:385`
  - DB `CHECK` 约束
  - `docs/superpowers/specs/2026-03-28-state-machines-and-constraints.md:347`
- 但当前真实 DDL 中:
  - `audit_trail` 没有 `actor_type`
    - `pos-backend/src/main/resources/db/migration/v2/V073__create_audit_trail.sql:2`
    - `pos-backend/src/main/resources/db/migration/v2/V073__create_audit_trail.sql:28`
  - `stores` 没有 `timezone`
    - `pos-backend/src/main/resources/db/migration/v2/V002__store_and_table.sql:1`
    - `pos-backend/src/main/resources/db/migration/v2/V002__store_and_table.sql:16`

**为什么这是问题**

图表本身不会执行代码，但它们会被当成下游实现和讨论的“视觉权威”。如果状态机图和约束文档里混入了当前 schema 没有的字段，后面再 claim Figma 图，团队就会默认这些字段已经被批准。

**必须修改**

- 所有状态机/约束文档只写“已在 final spec 里批准”的字段和约束。
- 如果要补 `actor_type` / `timezone` / `CHECK`，先扩 final spec，再更新文字和图。

---

## 图表索引说明

`docs/superpowers/specs/2026-03-28-figma-diagrams.md` 本身没有新 blocker；它的问题是“准确反映了当前仍有漂移的文档内容”。在上述 4 个问题修完之前，这批图可以继续保留，但不应被当作最终实现基线。

---

## 建议放行边界

当前可以保留继续深化的材料:

- J01 / J02 / J05 / J06 / J08 的 journey 骨架
- 大部分跨模块边界与并发控制思路
- Figma 图表索引作为工作清单

当前不建议直接拿去推 migration / code 的部分:

- J11 并台
- J04 V2 注销扩展
- J12 V2 预约锁桌 + 候位
- 状态机文档里新增的 `MIXED / actor_type / timezone / V096`

---

## 最终结论

这三份文档说明方向已经对了，但还没重新收敛成一个新的单一真相。下一步最值当的不是继续加图，而是先把 “final spec / journey / state machine” 三者重新对齐一次。
