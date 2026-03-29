# FounderPOS V3 — Implementation Plan & Roadmap

**Version:** V20260329001
**Date:** 2026-03-29
**Status:** IN PROGRESS
**受众：** Jeff + Claude 协作写代码的执行清单
**前置文档：** `docs/83-system-architecture-v3.md`（架构总纲）

---

## 源 of Truth 优先级

```
1. docs/75-complete-database-schema.md          ← 120 表原始 DDL
   + V066/V067 (追赶) + V070-V101 (gap 修复)   ← 增量 DDL
2. docs/superpowers/specs/final-executable-spec.md  ← 设计决策 D1-D10
3. docs/85-api-contract.md                      ← API 响应格式 + 端点契约
4. docs/86-rbac-seed-data.md                    ← 权限种子数据（Session 1.1 必读）
5. docs/83-system-architecture-v3.md            ← 5 大系统 + 128 表归属
6. docs/80-step-0.3-data-model-gaps.md          ← 9 新表 + 8 ALTER 设计说明
```

**最终数字（已验证 2026-03-29）：**
- 物理表：128（不含 flyway_schema_history）
- Migration 文件：20 个（V066-V101，V099 已删除）+ V102-V108（Phase 1-3 增量）
- 新表：9 张，ALTER：8 项

---

# Part 1: Roadmap

```
Phase 0 (完成)  ── 设计收尾
  ✅ 0.1-0.4 Journey + 状态机 + Gap + DDL Review
  ✅ 0.5 生成 Flyway migration SQL（20 文件）
  ✅ 0.6 验证 migration 可执行（128 表通过）
  ✅ 0.7 Code Review 修复（4 项）

Phase 1: 能登录、能审计 (Week 1)
  ✅ Session 1.1  RBAC + 统一用户 + Legacy 迁移
     → 41 文件, 2806 行, 5 轮安全审查全部通过
  ✅ Session 1.2  审计切面 + audit_trail
     → 11 文件, 700 行, 4 轮安全审查全部通过

Phase 2: 交易核心 (Week 2)
  ✅ Session 2.1  订单引擎重构（并台、状态机、sessionChain、QR、buffet 字段透传）
     → 48 文件, ~2170 行, 4 轮安全审查
  ⬜ Session 2.2  结算引擎（支付叠加、幂等、冻结/确认/释放）
  ⬜ Session 2.3  退款 + 预约→入座

Phase 3: 商品升级 (Week 3)
  ✅ Session 3.1  SKU + 修饰符 + 菜单时段
     → 38 文件, 8 轮 review, 权限/价格链/CDN 图片全收口
  ✅ Session 3.2  自助餐引擎
     → 38 文件, 2868 行, 31 个单元测试, final review APPROVED

Phase 4: 厨房 + 库存 (Week 3-4)
  ⬜ Session 4.1  KDS + 厨房票
  ⬜ Session 4.2  库存 + 采购 + 盘点

Phase 5: 客户营销 (Week 4)
  ⬜ Session 5.1  会员 + 积分 + 储值
  ⬜ Session 5.2  优惠券 + 促销 + 渠道

Phase 6: 联调收尾 (Week 5-6)
  ⬜ Session 6.1  外卖对接 + AI 层
  ⬜ Session 6.2  部署 + 前端联调
```

---

# Part 2: Implementation Plan（按 Session）

## Phase 0: 设计收尾

### Session 0.5 — 生成 Flyway Migration SQL ✅

**实际产出：**
- V066: 74 个 CREATE TABLE（docs 66-73 基础表追赶）
- V067: 现有表 ALTER（buffet/CRM/channel 字段）
- V070-V101: Step 0.3 gap 修复（9 新表 + ALTER）
- V099 已删除（列名冲突，等 Phase 1 重写时处理）
- 总计 20 个 migration 文件

---

### Session 0.6 — 验证 Migration 可执行 ✅

**验证结果：**
- DROP + 重建 pos_v2_db，从 V001 跑全量
- 128 张物理表全部创建成功（不含 flyway_schema_history）
- 9 张新表验证通过
- 10 个 ALTER 字段抽样验证全部通过
- 修复项：JSON DEFAULT 语法、FK 名冲突、COMMENT 语法、member_accounts 重复列

---

### Session 0.7 — Code Review 修复 ✅

**Agent Review 发现 4 项：**
- P0: V099 列名改动与 Java 实体冲突 → 删除 V099
- P1: QR 方案双轨 → 统一为 qr_tokens 表方案
- P1: API 契约与现有 ApiResponse 不一致 → docs/85 改为 code/message/data 格式
- P2: Handoff 文档数字不一致 → 已更新

---

## Phase 1: 能登录、能审计（Week 1）

### Session 1.1 — RBAC + 统一用户 + Legacy 迁移 ✅

**完成日期：** 2026-03-29

**实际产出（41 文件，2806 行）：**
- `v2/rbac/` 模块：6 Entity + 6 Repository + 4 Service + 1 Controller + 8 DTO + 7 Request
- `auth/` 模块改造：AuthenticatedActor 升级、JwtAuthFilter 加 RBAC 权限解析、SecurityConfig 权限化
- V102: 59 权限 + 8 预置角色 + 213 条角色权限映射（幂等 SQL，使用临时表）
- V103: auth_users/staff → users 迁移（user_code='AU-<id>' 映射键）
- JwtProvider: 新增 6 参数 generateToken（含 userCode）
- PermissionCacheService: Caffeine 缓存，5 分钟 TTL
- PIN 登录: /api/v2/auth/pin-login（含门店权限校验）
- RBAC 管理 API: 13 个端点（/api/v2/rbac/**）

**安全审查（6 轮，全部通过）：**
- Round 2: PIN 门店校验、锁定绕过、租户隔离、V103 碰撞、缓存驱逐
- Round 3: 平台路由收紧、DISABLED/LOCKED 异常类型、V103 OWNER/KITCHEN 映射、深层租户检查
- Round 4: createUser 校验补全、预留 roleCode 拦截、密码错误不回退 legacy
- Round 5-6: Legacy JWT id 映射修复（AU-<id> 确定性映射键）

**验收：**
- 288 文件编译零错误（Java 17, Spring Boot 3.3.3）
- V102/V103 在本地 MySQL 8.4 执行通过
- POST /api/v2/auth/login 返回 JWT
- JWT 带 permissions 列表
- 旧 auth_users 数据迁移到 users 无丢失

---

### Session 1.2 — 审计切面 + audit_trail ✅

**完成日期：** 2026-03-29

**实际产出（11 文件，700 行）：**
- `v2/audit/` 模块：@Audited 注解 + AuditAspect + AuditEvent + AuditEventListener + AuditAsyncConfig
- AuditTrailEntity + JpaAuditTrailRepository
- AuditTrailService（list/pending/approve/reject + 门店权限校验）
- AuditV2Controller: 4 个端点（/api/v2/audit/**）
- V104: audit_trail.store_id 改 nullable（支持商户级操作）
- 已在 RBAC 6 个写方法上加 @Audited

**安全审查（4 轮，全部通过）：**
- Gemini: 同步阻塞→异步事件驱动、密码泄漏→敏感字段脱敏、事务污染→@Order(LOWEST_PRECEDENCE)
- Codex Round 1: 跨门店读写→SecurityConfig 权限 + 门店校验、store_id=0→nullable
- Codex Round 2: STORE_MANAGE 全局 bypass→仅 SUPER_ADMIN 角色可跨店

**验收：**
- @Audited 方法调用后异步写入 audit_trail
- 敏感字段（password/pin/token）自动脱敏为 [REDACTED]
- 审计 API 需 AUDIT_VIEW/AUDIT_APPROVE 权限 + 门店隔离
- 295 文件编译零错误

**延迟到后续 Phase 的 P2 项：**
- 商户级审计查询 API（storeId=null 的列表）
- 审批强制执行门（requiresApproval 前置拦截 + 403 PENDING）

---

### Phase 1 Milestone Summary

**完成日期：** 2026-03-29
**总产出：** 52 文件，3506 行代码，2 个 Flyway 迁移

### Key Findings

1. **权限设计要前置。** Session 1.1 的 59 个 permission code 是后续所有模块的安全基石。如果权限粒度在 Phase 1 没设计好，Phase 2+ 每个 session 都要回来改 SecurityConfig。

2. **Legacy 迁移必须幂等。** V103 的 INSERT ... ON DUPLICATE KEY UPDATE 设计让 migration 可以反复执行不出错，但代价是映射逻辑复杂（AU-<id> 键、staff_code 优先级）。建议后续迁移都遵循这个模式。

3. **审计不能阻塞业务。** AuditAspect 改为异步事件驱动后，主业务 RT 不受影响。但异步写入意味着审计记录可能延迟 1-2 秒，这对合规场景可接受。

### Important Lessons

1. **跳过 superpowers 工作流导致返工。** Session 1.1 初版直接写代码，多个安全问题在 review 中才发现。**教训：所有编码必须走 superpowers 流程（brainstorm → plan → review）。**

2. **SecurityConfig 的 permitAll 规则顺序决定安全。** Spring Security matcher 是 first-match-wins。细粒度权限 matcher 必须放在 `/api/v2/stores/**` permitAll 之前。

---

## Phase 2: 交易核心（Week 2）

### Session 2.1 — 订单引擎重构 ✅

**完成日期：** 2026-03-29
**总产出：** 48 文件，~2170 行代码

**实际产出：**

*桌台增强（并台/清台/QR）：*
- `v2/store/` 模块：TableMergeApplicationService + TableCleanService + QrTokenService
- `v2/store/` 实体：QrTokenEntity + TableMergeRecordEntity + 2 Repository
- `auth/` 模块：QrOrderingFilter（X-Ordering-Token JWT 验证 + qrTokenId 吊销检查）
- SecurityConfig：table 操作 4 端点 + payment 端点权限门
- QrScanController：GET /qr/{storeId}/{tableId}/{token} → 302 重定向
- CashierSettlementApplicationService：buildSessionChain + rejectMergedChildSession + 并台聚合
- TableOperationsV2Controller：4 个新端点（merge/unmerge/mark-clean/qr-refresh）

*订单引擎修补（buffet 字段透传）：*
- ActiveTableOrderItemEntity + SubmittedOrderItemEntity：加 buffet 3 字段
- toSubmittedItem()：拷贝 buffet 字段 + lineTotal 按 D6 规则调整
- mergeQrItems()：buffet-aware 数量合并
- DTO 透传：ActiveTableOrderDto + SubmittedOrderDto + MerchantAdminOrderItemDto

**验收：**
- ✅ 并台 → 拆台 → 清台 → QR 刷新全流程
- ✅ 扫码 → JWT → 进入点单页
- ✅ buffet 字段从 active → submitted 正确透传
- ✅ 编译零错误，测试通过

**Key Findings：**
1. 并台的复杂度在结算侧，不在并台侧
2. QR token 签发和验证是两个独立的安全边界
3. 嵌套并台必须显式禁止
4. plan review 抓到了真正的 bug：surcharge 需要乘 quantity

---

### Session 2.2 — 结算引擎

**目标：** 支付叠加、幂等、冻结→确认→释放三态完整

**Prompt：**
```
读 final-executable-spec.md D2(支付叠加), D3(券并发), D7(支付重试), D10(不换绑会员)。
在 pos-settlement 模块实现：

1. PaymentStackingService 四阶段：
   a. previewStacking(sessionId) → 干跑，不冻结，返回分解明细
   b. collectStacking(sessionId, stackingChoices) → 冻结积分/储值 + 锁券 + 写 holds
   c. confirmStacking(settlementId) → holds CONFIRMED，扣减余额，券 USED
   d. releaseStacking(settlementId) → holds RELEASED，回退冻结，券 AVAILABLE

2. CouponLockingService：
   - lockCoupon(couponId, expectedVersion) → CAS UPDATE
   - releaseCoupon(couponId) → AVAILABLE
   - 定时任务：10 分钟未确认自动释放

3. PaymentRetryService：
   - switchMethod(attemptId, newMethod) → 旧 attempt REPLACED，新 attempt 创建

4. 修改 CashierSettlementApplicationService：
   - findByTableSessionIdInAndSettlementStatus(sessionChainIds, ...) 支持并台
   - memberId 从 submitted_orders.member_id 取，不接受参数

API 端点：
- POST /settlement/preview-stacking
- POST /settlement/collect-stacking
- POST /settlement/{id}/confirm-stacking
- POST /payment-attempts/{id}/switch-method
```

**验收：**
- 积分 + 券 + 储值 + 现金混合支付成功
- 券并发 CAS 正确（两个终端同时锁同一张券，后者失败）
- 支付失败 → 冻结释放 → 余额恢复
- 并台结账金额 = 两桌合计

---

### Session 2.3 — 退款 + 预约→入座

**目标：** 退款流程完整，预约到入座闭环

**Prompt：**
```
1. RefundService：
   - 部分退款：按 item 粒度，回退积分/储值/券
   - 全额退款：关闭 settlement，回退所有支付手段
   - 退款审批：金额 > threshold → 需 REFUND_APPROVE 权限

2. ReservationService：
   - 创建预约：时间 + 人数 + 联系方式
   - 预约→入座：到店后关联 table_session
   - 过期未到：自动取消 + 释放桌台
   - 预约提醒：临近时间通知

API 端点：
- POST /settlements/{id}/refund
- CRUD /reservations
- POST /reservations/{id}/seat
```

**验收：**
- 退款后积分/储值/券正确回退
- 预约到入座全流程
- 过期自动取消

---

## Phase 3: 商品升级（Week 3）

### Session 3.1 — SKU + 修饰符 + 菜单时段 ✅

**完成日期：** 2026-03-29
**总产出：** ~38 文件，8 轮 review

**实际产出：**
- `v2/catalog/` 模块：MenuTimeSlotEntity + MenuTimeSlotProductEntity + ModifierGroupEntity + ModifierOptionEntity + SkuModifierBindingEntity + SkuPriceOverrideEntity
- 6 Repository + 3 Service（MenuTimeSlotManagementService, ModifierManagementService, SkuPriceOverrideService）
- MenuQueryService：统一菜单查询（按用餐模式 + 时段过滤 + 4 级价格优先级）
- MenuV2Controller + ModifierV2Controller + PriceOverrideV2Controller
- SecurityConfig 收紧 + CDN 图片 URL 解析

**8 轮 review 修复的安全/架构问题：**
- 租户隔离（merchant → store-scope 两级）
- RBAC 权限门（MENU_VIEW / MENU_MANAGE 读写分离）
- IDOR 防御（路径参数 storeId 前置校验）
- 价格链（4 级优先级 + price_context 场景类型 + slot_code ref）
- 图片 URL（CDN 配置 + 批量解析，不占业务线程）
- @Convert 消除热路径 JSON 解析

---

### Session 3.2 — 自助餐引擎 ✅

**完成日期：** 2026-03-29
**总产出：** 38 文件，2868 行，31 个单元测试

**实际产出：**
- BuffetPackageEntity + BuffetPackageItemEntity + 2 Repository
- V108 migration（补 V067 遗漏的 child_count + buffet_overtime_minutes）
- BuffetPackageService：CRUD 档位 + SKU 绑定（BUFFET_MANAGE + store-scope）
- BuffetSessionService：开台 + 实时状态计算 + 限量校验（advisory）
- BuffetMenuService：自助餐菜单（集成到统一 /menu 端点，BUFFET+packageId 委托）
- BuffetPricingService：结账计价（人头费 + 差价 + 套餐外 + 超时费含 grace/cap）
- 3 Controller + SecurityConfig（buffet/status permitAll）
- TableSessionEntity / order item entities 扩展 buffet 字段

**设计规格：** `docs/superpowers/specs/2026-03-29-session-3.2-buffet-module-design.md`
**实现计划：** `docs/superpowers/plans/2026-03-29-session-3.2-buffet-module.md`

**验收：**
- ✅ 编译零错误，31 个单元测试全部通过
- ✅ Final review APPROVED（安全审计 + spec 合规 + 代码质量）
- ✅ Session 3.1 教训全部吸收（IDOR、store-scope、MENU_VIEW、CDN 图片从一开始就做对）

**已知延迟项：**
- validateBuffetOrder 目前是 advisory（未接入真实下单流程），等新 order service
- ENDED 状态由 settlement 写入（Session 2.2 实现）

---

### Phase 3 Milestone Summary

**完成日期：** 2026-03-29
**总产出：** ~76 文件，~3600 行代码，31 个单元测试，1 个 Flyway 迁移（V108）
**Review 轮次：** Session 3.1 共 8 轮，Session 3.2 共 2 轮（含 final review）

#### 功能框架

Phase 3 构建了完整的**商品管理 + 自助餐引擎**，覆盖从商品配置到顾客点单到结账计价的全链路：

```
┌─ 管理端（BUFFET_MANAGE / MENU_MANAGE）─────────────────────────┐
│                                                                 │
│  商品三层结构          修饰符系统         时段菜单               │
│  Product → SKU        ModifierGroup     MenuTimeSlot            │
│  (category归属)       → Option          → SlotProduct           │
│                       → SKU 绑定          (可见性控制)           │
│                                                                 │
│  价格覆盖链                 自助餐档位                          │
│  SkuPriceOverride          BuffetPackage                        │
│  4级优先级:                 → PackageItem                       │
│  STORE > TIME_SLOT         (INCLUDED/SURCHARGE/EXCLUDED)        │
│  > DELIVERY > BASE         + maxQtyPerPerson 限量               │
│                                                                 │
├─ 顾客端（permitAll）────────────────────────────────────────────┤
│                                                                 │
│  统一菜单查询 GET /stores/{id}/menu                             │
│  ├── diningMode=A_LA_CARTE  → MenuQueryService 正常路径         │
│  ├── diningMode=BUFFET      → 委托 BuffetMenuService            │
│  │   + packageId              (package_items→sku 路径)          │
│  └── timeSlotId=X           → 时段可见性过滤                    │
│                                                                 │
│  自助餐生命周期                                                 │
│  POST /buffet/start → session.BUFFET → 计时开始                 │
│  GET  /buffet/status → 实时计算 ACTIVE/WARNING/OVERTIME         │
│  GET  /buffet-pricing/calculate → 人头费+差价+套餐外+超时费     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**权限矩阵：**

| 操作 | 权限 | 租户校验 |
|---|---|---|
| 商品/修饰符/时段/价格覆盖 写 | MENU_MANAGE | merchant + store access |
| 商品/修饰符/时段/价格覆盖 读 | MENU_VIEW ∨ MENU_MANAGE | merchant + store access |
| 自助餐档位 写 | BUFFET_MANAGE | merchant + store access |
| 自助餐档位 读 | MENU_VIEW ∨ BUFFET_MANAGE | merchant + store access |
| 开台 | BUFFET_START | merchant + store access |
| 结账计价 | BUFFET_START | merchant + store access |
| 查菜单 / 查 buffet 状态 | 无（顾客端） | store 存在性 |

**数据流（自助餐结账）：**

```
人头费 = package.priceCents × guestCount + childPriceCents × childCount
差价   = Σ(item.buffetSurchargeCents × quantity)  WHERE buffetIncluded=true
套餐外 = Σ(item.lineTotalCents)                   WHERE buffetIncluded=false
超时费 = min(max(0, 超出分钟 - grace), maxOvertime) × feePerMinute
总计   = 人头费 + 差价 + 套餐外 + 超时费
```

#### 设计中踩的坑

**1. doc/75 和 final-executable-spec 的 schema 不一致（P0 级）**

doc/75 的 `buffet_package_items` 用 `is_included BOOLEAN`（二态），final-exec-spec V083 用 `inclusion_type VARCHAR(32)`（三态 INCLUDED/SURCHARGE/EXCLUDED）。Session 3.2 一开始差点按 doc/75 来，在 brainstorming 阶段拦住了。**教训：** doc/75 是概览文档，V082/V083 from final-exec-spec 才是 DDL 权威。已在 spec 中加了明确的 authoritative source 声明。

**2. V067 catch-up migration 漏了两个字段（P0 级）**

V067 给 `table_sessions` 加了 buffet 字段，但漏掉了 `child_count` 和 `buffet_overtime_minutes`（这两个在 archive V075 里有但没被 catch-up 收录）。直到 plan review 才发现。补了 V108 migration。**教训：** catch-up migration 必须逐字段对照 archive spec，不能只看"大概有了"。

**3. V074 DDL 的字段名和 spec D6 不一致（P0 级）**

D6 spec 写 order items 有 `buffet_package_id`，但实际 V074 DDL 加的是 `buffet_inclusion_type`。`buffet_package_id` 只在 `table_sessions` 上（session 级别），不冗余到行项。第一版 plan 按 spec 来映射了错误的字段，plan review 发现了。**教训：** DDL 是 source of truth，spec 的字段名仅供参考。

**4. 图片 URL 架构争论三轮（P1 级）**

Session 3.1 reviewer 连续三轮指出 `/api/v2/images/{id}` 硬编码会耗尽 Tomcat 线程池。最终改为 `storage.public-base-url` 配置 + `ImageUploadService.resolvePublicUrls()` 批量解析。但改完 `MenuQueryService` 后，`QrMenuApplicationService` 和 `AdminCatalogReadService` 仍有同样的硬编码。**教训：** 架构级修改必须全代码库 grep，不能只改当前模块。

**5. 权限从 merchant-scope 到 store-scope 的演进（P1 级）**

Session 3.1 初版只做了 merchant-level 校验（`store.merchantId == actor.merchantId`）。reviewer 指出 STORE_MANAGER 是 store-scoped role，必须额外检查 `user_store_access`。修了 3 个 service 后，Session 3.2 从一开始就把 store-scope 做对了。**教训：** 权限模型必须在第一个 service 就建立正确的 pattern，后续 service 复制。

**6. 统一 /menu 端点 vs 独立 /buffet-menu 端点（P1 级）**

初版 spec 给 buffet menu 设计了独立的 `GET /buffet-menu/?packageId=X`。spec reviewer 指出 J02 user journey step 4 用的是统一 `GET /menu?diningMode=BUFFET&packageId=X`。改为在 `MenuQueryService` 里做 BUFFET 委托，避免前端改路由。**教训：** API 设计必须回溯 user journey，不能凭空加端点。

**7. 测试代码膨胀（P2 级）**

31 个 Mockito 单测占了 ~1240 行（总量的 43%）。主要原因：SecurityContextHolder mock setup 每个 test case 都要重复，entity 构造没有 builder/factory。这些测试只验证 Java 逻辑，不连 DB、不起 Spring。**教训：** 当前测试策略是快速反馈回路，不是生产验收。后续考虑共享 test fixture 或改用集成测试。

#### 跨 Session 遗留问题

| # | 来源 | 级别 | 问题 | 影响 | 状态 |
|---|------|------|------|------|------|
| 1 | 3.2 | P2 | `validateBuffetOrder` 不查已下单数量 | 限量校验不完整，等新 order service 接入时必须补 | TODO 已标注 |
| 2 | 3.2 | P2 | `buffet_status=ENDED` 无人写入 | 结算完成后 session 不会标记 ENDED | 等 Session 2.2 结算引擎 |
| 3 | 3.1 | P2 | `QrMenuApplicationService` + `AdminCatalogReadService` 仍有硬编码图片 URL | 顾客端和管理端图片走 Spring 代理 | 待全代码库清理 |
| 4 | 3.2 | P2 | `enforceStoreAccess()` 在 4 个 service 里复制粘贴 | 维护成本高，改一处要改四处 | 建议提取 `StoreAccessEnforcer` 共享组件 |
| 5 | 3.2 | P2 | Mockito inline/ByteBuddy attach 在 Homebrew JDK 17 失败 | 本地跑测试需要特定 JVM 参数 | 环境问题，CI 环境待验证 |
| 6 | 3.1 | P2 | 审批强制执行门仍是 metadata-only | `@Audited(requiresApproval=true)` 不阻塞操作 | 延迟到后续 Phase |

---

## Phase 4: 厨房 + 库存（Week 3-4）

### Session 4.1 — KDS + 厨房票

**目标：** 订单按工作站路由拆票，KDS 状态流转，离线自动回退打印

**Prompt：**
```
在 pos-kitchen 模块实现：

1. KitchenStationEntity（含 fallback_printer_ip, kds_health_status, last_heartbeat_at）
2. KitchenTicketEntity + KitchenTicketItemEntity
3. TicketRoutingService：
   - 订单提交 → 按 SKU 的 station_id 拆分为多个 ticket
   - 一个 submitted_order 可拆成 N 个 ticket
4. KdsHealthMonitorService：
   - 接收心跳：POST /stations/{id}/heartbeat
   - 90s 无心跳 → OFFLINE → 自动切 fallback_printer_ip
   - 恢复心跳 → ONLINE → 自动切回
5. TicketStatusService：
   - SUBMITTED → PREPARING → READY → SERVED
   - 退单：SUBMITTED/PREPARING → CANCELLED → 触发库存回补

API 端点：
- POST /stations/{id}/heartbeat
- PUT /kitchen-tickets/{id}/status
- GET /stores/{storeId}/kitchen-tickets?stationId=X&status=Y
```

**验收：**
- 订单拆票正确（不同 station 的 SKU 分到不同票）
- 状态流转正确
- 模拟 KDS 断线 → 票自动打印

---

### Session 4.2 — 库存 + 采购 + 盘点

**目标：** 送货→OCR→入库→SOP 扣减→预警→盘点

**Prompt：**
```
在 pos-inventory 模块实现：

1. InventoryItemEntity, InventoryBatchEntity, RecipeEntity, SupplierEntity + Repository
2. PurchaseInvoiceService：
   - 创建送货单 + OCR 扫描（async）→ ocr_raw_result
   - 员工确认 → 创建 batches + movements + 更新 current_stock
3. StockDeductionService：
   - 结账触发：按 recipes 查 SKU 原料消耗
   - 修饰符消耗：大份 × multiplier，加辣 + 额外原料
   - FIFO 扣减：按 expiry_date ASC 扣 inventory_batches
4. InventoryAlertService：
   - 每日扫描：current_stock < safety_stock → order_suggestions
   - 临期批次 → inventory_driven_promotions(DRAFT)
5. StocktakeService：创建盘点任务 → 录入 → 差异 → 审批 → 调整
6. SopImportService：CSV 上传 → 校验 → 批量更新 recipes
7. WasteRecordService：报损 → 审批 → 库存扣减

API 端点参考 user-journeys.md J08。
```

**验收：**
- 送货→入库→stock 增加
- 结账→SOP 扣减→stock 减少（FIFO 正确）
- 库存预警生成 order_suggestions
- 临期自动生成促销草案

---

## Phase 5: 客户营销（Week 4）

### Session 5.1 — 会员 + 积分 + 储值

**目标：** 会员注册→积分获取→充值→等级升级

**Prompt：**
```
在 pos-member 模块实现：

1. MemberEntity, MemberAccountEntity, PointsBatchEntity + Repository
2. MemberService：注册、查询、等级升级
3. PointsService：
   - 结账后按 points_rules 计算积分
   - 积分流水记 member_points_ledger
   - 积分批次 FIFO 过期
4. StoredValueService：
   - 充值 → member_cash_ledger + member_accounts 更新
   - 充值活动（recharge_campaigns）：满额送储值/积分/券
5. TierService：
   - lifetime_spend_cents 达标 → 自动升级
   - 等级权益：积分倍率、折扣、生日奖励

API 端点：
- POST /members/register
- POST /members/{id}/recharge
- GET /members/{id}/points-history
- GET /members/{id}/account
```

**验收：**
- 注册 → 积分 = 0 → 消费 → 获得积分 → 积分可查
- 充值 → 储值增加 → 可在结算时使用
- 累计消费达标 → 等级自动升级

---

### Session 5.2 — 优惠券 + 促销 + 渠道

**目标：** 优惠券发放/使用，促销规则引擎，渠道分润

**Prompt：**
```
在 pos-member（券）和 pos-promotion（促销）和 pos-integration（渠道）模块实现：

1. CouponTemplateService：模板 CRUD
2. MemberCouponService：发券、用券（CAS 已在 Session 2.2 实现）、过期定时任务
3. PromotionRuleService：
   - CRUD 促销规则（满减/折扣/赠品）
   - 自动命中最优规则
   - promotion_hits 记录
4. ChannelService：渠道 CRUD + 佣金规则
5. ChannelAttributionService：订单归因 + 佣金计算
6. ChannelSettlementService：月结批次生成
```

**验收：**
- 发券 → 会员持有 → 结账时可用 → USED
- 促销规则命中 → 折扣正确
- 渠道归因 + 佣金计算正确

---

## Phase 6: 联调收尾（Week 5-6）

### Session 6.1 — 外卖对接 + AI 层

**目标：** 外卖平台订单接入，AI 报表 + MCP 工具连真数据

**Prompt：**
```
1. DeliveryWebhookController：
   - POST /webhooks/delivery/{platform}
   - 接收外卖平台订单 → 创建 submitted_order(DELIVERY)
   - 每次调用记 external_integration_logs
2. DeliveryStatusService：READY_FOR_PICKUP → PICKED_UP → DELIVERED

3. ReportSnapshotService：
   - generateDaily(storeId)（定时任务）
   - 计算 metrics_json + 调用 AI 生成摘要/建议
4. 更新 MCP 工具：读工具连接真实 Repository
5. AI 建议审批：ai_recommendations 状态机（PROPOSED → APPROVED/REJECTED）
```

**验收：**
- 外卖 webhook → 订单创建 → 厨房出票
- AI 日报内容有意义
- MCP 工具返回真实数据

---

### Session 6.2 — 部署 + 前端联调

**目标：** 部署到 AWS，12 条 Journey 全部走通

**Prompt：**
```
1. E2E 集成测试：
   - 逐条走 J01-J12 的 main flow
   - @SpringBootTest + TestContainers MySQL
2. 部署：
   - 更新 docker-compose.yml
   - Flyway migration 在 AWS MySQL 上跑通
   - Legacy 数据迁移脚本
   - Nginx 配置更新
3. 前端联调：
   - API 响应格式对齐
   - 跨域配置
   - 健康检查端点确认
```

**验收：**
- `mvn test` 全部通过
- 12 条 Journey 的 main flow 有测试覆盖
- AWS 部署运行正常

---

## 跨 Session 遗留问题

| # | 来源 | 级别 | 问题 | 状态 |
|---|------|------|------|------|
| 1 | 1.2 | P2 | 审批强制执行门（requiresApproval 前置拦截 + 403 PENDING） | 延迟到后续 |
| 2 | 2.1 | P1 | SecurityConfig `/api/v2/stores/**` permitAll 太宽泛 | 待 Phase 6 统一收紧 |
| 3 | 2.1 | P2 | 无定向测试覆盖 QR token / 并台结算 / legacy preview | 待补 |
| 4 | 3.2 | P2 | BuffetPackageServiceTest 在 Homebrew JDK 17 上 Mockito attach 失败 | 环境问题 |
| 5 | 3.2 | P2 | validateBuffetOrder 未查已下单数量（advisory） | 等新 order service |
