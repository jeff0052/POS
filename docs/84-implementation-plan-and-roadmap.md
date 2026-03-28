# FounderPOS V3 — Implementation Plan & Roadmap

**Version:** V20260328021
**Date:** 2026-03-28
**Status:** DRAFT
**受众：** Jeff + Claude 协作写代码的执行清单
**前置文档：** `docs/83-system-architecture-v3.md`（架构总纲）

---

## 源 of Truth 优先级

```
1. docs/80-step-0.3-data-model-gaps.md     ← DDL 以这个为准
2. specs/final-executable-spec.md          ← 设计决策 D1-D10 以这个为准
3. specs/sprint-plan-complete.md           ← Java 类名/API 端点参考，但 DDL 已过时
4. docs/83-system-architecture-v3.md       ← 表归属和框架
```

---

# Part 1: Roadmap（按周）

```
Week 0 (当前)  ── 设计收尾
  ✅ 0.1-0.4 Journey + 状态机 + Gap + DDL Review
  →  0.5 生成 Flyway migration SQL
  →  0.6 验证 migration 可执行

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

### Session 1.1 — RBAC + 统一用户 + Legacy 迁移

**目标：** users 表替代 auth_users + staff，RBAC 权限体系可用

**前置：** Phase 0 完成（migration 已跑）

**Prompt：**
```
读 docs/83-system-architecture-v3.md 第 5.2 节和 docs/75 的 Auth/RBAC DDL。
在 pos-auth 模块实现：

1. UserEntity + UserRepository（对应 users 表）
2. PermissionEntity、CustomRoleEntity、UserRoleEntity、UserStoreAccessEntity
3. UserService：CRUD + 密码 hash + PIN hash + 登录 + 锁定
4. RbacService：角色分配、权限查询、门店授权
5. SecurityConfig：JWT 认证 + 权限过滤器
6. 数据迁移脚本：auth_users + staff → users 表
7. 预置权限种子数据（INSERT permissions）

参考现有代码的命名风格和包结构。
```

**产出：**
- `pos-auth/` 下 ~15 个 Java 文件
- 权限种子数据 SQL
- Legacy 迁移脚本

**验收：**
- POST /api/v2/auth/login 返回 JWT
- JWT 带 permissions 列表
- 旧 auth_users 数据迁移到 users 无丢失

---

### Session 1.2 — 审计切面 + audit_trail

**目标：** @Audited 注解 + AOP 切面，所有写操作自动记审计日志

**Prompt：**
```
读 final-executable-spec.md D8 设计决策。
在 pos-common 模块实现：

1. @Audited 注解（action, riskLevel, requiresApproval, targetType, targetIdExpression）
2. AuditAspect：@Around 切面，自动记录 before/after snapshot
3. AuditTrailEntity + AuditTrailRepository（对应 audit_trail 表）
4. AuditTrailService：查询审计日志、审批流程（approve/reject）
5. AuditTrailController：GET /audit-logs, POST /audit-logs/{id}/approve

注意：action_log 不动，保留给 AI MCP 工具层。audit_trail 是独立的新表。
```

**产出：**
- `pos-common/` 下审计相关 ~6 个文件
- 审计 Controller

**验收：**
- 任何加了 @Audited 的方法被调用后，audit_trail 有记录
- before_snapshot 和 after_snapshot 正确
- 需要审批的操作返回 403 + PENDING 状态

---

## Phase 2: 交易核心（Week 2）

### Session 2.1 — 桌台增强（并台/清台/QR）

**目标：** 桌台状态机完善，并台/拆台/清台/动态 QR 全部可用

**Prompt：**
```
读 final-executable-spec.md D1(并台), D4(QR), D9(清台)。
在 pos-core 模块实现：

1. StoreTableEntity 更新状态机：AVAILABLE/OCCUPIED/RESERVED/PENDING_SETTLEMENT/PENDING_CLEAN/MERGED/DISABLED
2. TableMergeApplicationService：merge(masterTableId, mergedTableId) + unmerge(mergeRecordId)
   - 并台：被并桌 session.merged_into_session_id = 主桌 session.id，桌台状态 → MERGED
   - 拆台：清空 merged_into_session_id，桌台状态 → OCCUPIED
3. QrTokenService：refreshQr(tableId) + validateQr(token)
   - UUID token 存 qr_tokens 表
   - 验证后颁发 JWT（sessionId 可为 null）
4. QrOrderingFilter：解析 X-Ordering-Token header，验证 JWT
5. TableCleanService：markClean(tableId) → 状态 PENDING_CLEAN → AVAILABLE + QR 刷新
6. 用 @Audited 标记 merge/unmerge/markClean

API 端点：
- POST /tables/merge
- POST /tables/unmerge
- POST /tables/{tableId}/mark-clean
- POST /tables/{tableId}/qr/refresh
- GET /qr/{storeId}/{tableId}/{token}（公开，扫码入口）
```

**产出：**
- `pos-core/` 下桌台相关 ~10 个文件
- QR Controller（公开端点）

**验收：**
- A01 并入 A02 → A02 状态 MERGED → 从 A01 结账能聚合两桌订单
- 拆台 → 两桌独立
- 清台 → PENDING_CLEAN → AVAILABLE → QR 刷新
- 扫码 → JWT → 进入点单页

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
