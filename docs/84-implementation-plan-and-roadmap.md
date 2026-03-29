# FounderPOS V3 — Implementation Plan & Roadmap

**Version:** V20260328021
**Date:** 2026-03-28
**Status:** DRAFT
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
- Migration 文件：20 个（V066-V101，V099 已删除）
- 新表：9 张，ALTER：8 项

---

# Part 1: Roadmap（按周）

```
Week 0 (完成)  ── 设计收尾
  ✅ 0.1-0.4 Journey + 状态机 + Gap + DDL Review
  ✅ 0.5 生成 Flyway migration SQL（20 文件）
  ✅ 0.6 验证 migration 可执行（128 表通过）
  ✅ 0.7 Code Review 修复（4 项）

Week 1         ── 能登录、能审计
  Phase 1: RBAC + 审计
  里程碑：用户登录 → 看到权限菜单 → 操作被审计记录

Week 2         ── 扫码点单到结账
  Phase 2: 交易核心
  里程碑：扫码 → 点菜 → 送厨 → 结账 → 清台全流程

Week 3         ── 自助餐 + 厨房
  Phase 3A: 自助餐 + 厨房 KDS
  里程碑：选档位 → 自助餐菜单 → 计时 → 超时计费 → KDS 显示

Week 4         ── 库存
  Phase 3B: 库存全链路
  里程碑：送货 → OCR → 入库 → SOP 扣减 → 预警 → 盘点

Week 5         ── 会员 + 营销
  Phase 4: 客户与营销
  里程碑：注册 → 积分 → 充值 → 用券 → 储值支付 → 渠道归因

Week 6         ── AI + 收尾
  Phase 5: 报表 + AI + 联调
  里程碑：AI 日报 → MCP 工具 → E2E 全流程 → 部署
```

---

# Part 2: Implementation Plan（按 Session）

## Phase 0: 基础准备

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

## Phase 1: 组织底座（Week 1）

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

**验收：**
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

**延迟到 Phase 2 的 P2 项：**
- 商户级审计查询 API（storeId=null 的列表）
- 审批强制执行门（requiresApproval 前置拦截 + 403 PENDING）

---

## Phase 1 Milestone Summary

**完成日期：** 2026-03-29
**总产出：** 52 文件，3506 行代码，2 个 Flyway 迁移

### Key Findings

1. **Legacy 兼容是最大的复杂度来源。** auth_users/staff 双表、旧 JWT claims、collation 差异、id 不一致——每一个都产生了 P1 级安全漏洞。如果从零开始会简单 10 倍，但我们必须保证旧 app 不挂。

2. **租户隔离必须从 Day 1 做。** 每个接受 merchantId/storeId/roleId 参数的 API 都是潜在的跨租户攻击面。reviewer 在 RBAC 管理、审计查询、角色分配、门店授权上反复发现同一类问题。教训：任何接受外部 ID 的 service 方法都必须先校验归属。

3. **审计切面不能同步写 DB。** 初版是直接 repository.save()，Gemini 指出这会在高并发下拖死业务主线程。改成 ApplicationEventPublisher + @Async + @TransactionalEventListener(AFTER_COMMIT) 后，审计写入完全不影响业务延迟。

4. **权限 ≠ 角色。** `STORE_MANAGE` 权限被当作"全局管理员"的 bypass 条件，但 STORE_MANAGER 也有这个权限——导致店长能看所有门店的审计日志。正确做法是用角色代码（SUPER_ADMIN）而不是权限码做全局 bypass。

5. **种子数据必须精确可执行。** 初版 docs/86 写"共 52 个权限"但实际有 59 个，SQL 有占位符。reviewer 指出后重写为完整可执行 SQL，用临时表实现幂等 upsert。

### Important Lessons for Phase 2+

1. **每个 service 方法的第一行应该是权限/租户校验**，不要依赖 Controller 或 SecurityConfig 单层防护。
2. **新旧系统并行期，异常类型决定 fallback 行为**——IllegalArgumentException vs IllegalStateException vs NoSuchElementException 的语义必须严格区分。
3. **FK 约束要考虑所有调用者的上下文**——audit_trail.store_id NOT NULL 看似合理，但商户管理员没有 store 上下文。
4. **code review 是真正的质量关**——10 轮 review 发现了 20+ 个安全问题，全部在编码阶段修复，零生产事故。
5. **预留 role code 必须加黑名单**——JwtAuthFilter 把 roleCode 转成 ROLE_<code> 写入 authorities，如果商户能创建 SUPER_ADMIN 角色就能伪造平台权限。

---

## Phase 2: 交易核心（Week 2）

### Session 2.1 — 桌台增强（并台/清台/QR） ✅

**完成日期：** 2026-03-29
**总产出：** 27 文件，~950 行代码，6 次 commit（含 4 轮 review 修复）

**实际产出：**
- `v2/store/` 模块：TableMergeApplicationService + TableCleanService + QrTokenService
- `v2/store/` 实体：QrTokenEntity + TableMergeRecordEntity + 2 Repository
- `auth/` 模块：QrOrderingFilter（X-Ordering-Token JWT 验证 + qrTokenId 吊销检查）
- `auth/` 模块：JwtProvider.generateOrderingToken（含 qrTokenId claim）
- SecurityConfig：table 操作 4 端点 + payment 端点权限门（在 `/stores/**` permitAll 之前）
- QrScanController：GET /qr/{storeId}/{tableId}/{token} → 302 重定向
- QrOrderingV2Controller：enforceTokenBinding（storeId + tableCode 双绑定）
- CashierSettlementApplicationService：buildSessionChain + rejectMergedChildSession + 全路径并台聚合
- ActiveTableOrderApplicationService：rejectNonOrderableTable（MERGED/PENDING_CLEAN/DISABLED 拒单）
- 4 个 DTO + 2 个 Request record
- TableOperationsV2Controller：4 个新端点（merge/unmerge/mark-clean/qr-refresh）

**验收：**
- ✅ A01 并入 A02 → A02 状态 MERGED → 从 A01 结账能聚合两桌订单
- ✅ 拆台 → 两桌独立
- ✅ 清台 → PENDING_CLEAN → AVAILABLE → QR 刷新
- ✅ 扫码 → JWT → 进入点单页
- ✅ 编译零错误（Java 17, Spring Boot 3.3.3）
- ✅ `./mvnw test -q` 通过

---

### Session 2.1 Key Findings

1. **跳过 superpowers 工作流导致返工 4 轮。** 初版直接写代码，11 个 P1 安全/逻辑问题在 review 中才被发现。如果先走 brainstorm → plan → review 流程，至少一半问题（QrOrderingFilter 漏做、mergeRecordId 用错实体、结算不走 session chain）在设计阶段就能拦住。**教训：以后所有编码必须走 superpowers 流程。**

2. **并台的复杂度在结算侧，不在并台侧。** merge/unmerge 本身很简单（打指针 + 改状态），但结算要聚合 session chain、关闭所有 merged session、设置所有桌 PENDING_CLEAN、拒绝 child table 独立结算、preview 金额一致性。**教训：并台的验收条件"从主桌结账能聚合两桌订单"必须在同一个 session 里实现，不能拆到 2.3。**

3. **QR token 签发和验证是两个独立的安全边界。** 签发端（QrScanController）验 qr_tokens 表后颁发 JWT；验证端（QrOrderingFilter）不仅要验 JWT 签名，还要：(a) 检查 qrTokenId 是否仍 ACTIVE（清台吊销），(b) 绑定 storeId + tableCode 防跨桌/跨店重放。任何一层缺失都是 P1。

4. **嵌套并台必须显式禁止。** `buildSessionChain` 只走一层 `merged_into_session_id`，如果允许 B→A 再 A→C，B 会变成不可达的 grandchild。修复方式是在 merge 时检查 mergedTable 是否已经是其他桌的 master。

5. **SecurityConfig 的 permitAll 规则顺序决定安全。** Spring Security matcher 是 first-match-wins。table 操作和 payment 端点的权限 matcher 必须放在 `/api/v2/stores/**` permitAll 之前，否则被通配符吞掉。

### Important Lessons for Session 2.2+

1. **legacy 入口必须和新入口保持语义一致。** `getSettlementPreview(activeOrderId)` 是遗留 API，但它也要感知并台。delegate 后还要保留原始 `activeOrderId` 字段，不能改变响应结构。
2. **table_merge_records 表是并台的 source of truth，不是 session 指针。** unmerge 必须通过 `mergeRecordId` 查 `table_merge_records`，不能用 `session.id` 代替——否则 merge-info API 无法实现，审计历史也丢失。
3. **状态机约束必须在所有入口强制执行。** MERGED/PENDING_CLEAN 桌不接受新订单——这个规则要在 `getQrOrderingContext` 和 `submitQrOrdering` 两个入口都拦截，不能只挡一个。
4. **filter 层做 token 验证时，必须同时做"活性检查"。** JWT 签名有效不等于业务状态有效——清台后 qr_tokens 已 EXPIRED，但 JWT 还没到期。filter 必须回查 DB 确认 token 未被吊销。

---

### 2.1 跨 Session 遗留问题

| # | 级别 | 问题 | 状态 |
|---|------|------|------|
| 1 | **P1** | SecurityConfig `/api/v2/stores/**` permitAll 太宽泛，Session 2.1 只加了 table 操作和 payment 的细粒度 matcher，其他 store 端点（如 reservation、transfer）仍然公开 | 待修：Phase 2 末尾统一做一次 SecurityConfig 全面收紧 |
| 2 | **P2** | 无定向测试覆盖 QR token 流程 / 并台结算 / legacy preview 并台路径 | 待修：Phase 2 测试 session 或各 session 补单元测试 |

---

### Session 2.2 — 订单引擎修补

**目标：** active_table_order_items 和 submitted_order_items 支持 buffet 字段，订单提交时正确拷贝

**Prompt：**
```
读 final-executable-spec.md D6(buffet 价格持久化)。
修改 pos-core 模块：

1. ActiveTableOrderItemEntity 加 3 个字段：isBuffetIncluded, buffetSurchargeCents, buffetInclusionType
2. SubmittedOrderItemEntity 加同样 3 个字段
3. 修改 ActiveTableOrderApplicationService.toSubmittedItem()：
   - 拷贝 buffet 字段到 submitted_order_items
   - 如果 isBuffetIncluded=true 且 surcharge=0 → lineTotal=0
   - 如果 isBuffetIncluded=true 且 surcharge>0 → lineTotal=surcharge
   - 如果 isBuffetIncluded=false → lineTotal=原价
4. 修改 settlement 金额计算：按上述逻辑汇总

不要动 buffet_packages 表的 CRUD——那是 Phase 3A 的事。
这里只修补订单层的字段传递。
```

**产出：**
- Entity 更新 2 个
- Service 更新 1 个（toSubmittedItem）

**验收：**
- 提交订单后 submitted_order_items 的 buffet 字段有值
- 结算金额正确（免费=0，差价=surcharge，套餐外=原价）

---

### Session 2.3 — 结算闭环（支付叠加 + 冻结/确认/释放）

**目标：** 积分/储值/券支付叠加，冻结→确认→释放三态完整

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

**产出：**
- `pos-settlement/` 下 ~8 个新文件
- 定时任务配置

**验收：**
- 积分 + 券 + 储值 + 现金混合支付成功
- 券并发 CAS 正确（两个终端同时锁同一张券，后者失败）
- 支付失败 → 冻结释放 → 余额恢复
- 并台结账金额 = 两桌合计

---

## Phase 3A: 商品与供应 — 自助餐 + 厨房（Week 3）

### Session 3.1 — SKU 三层 + 修饰符 + 时段菜单

**目标：** 商品管理 CRUD 完整，修饰符可配置，时段菜单可用

**Prompt：**
```
在 pos-catalog 模块实现：

1. ProductEntity, SkuEntity, ModifierGroupEntity, ModifierOptionEntity + Repository
2. SkuService：CRUD + 价格覆盖链（sku_price_overrides → base_price_cents）
3. ModifierService：组 CRUD + 选项 CRUD + SKU 绑定
4. MenuTimeSlotService：时段 CRUD + 商品可见性
5. 菜单查询 API：GET /stores/{storeId}/menu?diningMode=X&timeSlotId=Y
   - 按用餐模式过滤
   - 按时段过滤
   - 返回 SKU + 修饰符 + 价格

不含 buffet_packages CRUD——下个 session 做。
```

**产出：**
- `pos-catalog/` 下 ~20 个文件

**验收：**
- 创建商品 → SKU → 绑修饰符 → 设时段 → 查菜单返回正确

---

### Session 3.2 — 自助餐模块

**目标：** 自助餐从开台到计时到超时计费全流程

**Prompt：**
```
读 final-executable-spec.md D5(buffet 开台), D6(价格持久化)。
创建 pos-buffet 模块：

1. BuffetPackageEntity + BuffetPackageItemEntity + Repository
2. BuffetPackageService：CRUD 档位 + 绑定 SKU（INCLUDED/SURCHARGE/EXCLUDED）
3. BuffetSessionService：
   - startBuffet(tableId, packageId, guestCount, childCount)
     → session.dining_mode=BUFFET, buffet_started_at=NOW(), buffet_ends_at=NOW()+duration
   - getBuffetStatus(tableId) → 剩余时间 + 状态（ACTIVE/WARNING/OVERTIME）
4. BuffetMenuService：
   - getBuffetMenu(storeId, packageId) → 过滤 SKU，标注 inclusionType + surcharge
   - 限量检查：max_qty_per_person × guestCount
5. BuffetPricingService：
   - calculateTotal(sessionId) → 人头费 + surcharge + 套餐外 + 超时费
   - overtimeFee = (超出分钟 - grace) × feePerMinute

API 端点：
- CRUD /buffet-packages
- POST /tables/{tableId}/buffet/start
- GET /tables/{tableId}/buffet/status
```

**产出：**
- `pos-buffet/` 下 ~12 个文件

**验收：**
- 选档位 → 开始 → 菜单正确（免费/差价/套餐外区分）
- 超时计费金额正确
- 限量点单拦截正确

---

### Session 3.3 — 厨房 KDS

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

**产出：**
- `pos-kitchen/` 下 ~10 个文件

**验收：**
- 订单拆票正确（不同 station 的 SKU 分到不同票）
- 状态流转正确
- 模拟 KDS 断线 → 票自动打印

---

## Phase 3B: 库存全链路（Week 4）

### Session 3.4 — 库存管理

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

**产出：**
- `pos-inventory/` 下 ~20 个文件
- 定时任务（库存预警、临期扫描）

**验收：**
- 送货→入库→stock 增加
- 结账→SOP 扣减→stock 减少（FIFO 正确）
- 库存预警生成 order_suggestions
- 临期自动生成促销草案

---

## Phase 4: 客户与营销（Week 5）

### Session 4.1 — 会员 + 积分 + 储值

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

**产出：**
- `pos-member/` 下 ~15 个文件

**验收：**
- 注册 → 积分 = 0 → 消费 → 获得积分 → 积分可查
- 充值 → 储值增加 → 可在结算时使用
- 累计消费达标 → 等级自动升级

---

### Session 4.2 — 券 + 裂变 + 促销引擎

**目标：** 优惠券发放/使用，推荐裂变，促销规则引擎

**Prompt：**
```
在 pos-member（券/裂变）和 pos-promotion（促销）模块实现：

1. CouponTemplateService：模板 CRUD
2. MemberCouponService：发券、用券（CAS 已在 Session 2.3 实现）、过期定时任务
3. ReferralService：
   - 生成推荐码
   - 好友注册关联 → 双方获奖励
4. PromotionRuleService：
   - CRUD 促销规则（满减/折扣/赠品）
   - 自动命中最优规则
   - promotion_hits 记录
5. InventoryDrivenPromotionService：
   - 临期/滞销 → 自动生成草案
   - 店长审批 → 创建 promotion_rule
```

**产出：**
- `pos-member/` 增加券/裂变 ~8 个文件
- `pos-promotion/` 下 ~8 个文件

**验收：**
- 发券 → 会员持有 → 结账时可用 → USED
- 推荐码 → 好友注册 → 双方获积分
- 促销规则命中 → 折扣正确

---

### Session 4.3 — 渠道分润 + 外卖对接

**目标：** 渠道归因、佣金计算、外卖订单接入

**Prompt：**
```
在 pos-integration 模块实现：

1. ChannelService：渠道 CRUD + 佣金规则
2. ChannelAttributionService：
   - 订单创建时记录 order_channel_attribution
   - 按渠道计算佣金 → channel_commission_records
3. ChannelSettlementService：月结批次生成
4. DeliveryWebhookController：
   - POST /webhooks/delivery/{platform}
   - 接收外卖平台订单 → 创建 submitted_order(DELIVERY)
   - 每次调用记 external_integration_logs
5. DeliveryStatusService：
   - READY_FOR_PICKUP → PICKED_UP → DELIVERED

API 端点参考 user-journeys.md J03, J10。
```

**产出：**
- `pos-integration/` 下 ~12 个文件

**验收：**
- 外卖 webhook → 订单创建 → 厨房出票
- 渠道归因正确
- 月结佣金计算正确

---

## Phase 5: 报表与 AI（Week 6 前半）

### Session 5.1 — 报表快照 + AI 摘要

**目标：** 每日自动生成报表快照，含 AI 摘要/亮点/警告/建议

**Prompt：**
```
在 pos-report 模块实现：

1. ReportSnapshotService：
   - generateDaily(storeId)（定时任务，每晚 23:00 UTC）
   - 计算 metrics_json：营收、订单数、客单价、翻台率、自助餐占比...
   - 调用 AI 模型生成 ai_summary, ai_highlights, ai_warnings, ai_suggestions
2. ReportSnapshotController：
   - GET /stores/{storeId}/reports/snapshot?type=DAILY_SUMMARY&date=2026-03-28
   - POST /merchants/{merchantId}/reports/multi-store-compare

报表模块只读业务表，不写任何业务表。
```

**产出：**
- `pos-report/` 下 ~6 个文件

**验收：**
- 定时任务跑完 → report_snapshots 有记录
- AI 摘要内容有意义
- 多店对比 API 返回正确

---

### Session 5.2 — MCP 工具对接真数据

**目标：** AI 层 MCP 工具连接真实业务数据

**Prompt：**
```
在 pos-ai 模块实现：

1. 更新现有 MCP 工具：读工具连接真实 Repository（不再 mock）
2. AI 建议审批流程：
   - ai_recommendations 状态机：PROPOSED → APPROVED/REJECTED
   - 老板在后台审批
3. Agent 身份模块保持现有结构（已有 restaurant_agents, agent_wallets）

重点是让已有的 MCP 工具读到真数据，而不是重写工具。
```

**产出：**
- `pos-ai/` 更新 ~8 个文件

**验收：**
- MCP 工具调用返回真实数据
- AI 建议可查看、可审批

---

## Phase 6: 联调与收尾（Week 6 后半）

### Session 6.1 — E2E 全流程测试

**目标：** 12 条 Journey 全部走通

**Prompt：**
```
读 docs/superpowers/specs/2026-03-28-user-journeys.md。
逐条走 J01-J12 的 main flow：

1. 对每条 Journey 写集成测试（@SpringBootTest + TestContainers MySQL）
2. 测试覆盖 main flow + 至少 1 个 alternative flow
3. 测试验证状态机流转正确、金额计算正确、并发控制正确

测试文件放 pos-app/src/test/java/...
```

**产出：**
- 12+ 个集成测试类

**验收：**
- `mvn test` 全部通过
- 12 条 Journey 的 main flow 有测试覆盖

---

### Session 6.2 — 部署 + 数据迁移

**目标：** 部署到 AWS，旧数据迁移

**Prompt：**
```
1. 更新 docker-compose.yml：加入新模块配置
2. 确保 Flyway migration 在 AWS MySQL 上跑通
3. Legacy 数据迁移脚本：
   - auth_users → users
   - staff → employees
   - roles → custom_roles
4. Nginx 配置更新（新 API 路由）
5. 健康检查端点确认

部署到 54.237.230.5。
```

**产出：**
- 更新后的 docker-compose.yml
- 迁移脚本
- 部署验证截图

**验收：**
- AWS 上能登录
- 扫码→点单→结账全流程通

---

# Part 3: Session 依赖图

```
Phase 0: 0.5 → 0.6
                ↓
Phase 1: 1.1 → 1.2
                ↓
Phase 2: 2.1 → 2.2 → 2.3
                ↓
Phase 3: 3.1 → 3.2 ──→ 3.3
                         ↓
                        3.4
                         ↓
Phase 4: 4.1 → 4.2 → 4.3
                         ↓
Phase 5: 5.1 → 5.2
                ↓
Phase 6: 6.1 → 6.2
```

**可并行的 session：**
- 3.3（厨房）和 3.4（库存）可并行
- 5.1（报表）和 5.2（MCP）可并行

---

# Part 4: 关键风险

| 风险 | 影响 | 缓解 |
|------|------|------|
| Migration 冲突（Sprint 计划 vs Step 0.3） | DDL 执行失败 | 以 docs/80 为准，Session 0.5 统一生成 |
| 支付叠加复杂度高 | Session 2.3 可能超时 | 拆成 preview + collect + confirm 三个 PR |
| OCR 依赖外部 API | 库存入库流程阻塞 | OCR 做成 async，失败可手动录入 |
| 前端联调量大 | Week 6 不够 | 后端先 API 完整，前端可后续迭代 |
| AI 模型调用 | 报表 AI 摘要依赖 LLM API | 先用模板摘要，LLM 做增强 |

---

# Part 5: 不在本轮范围（P1）

以下功能出现在 Sprint 计划但不在 Step 0.3 的 125 表框架内，推迟到 P1：

| 功能 | 表 | 理由 |
|------|---|------|
| 巡店检查 | inspection_records, inspection_items | 非核心流程 |
| CCTV 事件 | cctv_events | 依赖硬件对接 |
| 支付叠加规则表 | payment_stacking_rules | 先硬编码顺序，后续加配置 |
| 会员标签自动化 | member_tags auto_rule | 先手动标签，后续加自动 |
| 营销触达 | marketing_campaigns 实际发送 | 先记录，后续对接 WhatsApp/SMS |

---

*共 16 个 Session，预计 6 周。每个 session 对应一次 Claude Code 对话。*
