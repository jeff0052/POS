# Session 2.2 Milestone: 支付叠加引擎 (Payment Stacking)

**Branch:** `feature/session-2.2-payment-stacking`
**Commits:** 29 (16 feat + 13 post-merge review fixes)
**Files Changed:** 53 files, +5000 lines
**Tests:** 12 unit tests (PaymentStackingServiceTest 9, PaymentRetryServiceTest 3)
**Review Rounds:** 6 轮 code review，修复 ~15 个 P1 + ~3 个 P2
**Merge commits:** `c8c43cd` (initial merge), `2739883` (post-review fixes)

---

## Key Functions Delivered

### 支付叠加 4 阶段流

| 能力 | 实现 |
|------|------|
| **previewStacking** | 干跑计算：积分→券→储值→外部支付顺序扣减；返回每一层可用额度和可选券列表（含 `validFrom/validUntil` + 零折扣过滤，与 collect 行为一致） |
| **collectStacking** | 冻结 + 发起支付：`SELECT FOR UPDATE` on masterSession 防并发；按 stepOrder(1→4) 依次写 hold；CAS 冻结积分/储值；外部支付 > 0 时由 controller 在事务外调用 VibeCash |
| **confirmStacking** | 冻结→扣减：holds HELD→CONFIRMED，member_accounts 实际扣减 points/cash，coupon LOCKED→USED，settlement PENDING→SETTLED，调用 TableSettlementFinalizer 关闭 session/table |
| **releaseStacking** | 全部回退：holds HELD→RELEASED，解冻积分/储值，coupon LOCKED→AVAILABLE，settlement PENDING→CANCELLED；若存在 SUCCEEDED attempt 则拒绝释放（防止已收款结算被回退） |

### CouponLockingService

| 能力 | 实现 |
|------|------|
| **CAS 锁券** | `lockCoupon(couponId, expectedVersion, sessionId)` — 乐观锁 UPDATE `lock_version + 1`，失败即并发冲突 |
| **释放/确认** | `releaseCoupon` 恢复 AVAILABLE；`confirmCoupon` 标记 USED + usedAt/usedOrderId |
| **5 重校验链** | 归属(memberId) → 有效期(validFrom/validUntil) → 折扣计算(templateId, minSpendCents) → 零折扣拦截 → CAS 锁定 — 全部在 lockCoupon 之前完成 |

### PaymentRetryService

| 能力 | 实现 |
|------|------|
| **switchMethod** | CAS 把旧 FAILED attempt 标记 REPLACED → 创建新 PENDING_CUSTOMER attempt → controller 在事务外建链 |
| **方案白名单** | `VALID_EXTERNAL_SCHEMES` 在 CAS markReplaced 之前校验，避免非法 scheme 把旧 attempt 卡在 REPLACED |
| **重试追踪** | `parentAttemptId` + `retryCount` + `maxRetries` 形成完整重试链 |

### VibeCash TransactionTemplate 架构

| 能力 | 实现 |
|------|------|
| **DB/HTTP 隔离** | `startStackingPayment` 和 `createPaymentLinkForSavedAttempt` 移除 `@Transactional`，改用 `TransactionTemplate` 三阶段：Phase 1 (validate+persist→commit) → HTTP call → Phase 2 (update URL/mark FAILED→commit) |
| **markAttemptFailed** | 独立事务标记 FAILED，外层 catch 静默 log，不掩盖原始异常 |
| **恢复端点** | `GET /{settlementId}/active-attempt` 返回最近的非 REPLACED attempt，覆盖建链失败 + 响应丢失两种恢复场景 |

### Webhook 路由

| 能力 | 实现 |
|------|------|
| **双路径分流** | `settlementRecordId != null` → stacking 路径，`null` → legacy 路径 |
| **payment.succeeded** | attempt→SUCCEEDED → confirmStacking |
| **payment.failed** | attempt→FAILED，settlement 保持 PENDING（switchMethod 窗口） |
| **checkout.session.expired** | attempt→EXPIRED → releaseStacking |
| **REPLACED 静默** | 忽略已被 switchMethod 替换的 attempt 的 webhook |

### 定时任务

| 能力 | 实现 |
|------|------|
| **券锁超时回收** | 每分钟扫描 LOCKED > 10min 的券，按 session 分组：有 PENDING settlement → releaseStacking，孤儿 → releaseCoupon |
| **结算超时回收** | 每分钟扫描 PENDING > 30min 的 settlement：SUCCEEDED attempt → confirmStacking（补偿丢失 webhook），PENDING_CUSTOMER > 30min → releaseStacking |

### 安全

| 能力 | 实现 |
|------|------|
| **SecurityConfig 显式 matcher** | preview/collect/confirm/release → SETTLEMENT_STACKING，switch-method → PAYMENT_SWITCH，active-attempt → SETTLEMENT_STACKING；全部在 `/stores/**` permitAll 之前 |
| **storeId+tableId 作用域** | confirmStacking/releaseStacking/switchMethod 全部校验调用方传入的 storeId+tableId 与 settlement/attempt 实际归属一致 |

### DDL (V109–V113)

| Migration | 内容 |
|-----------|------|
| V109 | settlement_payment_holds: +table_session_id, +step_order, rename hold_amount→hold_amount_cents, +points_held, +coupon_id |
| V110 | member_coupons: +locked_by_session, +locked_at 索引 |
| V111 | settlement_records: +stacking detail fields (points/cash/coupon/external cents), cashier_id nullable |
| V112 | payment_attempts: +settlement_record_id, +retry chain (retry_count, parent_attempt_id, replaced_by_attempt_id) |
| V113 | settlement_records: +stacking_session_id + 索引 |

---

## 设计 Lessons

### L1: `@Transactional` 不能包 HTTP call

**问题：** 初版 `startStackingPayment` 和 `createPaymentLinkForSavedAttempt` 都是 `@Transactional`，在同一事务里做了 `saveAndFlush(attempt)` + `httpClient.send()` + `attempt.setUrl()`。VibeCash 超时 5 秒时，DB 连接被占 5 秒；VibeCash 500 时，attempt 跟着整个事务一起回滚——前端看不到失败记录。

**正确做法：** 注入 `PlatformTransactionManager`，用 `TransactionTemplate` 拆成三阶段：
- Phase 1：validate + persist attempt → commit（attempt 可观测）
- HTTP call：事务外
- Phase 2：persist URL / mark FAILED → commit

**原则：** 任何涉及外部系统调用的方法，DB 写和网络调用必须分属不同事务边界。`@Transactional` 放在包含 HTTP/RPC 的方法上一定是 bug。

---

### L2: 校验必须在不可逆操作之前

**问题：** `toGatewayScheme()` 是唯一的支付方案校验点，但它在 VibeCash service 里——远在 `collectStacking` 创建 settlement/holds 之后，远在 `switchMethod` 把旧 attempt 标记 REPLACED 之后。非法 scheme 会留下无法继续的半成品状态。

**正确做法：** 定义 `VALID_EXTERNAL_SCHEMES = Set.of("WECHAT_QR", "ALIPAY_QR", "PAYNOW_QR")`，在每个入口的第一行（任何 DB 写之前）校验。

**原则：** 校验（validation）必须在第一个不可逆副作用（DB write、CAS、外部调用）之前完成。如果校验逻辑在另一个 service 里，要么提前校验，要么把校验逻辑提取成静态方法或常量。

---

### L3: 幂等路径必须覆盖所有终态

**问题：** `collectStacking` 幂等分支查到 PENDING settlement 后，只在 attempt 列表里找 `PENDING_CUSTOMER` 的 URL。如果 webhook 已把 attempt 推到 `SUCCEEDED` 但 settlement 还没来得及 confirm，`existingUrl = null`，controller 再次调 `startStackingPayment()` 建了一条新 checkout link——同一笔结算双重收款。

**正确做法：** 幂等分支先检查 `SUCCEEDED` attempt，存在则 throw（让 scheduler 或显式 confirm-stacking 完成结算），不给 controller 建链的机会。

**原则：** 幂等返回路径不能只处理 happy path 状态，必须枚举所有可能的中间态（PENDING_CUSTOMER、SUCCEEDED、FAILED、EXPIRED）并做正确处理，否则会在竞态窗口里产生不一致。

---

### L4: recovery 接口的过滤条件要跟错误路径对齐

**问题：** `active-attempt` recovery 端点只返回 `PENDING_CUSTOMER` attempt。但建链失败时 `markAttemptFailed()` 把 replacement attempt 置为 `FAILED`，于是 recovery 返回空——旧 attempt 已 `REPLACED`，前端无法恢复。

**正确做法：** 过滤条件改为 `!= "REPLACED"`。只有 REPLACED 是"被取代"的含义，其他状态（FAILED、EXPIRED、PENDING_CUSTOMER、SUCCEEDED）都是前端需要看到的当前态。

**原则：** 设计 recovery/查询接口时，先列出所有可能的上游错误路径（正常、HTTP 失败、响应丢失、provider 拒绝），确认每条路径下的实体状态都能被查询到。过滤条件应该排除明确不需要的状态，而不是枚举允许的状态。

---

### L5: 券校验链的顺序决定了用户体验

**问题：** 初版 coupon block 的顺序是：加载 → CAS lock → 计算折扣 → 发现折扣 = 0 → 但券已经被锁了。第二版修了顺序但缺有效期校验。第三版补了 `validFrom/validUntil`。三轮才修完，而且每轮都是"加了新校验但位置不对"。

**最终顺序：** 加载 → memberId 归属 → validFrom/validUntil → calculateCouponDiscount → 零折扣拦截 → lockCoupon

**原则：** 涉及"先冻结再验证"的流程，必须把所有校验前置。校验链的排列应遵循"成本递增"原则：本地校验 → DB 读校验 → CAS/锁定操作，中间任何一步失败都不应产生需要清理的副作用。

---

### L6: preview 和 collect 的过滤逻辑必须镜像

**问题：** `previewStacking` 只按 `validUntil > now` 过滤券，但 `collectStacking` 还检查了 `validFrom`、minSpendCents、零折扣。前端显示"可选券 3 张"，用户选了一张，collect 立刻 400 拒绝。

**正确做法：** preview 的券过滤加注释 `// Mirror collectStacking validity window exactly`，使用完全相同的条件。

**原则：** 预览/预估和实际执行的业务规则必须来自同一段逻辑或保持显式镜像关系。如果两处逻辑分开维护，加 `// Must mirror XXX` 注释 + 在测试里验证一致性。

---

## 跨 Session 遗留问题

### P1 级

| 问题 | 影响 | 建议归属 |
|------|------|----------|
| **`startPayment` (legacy) 仍是 `@Transactional` 包 HTTP** | Session 2.2 只修了 stacking 路径的两个方法，legacy `startPayment()` (line 86) 仍然在一个事务里做 saveAndFlush + HTTP send + update URL。VibeCash 超时/500 时连接池和数据一致性风险同理存在。 | Session 6 legacy 清理或下次触碰 VibeCash service 时顺带修 |
| **Mockito ByteBuddy 阻塞完整测试** | Homebrew JDK 17 上 `./mvnw test` 全量跑会 hang（ByteBuddy inline agent attach 失败）。只能用 `-Dtest=ClassName` 单独跑。post-merge review 的测试验证不完整。 | 升级 Mockito 5.x（内置 inline mock）或在 surefire 加 `-javaagent` argLine |
| **`/api/v2/stores/**` broad permitAll** | 大量桌台、订单端点仍是 permitAll（POS tablet WebView 场景）。stacking 端点已加显式 matcher，但其他新增端点如果忘记加 matcher 都会直接变公开。 | Phase 6 全面收口 |

### P2 级

| 问题 | 影响 | 建议归属 |
|------|------|----------|
| **`settlement_records` 缺 `table_session_id` FK** | 退款 item 归属校验、settlement→session 追溯都是间接通过 `tableId` + 最近 session 推导。桌台复用后可能匹配错 session。 | 下次 settlement DDL 时补加 |
| **coupon template 缓存** | `calculateCouponDiscount` 每次都查 `couponTemplateRepo.findById(templateId)`。preview 和 collect 各调一次，高并发下对 DB 有压力。template 数据不常变，适合本地缓存。 | Phase 5 性能优化 |
| **scheduler 硬编码超时** | coupon lock 10min、settlement 30min 都是代码常量。不同商家可能需要不同超时。 | Phase 5 或抽成 store_settings |
| **`handleWebhook` 里的 confirmStacking/releaseStacking 跑在 webhook 事务里** | `handleWebhook` 是 `@Transactional`，内部调用的 `confirmStacking` 以 PROPAGATION_REQUIRED 加入外层事务。如果 confirmStacking 内部抛异常，webhook 处理也会回滚，attempt 状态更新丢失。 | 考虑把 webhook 的 attempt 更新和 stacking 操作拆到不同事务 |

---

## 文件清单

### 新增文件
```
pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/PaymentStackingService.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CouponLockingService.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/PaymentRetryService.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/TableSettlementFinalizer.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/application/dto/StackingPreviewDto.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/application/dto/StackingCollectResultDto.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/application/dto/PaymentRetryResultDto.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/infrastructure/persistence/entity/SettlementPaymentHoldEntity.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/infrastructure/persistence/entity/CouponTemplateEntity.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/infrastructure/persistence/repository/JpaSettlementPaymentHoldRepository.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/infrastructure/persistence/repository/JpaCouponTemplateRepository.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/PaymentStackingV2Controller.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/request/CollectStackingRequest.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/interfaces/rest/request/SwitchMethodRequest.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/application/scheduler/StackingMaintenanceScheduler.java
pos-backend/src/main/java/com/developer/pos/v2/member/infrastructure/persistence/entity/CouponTemplateEntity.java
pos-backend/src/main/java/com/developer/pos/v2/member/infrastructure/persistence/entity/MemberCouponEntity.java
pos-backend/src/main/java/com/developer/pos/v2/member/infrastructure/persistence/repository/JpaCouponTemplateRepository.java
pos-backend/src/main/resources/db/migration/v2/V109__settlement_payment_holds_stacking_fields.sql
pos-backend/src/main/resources/db/migration/v2/V110__member_coupons_locking.sql
pos-backend/src/main/resources/db/migration/v2/V111__settlement_records_stacking_details.sql
pos-backend/src/main/resources/db/migration/v2/V112__payment_attempts_retry_chain.sql
pos-backend/src/main/resources/db/migration/v2/V113__settlement_stacking_session_id.sql
pos-backend/src/test/java/com/developer/pos/v2/settlement/application/service/PaymentStackingServiceTest.java
pos-backend/src/test/java/com/developer/pos/v2/settlement/application/service/PaymentRetryServiceTest.java
pos-backend/src/test/java/com/developer/pos/v2/settlement/application/service/CouponLockingServiceTest.java
```

### 修改文件
```
pos-backend/src/main/java/com/developer/pos/auth/security/SecurityConfig.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/VibeCashPaymentApplicationService.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/CashierSettlementApplicationService.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/infrastructure/persistence/entity/SettlementRecordEntity.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/infrastructure/persistence/entity/PaymentAttemptEntity.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/infrastructure/persistence/repository/JpaPaymentAttemptRepository.java
pos-backend/src/main/java/com/developer/pos/v2/settlement/infrastructure/persistence/repository/JpaSettlementRecordRepository.java
pos-backend/src/main/java/com/developer/pos/v2/member/infrastructure/persistence/entity/MemberAccountEntity.java
pos-backend/src/main/java/com/developer/pos/v2/member/infrastructure/persistence/repository/JpaMemberAccountRepository.java
pos-backend/src/main/java/com/developer/pos/v2/member/infrastructure/persistence/repository/JpaMemberCouponRepository.java
pos-backend/src/main/java/com/developer/pos/v2/common/application/StoreAccessEnforcer.java
```
