# FounderPOS V3 Final Executable Spec

第三轮架构审计结论

- 审计对象: `docs/superpowers/specs/2026-03-28-final-executable-spec.md`
- 审计日期: `2026-03-28`
- 审计范围: final spec 与当前 `pos-backend` 实际 schema / service 契约对齐性复核
- 结论: `PARTIALLY APPROVED`

审批边界:

- `S1-S2` 方向已达到可开工标准。
- `S3` 仍有 1 个金融规则 blocker。
- `S4-S6` 仍不满足“single source of truth / executable spec”标准。

这轮和前两轮不同。并台、QR 空桌扫码、buffet 字段落 `submitted_order_items`、`audit_trail` 拆表、`memberId` 不换绑、`payment_attempts` 状态机回归真实实现，这些核心 blocker 基本都已经修正。剩下的问题集中在“文档是否真的自洽可执行”和“会员权益在混单场景下怎么落地”。

---

## Findings

### 1. `[P1]` Final spec 仍然不是完整自包含文档，`V086-V095` 没有可执行 DDL 来源

**证据**

- final spec 声明自己是唯一实现真相:
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:638`
- 但 `V086-V095` 只给了文件名列表，并明确要求“参考原 spec”:
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:356`
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:371`
- 被宣告废弃的旧文档之间，对这些 migration 的编号和内容并不一致:
  - `docs/superpowers/specs/2026-03-28-design-v2-post-review.md:739`
  - `docs/superpowers/specs/2026-03-28-design-v2-post-review.md:748`
  - `docs/superpowers/specs/2026-03-28-sprint-plan-complete.md:1373`
  - `docs/superpowers/specs/2026-03-28-sprint-plan-complete.md:2093`
  - `docs/superpowers/specs/2026-03-28-sprint-plan-complete.md:2102`

**为什么这是问题**

final spec 现在对 `V070-V085` 是可执行的，因为 DDL 已经完整写进文档；但 `V086-V095` 只剩摘要，真正要建什么表、字段、索引、约束，开发仍然要回头翻旧文档。问题在于旧文档并不是同一口径:

- `design-v2-post-review` 只给 summary，没有完整 DDL。
- `sprint-plan-complete` 虽然有部分 DDL，但编号体系不同，还保留了已经被 final spec 否掉的旧阶段拆分。

这意味着 final spec 对 `S4-S6` 还不能被称为“可执行 spec”。Claude 如果按这版直接写 `V086-V095`，仍然会遇到“该信哪份旧文档”的问题。

**必须修改**

- 把 `V086-V095` 的完整 DDL 直接补进 final spec，至少达到 `V070-V085` 同等级别的自包含程度。
- 或者在 final spec 里明确指定唯一引用源，并保证该引用源的编号、sprint 边界、表结构定义与 final spec 完全一致。
- 在文档头部把审批边界写清楚:
  - 如果当前只打算放行 `S1-S3`，就不要再宣称整份 `V070-V095` 都是 executable。

**验收标准**

- 不查看任何已废弃文档，工程师也能仅凭 final spec 写出 `V086-V095`。
- `V086-V095` 的 migration 编号、sprint 归属、DDL 内容在单一文档中唯一确定。

---

### 2. `[P1]` 结算叠加仍未定义“同桌/并台链路出现多个 memberId”时的明确处理规则

**证据**

- final spec 规定结算会员来自 `submitted_orders.member_id`，并要求权益必须属于同一个 member:
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:399`
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:400`
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:560`
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:563`
- final spec 还要求并台时按 `sessionChain` 聚合多个 `submitted_orders` 一起结算:
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:384`
  - `docs/superpowers/specs/2026-03-28-final-executable-spec.md:390`
- 当前实现拿会员的方式仍然是“取第一个非空 memberId”:
  - `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java:107`
  - `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java:115`
- `submitted_orders.member_id` 本来就是逐单写入的，不保证同桌同 session 一定唯一:
  - `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java:425`
  - `pos-backend/src/main/java/com/developer/pos/v2/order/application/service/ActiveTableOrderApplicationService.java:437`

**为什么这是问题**

文档现在只写了“必须属于同一个 member”，但没有定义异常分支:

- 如果同一桌未结订单里同时出现 `memberA` 和 `memberB`，`preview-stacking` 应该报错、降级成纯外部支付，还是要求先拆单？
- 并台后两个 session 各自带不同 member 时，是否禁止叠加权益？
- coupon / points / cash balance 的 owner 校验，是按“所有 unpaid orders 必须同 member”，还是“只要存在冲突就禁用会员权益”？

没有这条规则，Claude 很容易沿用当前 `findFirst()` 的行为，把第一个会员当成本次结算会员。那样会把 A 会员的积分/券/储值错误用于 A+B 的混合账单，属于财务和权益归属错误。

**必须修改**

- 在 `D2` / `D10` 下补一条硬规则:
  - 先对 `sessionChain` 内所有 `UNPAID submitted_orders.member_id` 做去重。
  - 去重结果为 `0` 个: 只允许外部支付。
  - 去重结果为 `1` 个: 允许会员权益叠加。
  - 去重结果 `> 1` 个: 明确失败策略。
- 推荐失败策略:
  - `preview-stacking` 返回 `memberBenefitEligible=false` + `reason=MULTIPLE_MEMBERS_IN_SESSION_CHAIN`
  - `collect-stacking` 若仍请求 points/coupon/cash，则直接拒绝
  - 只允许纯外部支付，或要求先拆台/拆单后再结算

**验收标准**

- final spec 明确写出多 member 场景的判定算法和 API 行为。
- 实现后不允许再出现“默认取第一个 memberId”这种隐式策略。

---

## 本轮确认已修复的前序 blocker

- 并台模型已收敛为“纯指针 + 结算聚合”，不再要求搬 `order_items`。
- `settlement_records` 已改回真实字段 `payable_amount_cents` / `collected_amount_cents`。
- `table_sessions.session_status` 已回归真实值 `OPEN/CLOSED`。
- 动态 QR 已明确 `sessionId` 可为 `null`，并由后端 `findOrCreateOpenSession` 懒创建。
- buffet 价格字段已要求同时落到 `active_table_order_items` 和 `submitted_order_items`。
- `action_log` 与人工审计已拆分为 `action_log` / `audit_trail` 两表。
- 结算 API 已禁止传入新的 `memberId`。
- coupon 并发防重已升级为 `LOCKED + lock_version` 的 CAS 方案。
- `payment_attempts` 状态机已与当前实现重新对齐。

---

## 最终建议

当前文档已经足够支撑 `S1-S2` 开工，`S3` 也只差最后一条会员归属规则就能落地。真正还没达到“最终可执行”标准的是 `S4-S6` 的自包含性。

建议按下面顺序收口:

1. 先补 `D10` 的 mixed-member 规则，放行 `S3`。
2. 再把 `V086-V095` 的完整 DDL 回填到 final spec，或者拆成新的、明确未废弃的附录文档。
3. 文档标题里的 `FINAL — 可开工版本` 可以保留，但建议改成更精确的表述:
   - `S1-S3 可开工，S4-S6 待补全 DDL`
   - 或者补齐后再恢复“全量可执行”结论。
