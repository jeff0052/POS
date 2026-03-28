# FounderPOS V3 — 12 User Journeys

**Version:** V20260328015
**Date:** 2026-03-28
**Status:** DRAFT
**对齐文档:** `2026-03-28-final-executable-spec.md`（单一可执行 spec）

---

## J01 顾客单点堂食

**Actor:** 顾客（扫码）+ 服务员（协助）
**Precondition:** 桌台 AVAILABLE，QR token 有效

**Trigger:** 顾客手机扫桌台二维码

**Main Flow:**
1. 扫码 → GET /qr/{storeId}/{tableId}/{token}
2. 后端验证 token（DB lookup）→ 颁发 ordering JWT（sessionId 可为 null）
3. 302 重定向到 /ordering?token={jwt}
4. 前端加载菜单 → GET /api/v2/stores/{storeId}/menu?diningMode=A_LA_CARTE
5. 顾客选菜 + 修饰符（辣度/规格/加料）→ 加入购物车
6. 提交订单 → POST /api/v2/qr-ordering/{storeId}/{tableId}/submit
   → 后端 findOrCreateOpenSession → 桌台 AVAILABLE→OCCUPIED
   → 创建 active_table_order + items → 服务端定价
   → persistSubmittedOrder → submitted_order(SUBMITTED/UNPAID)
7. 厨房收到 kitchen_ticket
8. 顾客加单（重复 5-7，同一 session 下新增 submitted_order）
9. 服务员发起结账 → POST /tables/{tableId}/settlement/collect
   → 聚合 session 下所有 UNPAID submitted_orders
   → settlement_record 写入
   → submitted_orders.settlement_status → SETTLED
   → table_sessions.session_status → CLOSED
   → store_tables.table_status → PENDING_CLEAN
10. 服务员清台 → POST /tables/{tableId}/mark-clean → table_status → AVAILABLE，QR 刷新
11. 顾客收到评价入口 → POST /stores/{storeId}/feedback (REVIEW)

**Alternative Flows:**

A1 商品停售:
  步骤 5 时 SKU 的 store_sku_availability.is_available = false
  → 前端显示"已售罄"，禁止加入购物车

A2 QR 过期:
  步骤 2 token 验证失败 → 400 "二维码已过期，请联系服务员"
  → 服务员在 POS 刷新 QR 或手动开台

A3 会话中途断开:
  顾客关闭浏览器后重新扫码 → 同一 token 仍有效（未过期）
  → JWT 重新颁发，前端恢复已点菜品（从 active_table_order 读）

**State Changes:**
| 表 | 字段 | 变化 |
|---|---|---|
| store_tables | table_status | AVAILABLE → OCCUPIED → PENDING_CLEAN → AVAILABLE |
| table_sessions | session_status | (created) OPEN → CLOSED |
| active_table_orders | status | DRAFT → SUBMITTED → PENDING_SETTLEMENT → (deleted) |
| submitted_orders | settlement_status | UNPAID → SETTLED |
| settlement_records | final_status | SETTLED (created) |
| customer_feedback | feedback_status | NEW (created) |

**Touched Modules:** pos-core, pos-catalog, pos-settlement, pos-member(如绑定会员), pos-kitchen

**APIs:**
- GET /qr/{storeId}/{tableId}/{token}
- GET /api/v2/stores/{storeId}/menu?diningMode=A_LA_CARTE
- POST /api/v2/qr-ordering/{storeId}/{tableId}/submit
- POST /api/v2/stores/{storeId}/tables/{tableId}/settlement/collect
- POST /api/v2/stores/{storeId}/tables/{tableId}/mark-clean
- POST /api/v2/stores/{storeId}/feedback

**DDL Impact:** V071 (QR token), V073 (audit_trail), V091 (customer_feedback)

**Done Criteria:**
- 扫码→点单→结账→清台全流程可走通
- QR 过期/无效场景有明确错误提示
- 结账后 table_status 经过 PENDING_CLEAN 再到 AVAILABLE
- 评价可提交并在后台可查

---

## J02 顾客自助餐

**Actor:** 顾客（扫码）+ 服务员（开台选档位）
**Precondition:** 桌台 AVAILABLE，至少一个 ACTIVE 的 buffet_package

**Trigger:** 服务员在 POS 开台并选择"自助餐"模式

**Main Flow:**
1. 服务员选桌 → 桌台 OCCUPIED → findOrCreateOpenSession
2. POST /stores/{storeId}/tables/{tableId}/buffet/start { packageId, guestCount:4, childCount:1 }
   → session.dining_mode = BUFFET
   → session.buffet_started_at = NOW()
   → session.buffet_ends_at = NOW() + 120min
   → session.buffet_status = ACTIVE
3. 顾客扫码 → QR 验证 → JWT（sessionId 已存在）
4. 前端检测 session.dining_mode=BUFFET → 加载自助餐菜单
   GET /stores/{storeId}/menu?diningMode=BUFFET&packageId={pkgId}
   → 每个 SKU 标注 inclusionType + surcharge
5. 顾客点套餐内菜品 → is_buffet_included=true, line_total_cents=0
6. 顾客点差价菜品 → is_buffet_included=true, buffet_surcharge_cents=2800
7. 顾客点套餐外啤酒 → is_buffet_included=false, line_total_cents=原价
8. 提交到厨房 → submitted_order_items 保留 buffet 字段
9. 前端轮询 GET /sessions/buffet/status → 倒计时 → WARNING(黄) → OVERTIME(红)
10. 结账 → POST /tables/{tableId}/settlement/collect
    金额 = (168×4 + 98×1) + surchargeTotal + extraTotal + overtimeFee

**Alternative Flows:**

A1 超时:
  buffet_ends_at 过后，buffet_status → OVERTIME
  overtimeFee = (actualMinutes - graceMinutes) × feePerMinute
  超过 max_overtime_minutes → 通知强制结账

A2 限量菜品:
  buffet_package_items.max_qty_per_person = 2，4 人桌最多 8 份
  第 9 份 → 400 "每人限点 2 份，已点 8 份"

A3 套餐外菜品停售:
  前端显示"已售罄"，但不影响套餐内菜品点单

A4 自助餐 + 并台:
  A02 并入 A01（都是自助餐同档位）→ 结账时人数合并
  金额 = package.price × (A01.guestCount + A02.guestCount) + ...

**State Changes:**
| 表 | 字段 | 变化 |
|---|---|---|
| table_sessions | dining_mode | A_LA_CARTE → BUFFET |
| table_sessions | buffet_status | ACTIVE → WARNING → OVERTIME → ENDED |
| active_table_order_items | is_buffet_included | 按 inclusionType 设置 |
| submitted_order_items | is_buffet_included | 从 active 拷贝（toSubmittedItem 修复） |

**Touched Modules:** pos-buffet, pos-core, pos-catalog, pos-settlement, pos-kitchen

**APIs:**
- POST /api/v2/stores/{storeId}/tables/{tableId}/buffet/start
- GET /api/v2/stores/{storeId}/menu?diningMode=BUFFET&packageId=X
- GET /api/v2/stores/{storeId}/tables/{tableId}/buffet/status
- POST /api/v2/stores/{storeId}/tables/{tableId}/settlement/collect

**DDL Impact:** V074 (order items buffet), V075 (sessions buffet), V082-V085 (packages, items, menu)

**Done Criteria:**
- 选档位→自助餐菜单→区分免费/差价/套餐外→计时→超时计费→结账金额正确
- 差价和套餐外在 submitted_order_items 中可查
- 超时宽限期和最大超时正确执行

---

## J03 顾客外卖

**Actor:** 顾客（外卖平台）+ 厨房 + 配送员
**Precondition:** 外卖平台已对接（delivery_platform_configs ACTIVE）

**Trigger:** Grab/Foodpanda 推送外卖订单到 webhook

**Main Flow:**
1. 外卖平台 webhook → POST /api/v2/webhooks/delivery/{platform}
2. 创建 submitted_order（source_order_type=DELIVERY, dining_mode=DELIVERY）
   → external_platform, external_order_no 记录来源
3. 厨房收到 kitchen_ticket → KDS 显示（标记外卖图标）
4. 厨房制作 → ticket_status: SUBMITTED → PREPARING → READY
5. 打包 → delivery_status: READY_FOR_PICKUP
6. 骑手取餐 → delivery_status: PICKED_UP
7. 送达 → delivery_status: DELIVERED
8. 外卖平台回调结算 → settlement（扣除平台佣金）
9. 记录 external_integration_log（每次 webhook 出入都记）
10. order_channel_attribution 记录渠道归因

**Alternative Flows:**

A1 接单超时:
  webhook 收到后 5 分钟内未确认 → 平台自动取消
  → submitted_order.fulfillment_status = CANCELLED

A2 骑手取消:
  delivery_status → CANCELLED_BY_RIDER
  → 重新分配或商家自配

A3 平台 webhook 失败:
  external_integration_logs 记录 result_status=FAILED
  连续 3 次 → 告警通知

**State Changes:**
| 表 | 字段 | 变化 |
|---|---|---|
| submitted_orders | delivery_status | READY_FOR_PICKUP → PICKED_UP → DELIVERED |
| kitchen_tickets | ticket_status | SUBMITTED → PREPARING → READY |
| external_integration_logs | result_status | SUCCESS/FAILED |
| order_channel_attribution | channel_id | 关联外卖平台 channel |

**Touched Modules:** pos-integration, pos-kitchen, pos-core, pos-settlement

**APIs:**
- POST /api/v2/webhooks/delivery/{platform} (inbound)
- 各平台 API (outbound: 确认接单、状态更新)

**DDL Impact:** V092 (external_integration_logs), 现有 delivery_orders/submitted_orders

**Done Criteria:**
- 外卖订单从平台推送到厨房出单全流程
- 每次 API 调用都有 integration_log
- 渠道归因正确记录

---

## J04 顾客会员全流程

**Actor:** 顾客
**Precondition:** 无（新顾客注册 or 已有会员）

**Trigger:** 顾客在 QR 点单页点击"成为会员"

**Main Flow:**
1. 注册 → POST /api/v2/members/register { phone, name }
   → members(tier_code=STANDARD, member_status=ACTIVE), member_accounts(points=0, cash=0)
2. 查询会员信息 → GET /api/v2/members/{id}/profile
   → 返回: 等级、积分余额、储值余额、可用券数量、消费统计
3. 绑定到当前订单 → submitted_orders.member_id = member.id
4. 结账 → 按 points_rules 计算积分
   → member_accounts.points_balance += earned
   → member_points_ledger 记录
5. 充值储值 → POST /api/v2/members/{id}/recharge { amountCents, bonusCents }
   → member_accounts.cash_balance_cents += (amount + bonus)
   → member_recharge_orders 记录
6. 查询消费/充值记录 → GET /api/v2/members/{id}/transactions?type=ALL&page=1
   → 返回: 消费明细、充值记录、积分流水、券使用记录（分页）
7. 领券 → member_coupons(coupon_status=AVAILABLE)
8. 下次消费 → 支付叠加:
   a. 积分抵扣 → frozen_points += X → 确认后 points_balance -= X
   b. 用券 → coupon_status: AVAILABLE → LOCKED (CAS) → USED
   c. 储值支付 → frozen_cash += X → 确认后 cash_balance -= X
   d. 剩余走现金/卡
9. 消费累计达标 → tier_code 升级 (STANDARD → SILVER → GOLD)
10. 推荐好友 → 生成 referral_code → 好友注册时关联 → 双方获奖励积分
11. 注销账户 → POST /api/v2/members/{id}/deactivate
    → 校验: frozen_points=0 AND frozen_cash_cents=0（无冻结中的资产）
    → 储值余额 > 0 → 要求先退余额 POST /api/v2/members/{id}/refund-balance
      → 退回原充值渠道或现金，member_recharge_orders 记录退款
    → 积分清零 → member_points_ledger 记录(type=DEACTIVATION_CLEAR)
    → 所有 AVAILABLE 券 → coupon_status = CANCELLED
    → members.member_status = DEACTIVATED
    → member_accounts 软删除（保留数据 90 天供审计）

**Alternative Flows:**

A1 券过期:
  coupon_status=EXPIRED，结算时校验 → 400 "优惠券已过期"

A2 积分不足:
  points_balance - frozen_points < min_points_deduct
  → 跳过积分抵扣步骤

A3 储值不够 + 混合支付:
  储值扣完后 remaining > 0 → 走外部支付（现金/卡）

A4 券并发抢用:
  两个终端同时对同一张券做 LOCKED
  → CAS: UPDATE ... WHERE lock_version = :expected
  → 后者 affected_rows=0 → 400 "优惠券已被使用"

A5 注销时有冻结资产:
  frozen_points > 0 或 frozen_cash_cents > 0
  → 400 "您有进行中的交易，请完成后再注销"

A6 注销时储值退款失败:
  原充值渠道退款失败 → 标记 refund_status=FAILED
  → 提示到店现金退款，audit_trail 记录

A7 已注销会员重新注册:
  同一手机号 → 检测到 DEACTIVATED 记录
  → 创建全新 member（新 ID），不继承旧积分/等级
  → 旧记录保留供审计

**State Changes:**
| 表 | 字段 | 变化 |
|---|---|---|
| member_accounts | points_balance | +earned / -deducted / 注销清零 |
| member_accounts | frozen_points | +held / -confirmed or -released |
| member_accounts | cash_balance_cents | +recharged / -deducted / 注销退款 |
| member_accounts | frozen_cash_cents | +held / -confirmed or -released |
| member_coupons | coupon_status | AVAILABLE → LOCKED → USED (or back to AVAILABLE) / 注销 → CANCELLED |
| settlement_payment_holds | hold_status | HELD → CONFIRMED or RELEASED |
| members | member_status | ACTIVE → DEACTIVATED (注销) |

**Touched Modules:** pos-member, pos-settlement, pos-promotion

**APIs:**
- POST /api/v2/members/register
- GET /api/v2/members/{id}/profile
- GET /api/v2/members/{id}/transactions?type=ALL&page=1
- POST /api/v2/members/{id}/recharge
- POST /api/v2/members/{id}/refund-balance
- POST /api/v2/members/{id}/deactivate
- POST /api/v2/stores/{storeId}/tables/{tableId}/settlement/preview-stacking
- POST /api/v2/stores/{storeId}/tables/{tableId}/settlement/collect-stacking

**DDL Impact:** V076-V079 (stacking), V078 (frozen), V081 (coupon lock), V096 (member_status + deactivation)

**Done Criteria:**
- 注册→查询→积分→充值→查记录→用券→积分抵扣→储值支付全链路
- 查询接口返回完整会员画像（等级/积分/储值/券/消费统计）
- 消费/充值/积分流水可分页查询
- 冻结/确认/释放三态正确
- 券并发 CAS 不会重复使用
- 等级升级自动触发
- 注销前校验无冻结资产、储值退款、积分清零、券作废
- 已注销会员同手机号可重新注册（新 ID）

---

## J05 收银员日常

**Actor:** 收银员 (CASHIER)
**Precondition:** 用户已登录 POS，角色含 SHIFT_OPEN_CLOSE + SETTLEMENT_COLLECT

**Trigger:** 上班打卡

**Main Flow:**
1. 开班 → POST /api/v2/shifts/open { cashierId, initialCashCents }
   → cashier_shifts(shift_status=OPEN)
2. 顾客到店 → 收银员在 POS 开台 → table AVAILABLE → OCCUPIED
3. POS 点单 → active_table_order + items
4. 送厨 → submitted_order
5. 结账 → settlement/collect { paymentMethod: CASH, collectedAmountCents }
   → settlement_record 写入
   → cashier_shift_settlements 关联
6. 退款请求 → POST /api/v2/refunds { orderId, reason, amountCents }
   → 校验: amountCents <= role.max_refund_cents
   → refund_records 写入
   → audit_trail 记录 (risk_level=MEDIUM)
7. 关班 → POST /api/v2/shifts/close { actualCashCents }
   → 计算差异 = actualCash - (initialCash + cashCollected - cashRefunded)
   → cashier_shifts(shift_status=CLOSED)

**Alternative Flows:**

A1 支付失败换方式:
  VibeCash 支付 FAILED → 收银员选"换现金"
  → oldAttempt.attempt_status = REPLACED
  → 新建 attempt (CASH) → 完成结账

A2 退款超额:
  amountCents > role.max_refund_cents
  → 403 "退款金额超出权限，需经理审批"
  → audit_trail(requires_approval=true, approval_status=PENDING)

A3 现金差异:
  关班时 actual - expected 差异 > 阈值
  → 告警通知值班经理
  → 记录 audit_trail(risk_level=HIGH)

**State Changes:**
| 表 | 字段 | 变化 |
|---|---|---|
| cashier_shifts | shift_status | OPEN → CLOSED |
| payment_attempts | attempt_status | FAILED → REPLACED (换方式时) |
| refund_records | refund_status | APPROVED/PENDING |
| audit_trail | approval_status | PENDING → APPROVED/REJECTED |

**Touched Modules:** pos-core, pos-settlement, pos-auth, pos-ops(audit)

**APIs:**
- POST /api/v2/shifts/open
- POST /api/v2/shifts/close
- POST /api/v2/stores/{storeId}/tables/{tableId}/settlement/collect
- POST /api/v2/refunds
- POST /api/v2/payments/{attemptId}/switch-method

**DDL Impact:** V073 (audit_trail), V080 (payment retry)

**Done Criteria:**
- 开班→点单→结账→退款→关班全流程
- 退款权限校验正确
- 支付失败换方式能成功结账
- 关班差异计算准确

---

## J06 厨房日常

**Actor:** 厨师 (KDS_OPERATE)
**Precondition:** kitchen_stations 已配置，KDS 在线

**Trigger:** submitted_order 创建后系统生成 kitchen_ticket

**Main Flow:**
1. 订单提交 → 按 SKU 的 station_id 路由 → 拆分为多个 kitchen_ticket
2. KDS 显示新票 → ticket_status = SUBMITTED，显示桌号 + 菜品 + 备注
3. 厨师点"开始制作" → ticket_status → PREPARING, started_at = NOW()
4. 制作完成 → ticket_status → READY, ready_at = NOW()
5. 服务员上菜确认 → ticket_status → SERVED, served_at = NOW()
6. POS 前厅看到每个 round 的状态更新

**Alternative Flows:**

A1 KDS 离线 → 回退打印:
  last_heartbeat_at > 90s 前 → kds_health_status = OFFLINE
  fallback_mode = AUTO → 后续 ticket 自动走 fallback_printer_ip
  通知值班经理 "工作站 {name} KDS 离线，已切换打印机"

A2 KDS 恢复:
  收到心跳 → kds_health_status = ONLINE → 自动切回 KDS

A3 退单:
  ticket_status = SUBMITTED 或 PREPARING 时可取消
  → ticket_status = CANCELLED
  → 关联的 submitted_order_item 标记退单
  → 如果库存已扣减 → 触发库存回补

**State Changes:**
| 表 | 字段 | 变化 |
|---|---|---|
| kitchen_tickets | ticket_status | SUBMITTED → PREPARING → READY → SERVED |
| kitchen_stations | kds_health_status | ONLINE → OFFLINE → ONLINE |

**Touched Modules:** pos-kitchen, pos-core, pos-inventory(库存回补)

**APIs:**
- POST /api/v2/stations/{stationId}/heartbeat
- PUT /api/v2/kitchen-tickets/{ticketId}/status
- GET /api/v2/stores/{storeId}/kitchen-tickets?stationId=X&status=SUBMITTED

**DDL Impact:** V095 (kitchen_stations fallback)

**Done Criteria:**
- 订单按 station 路由正确拆票
- KDS 状态流转 SUBMITTED→PREPARING→READY→SERVED
- 离线自动回退打印，恢复自动切回
- 退单时库存回补

---

## J07 店长日常

**Actor:** 店长 (STORE_MANAGER)
**Precondition:** 已登录 pc-admin

**Trigger:** 每日上班

**Main Flow:**
1. 查看昨日报表 → GET /stores/{storeId}/reports/snapshot?type=DAILY_SUMMARY
   → metrics_json + AI 摘要
2. 菜单管理 → CRUD products/skus，设置 menu_modes，绑定时段
3. 配置自助餐档位 → CRUD buffet_packages + 绑定 SKU
4. 查看库存预警 → inventory_items WHERE current_stock < safety_stock
5. 审批库存驱动促销草案 → POST /inventory-promotions/{id}/approve
   → draft_status: DRAFT → APPROVED → 创建 promotion_rule
6. 排班 → employee_schedules CRUD
7. 处理异常:
   a. 审批大额退款 → audit_trail(approval_status: PENDING → APPROVED)
   b. 处理客诉 → customer_feedback(feedback_status: NEW → IN_PROGRESS → RESOLVED)

**Alternative Flows:**

A1 员工临时请假:
  leave_requests 创建 → 找替班 → 调整 employee_schedules

A2 库存预警:
  safety_stock 告警 → 查看 order_suggestions → 生成 purchase_order

**State Changes:**
| 表 | 字段 | 变化 |
|---|---|---|
| inventory_driven_promotions | draft_status | DRAFT → APPROVED |
| audit_trail | approval_status | PENDING → APPROVED/REJECTED |
| customer_feedback | feedback_status | NEW → IN_PROGRESS → RESOLVED |

**Touched Modules:** pos-catalog, pos-buffet, pos-inventory, pos-promotion, pos-report, pos-ops

**APIs:**
- GET /api/v2/stores/{storeId}/reports/snapshot
- CRUD /api/v2/stores/{storeId}/buffet-packages
- GET /api/v2/stores/{storeId}/inventory-promotions?status=DRAFT
- POST /api/v2/stores/{storeId}/inventory-promotions/{id}/approve
- POST /api/v2/stores/{storeId}/audit-logs/{id}/approve

**DDL Impact:** V082-V085 (buffet), V089 (promotions), V073 (audit), V091 (feedback)

**Done Criteria:**
- 报表 + AI 摘要可看
- 自助餐档位从创建到上线全流程
- 库存促销草案审批→创建促销规则
- 退款审批流程

---

## J08 库存全链路

**Actor:** 店长 + 库存员
**Precondition:** inventory_items 已建档，suppliers 已配置

**Trigger:** 供应商送货到店

**Main Flow:**
1. 拍照送货单 → POST /invoices/{id}/ocr-scan { imageAssetId }
   → ocr_status: PROCESSING → COMPLETED
   → ocr_raw_result 存 JSON
2. 员工复核 OCR 结果 → 修正匹配 → POST /invoices/{id}/ocr-confirm
   → purchase_invoice_items 写入
   → inventory_batches 创建（FIFO，含 expiry_date）
   → inventory_movements(PURCHASE)
   → inventory_items.current_stock += qty
3. 顾客下单 → 结账 → SOP 扣减:
   → recipes 查 SKU 的原料消耗
   → 含修饰符差异: 大份 × 1.5，加辣 + 辣椒 10g
   → inventory_batches FIFO 扣减（expiry_date ASC）
   → inventory_movements(SALE_DEDUCT)
4. 每日凌晨扫描 → 库存预警:
   current_stock < safety_stock → 生成 order_suggestions
5. 临期批次 → 生成 inventory_driven_promotions(DRAFT)
6. 月度盘点 → stocktake_tasks → 员工录入 counted_qty
   → variance_qty = counted - system → 审批 → 调整
7. 报损 → waste_records → 审批 → inventory_movements(WASTE)

**Alternative Flows:**

A1 送货单数量不符:
  OCR 识别 10kg，实际收到 8kg → 员工修正为 8kg
  → purchase_invoice_items.quantity = 8

A2 盘点差异大:
  variance_cost_cents > 阈值 → 需要店长审批
  → stocktake_tasks.task_status 停在 COMPLETED 等 APPROVED

A3 SOP 批量导入:
  上传 CSV → sop_import_batches(VALIDATING → VALIDATED → IMPORTING → COMPLETED)
  部分行无效 → error_details JSON 记录

**State Changes:**
| 表 | 字段 | 变化 |
|---|---|---|
| purchase_invoices | ocr_status | PROCESSING → COMPLETED |
| inventory_batches | remaining_qty | received_qty → 逐步减少 |
| inventory_items | current_stock | += purchase / -= sale_deduct / ± adjustment |
| inventory_driven_promotions | draft_status | DRAFT → APPROVED/REJECTED |

**Touched Modules:** pos-inventory, pos-promotion, pos-catalog(recipes)

**APIs:**
- POST /api/v2/stores/{storeId}/invoices/{id}/ocr-scan
- POST /api/v2/stores/{storeId}/invoices/{id}/ocr-confirm
- POST /api/v2/stores/{storeId}/sop/import
- GET /api/v2/stores/{storeId}/inventory-alerts

**DDL Impact:** V086 (recipes modifier), V087 (OCR), V088 (SOP import), V089 (promotions)

**Done Criteria:**
- 送货→OCR→入库→SOP 扣减→预警→盘点全链路
- FIFO 按 expiry_date 扣减
- 修饰符消耗差异（倍率+追加）正确
- 临期促销草案自动生成

---

## J09 老板视角

**Actor:** 老板 / 品牌管理者
**Precondition:** 已登录 pc-admin 或 platform-admin，有 REPORT_VIEW_ALL 权限

**Trigger:** 每天/每周查看经营状况

**Main Flow:**
1. 查看日报 → GET /stores/{storeId}/reports/snapshot?type=DAILY_SUMMARY
   → AI 摘要: "今日营收 $12,500，环比+15%，客单价 $86，翻台率 3.2"
   → ai_highlights: ["营收创周内新高", "自助餐占比30%"]
   → ai_warnings: ["牛腩库存仅剩2天", "A03桌翻台率低"]
   → ai_suggestions: ["推出午市自助套餐", "A03桌考虑调整座位"]
2. 多店对比 → POST /merchants/{merchantId}/reports/multi-store-compare
   → 各店营收/客单价/翻台率/人效排名
   → AI 对比摘要
3. 审批 AI 建议 → ai_recommendations → APPROVE/REJECT
4. 渠道分析 → channel_performance_daily → 各渠道 ROI
5. 审批大额退款/改价 → audit_trail → APPROVE

**Alternative Flows:**

A1 AI 建议被拒:
  老板 REJECT → ai_recommendations.approval_status = REJECTED
  → AI 学习反馈

A2 渠道 ROI 异常:
  某渠道佣金 > 营收 30% → ai_warnings 高亮
  → 建议调整佣金或停用渠道

**State Changes:**
| 表 | 字段 | 变化 |
|---|---|---|
| report_snapshots | ai_summary | 生成 |
| ai_recommendations | approval_status | PROPOSED → APPROVED/REJECTED |
| audit_trail | approval_status | PENDING → APPROVED/REJECTED |

**Touched Modules:** pos-report, pos-ai, pos-ops(audit)

**APIs:**
- GET /api/v2/stores/{storeId}/reports/snapshot
- POST /api/v2/merchants/{merchantId}/reports/multi-store-compare
- GET /api/v2/stores/{storeId}/audit-logs/pending-approval

**DDL Impact:** V094 (report_snapshots)

**Done Criteria:**
- 日报含 AI 摘要/亮点/警告/建议
- 多店横向对比可看
- 审批流程完整

---

## J10 财务月结

**Actor:** 财务 (FINANCE)
**Precondition:** 有 REPORT_FINANCE + SETTLEMENT_REFUND_LARGE 权限

**Trigger:** 月末

**Main Flow:**
1. 审批待处理退款 → audit_trail WHERE action='REFUND' AND approval_status='PENDING'
2. 渠道结算 → channel_settlement_batches
   → 汇总各渠道当月佣金
   → 生成结算单
3. GTO 税务导出 → gto_export_batches → 按 GST 9/109 提取
4. 薪资计算 → payroll_periods + payroll_records
   → 考勤 + 工时 + 加班 → 应发工资
5. 导出报表 → 财务报表 Excel

**Alternative Flows:**

A1 渠道结算争议:
  商家认为佣金计算有误 → 标记争议
  → channel_settlement_batches.batch_status = DISPUTED

A2 GTO 导出失败:
  数据不完整 → gto_export_batches.batch_status = FAILED
  → 重试 (retry 机制已有)

**State Changes:**
| 表 | 字段 | 变化 |
|---|---|---|
| audit_trail | approval_status | PENDING → APPROVED |
| channel_settlement_batches | batch_status | PENDING → SETTLED/DISPUTED |
| gto_export_batches | batch_status | PENDING → EXPORTED/FAILED |
| payroll_records | payroll_status | DRAFT → APPROVED → PAID |

**Touched Modules:** pos-settlement, pos-report, pos-integration, pos-ops

**APIs:**
- GET /api/v2/stores/{storeId}/audit-logs?action=REFUND&approvalStatus=PENDING
- POST /api/v2/merchants/{merchantId}/channel-settlements/generate
- POST /api/v2/stores/{storeId}/gto/export

**DDL Impact:** V073 (audit_trail), V092 (integration logs)

**Done Criteria:**
- 退款审批→渠道结算→GTO→薪资全链路
- 争议标记和重试机制

---

## J11 并台

**Actor:** 服务员
**Precondition:** 两桌都是 OCCUPIED，各有 OPEN session

**Trigger:** 两桌客人要合到一起坐

**Main Flow:**
1. 服务员在 POS 长按 A01 → 选"并台" → 选 A02
2. POST /stores/{storeId}/tables/merge { masterTableId: A01, mergedTableId: A02 }
3. A02.session.merged_into_session_id = A01.session.id
4. A02.table_status → MERGED
5. A02 的 active_table_order 保留不动，继续可从 A02 扫码点单
6. 提交到厨房 → A02 的 submitted_order 归 A02 的 session
7. 结账（在 A01 发起）:
   → sessionChain = [A01.session, A02.session]
   → 聚合所有 UNPAID submitted_orders
   → 统一结账
8. 结账后:
   → A01.table → PENDING_CLEAN
   → A02.table → AVAILABLE (被并桌直接释放)
   → 两个 session 都 → CLOSED

**Alternative Flows:**

A1 两桌都有会员:
  submitted_orders 中 member_id 不同
  → 结算取第一个非 null 的 member_id（和现有逻辑一致 line 112-116）
  → 积分归该会员

A2 两桌不同用餐模式:
  A01 = A_LA_CARTE, A02 = BUFFET → 允许并台
  → 结账时分别按各自模式计算:
    A01 部分 = sum(submitted_orders.payable_amount_cents)
    A02 部分 = buffet 金额计算
  → 合计后走支付

A3 未结账前拆台:
  POST /stores/{storeId}/tables/unmerge { mergeRecordId }
  → A02.merged_into_session_id = NULL
  → A02.table_status → OCCUPIED
  → 各桌独立结账

A4 已结账后不能拆台:
  A01.session.session_status = CLOSED → 400 "已结账不能拆台"

**State Changes:**
| 表 | 字段 | 变化 |
|---|---|---|
| table_sessions | merged_into_session_id | NULL → masterSession.id |
| store_tables | table_status | OCCUPIED → MERGED (被并桌) |
| table_merge_records | unmerged_at | NULL → timestamp (拆台时) |

**Touched Modules:** pos-core, pos-settlement, pos-buffet(混合模式时)

**APIs:**
- POST /api/v2/stores/{storeId}/tables/merge
- POST /api/v2/stores/{storeId}/tables/unmerge
- GET /api/v2/stores/{storeId}/tables/{tableId}/merge-info

**DDL Impact:** V070 (session merge), V071 (table status), V072 (merge_records)

**Done Criteria:**
- 并台后两桌订单统一结账，金额正确
- 拆台还原正确
- 混合模式（单点+自助）并台可计算
- 不动 active_table_orders 和 submitted_orders

---

## J12 预约→入座全链路

**Actor:** 顾客（预约）+ 服务员（候位/叫号）+ Walk-in 顾客
**Precondition:** 门店支持预约（reservations 表已有）

**Trigger:** 顾客通过 QR/电话/Google 预约，或 Walk-in 直接到店

**Main Flow（预约路径 — 创建即锁桌）:**
1. 创建预约 → POST /api/v2/stores/{storeId}/reservations
   { date, time, guestCount, contactName, contactPhone, source }
   → 系统按 guestCount 自动匹配合适桌台
   → reservations(status=CONFIRMED, table_id=指定桌)
   → store_tables.table_status: AVAILABLE → RESERVED
   → reservations.reserved_table_id = table.id
2. 预约时间到，顾客到店 → 服务员确认身份
3. 直接入座（桌已锁定）→ POST /api/v2/stores/{storeId}/reservations/{id}/seat
   → reservation.status: CONFIRMED → SEATED
   → store_tables.table_status: RESERVED → OCCUPIED
   → 创建 table_session
4. 正常点单流程（J01 或 J02）

**Main Flow（Walk-in 路径 — 候位队列）:**
5. Walk-in 顾客到店，无预约，当前无空桌
6. 取号 → POST /api/v2/stores/{storeId}/queue/take-number { guestCount }
   → queue_tickets(status=WAITING, queue_no=A001)
7. 有桌空出 → 叫号 → POST /api/v2/stores/{storeId}/queue/{ticketId}/call
   → queue_tickets.status: WAITING → CALLED
   → 广播叫号（屏幕/短信）
8. 顾客应答入座 → POST /api/v2/stores/{storeId}/queue/{ticketId}/seat
   → queue_tickets.status: CALLED → SEATED
   → 分配桌台 → table: AVAILABLE → OCCUPIED → session 创建
9. 正常点单流程（J01 或 J02）

**Alternative Flows:**

A1 预约未到（NO_SHOW）:
  超过预约时间 30 分钟 → reservation.status: CONFIRMED → NO_SHOW
  → store_tables.table_status: RESERVED → AVAILABLE（释放锁定的桌台）
  → 桌台可重新分配给 Walk-in 或其他预约

A2 预约时锁桌失败（无合适桌台）:
  所有符合人数的桌台在该时段已被 RESERVED 或 OCCUPIED
  → 400 "该时段暂无合适桌位"
  → 建议其他时段，或进入 WAIT_LISTED
  → reservations(status=WAIT_LISTED)，有桌释放时通知

A3 候位过号（Walk-in）:
  叫号 3 次未应答 → queue_tickets.status: CALLED → SKIPPED
  → 叫下一位
  → 过号的顾客可重新排队

A4 顾客取消预约:
  reservation.status: CONFIRMED → CANCELLED
  → store_tables.table_status: RESERVED → AVAILABLE（释放桌台）

A5 Google Reservation 同步:
  Google 推送预约 → webhook → 创建 reservations(source=GOOGLE)
  → 同样走锁桌逻辑（匹配桌台 → RESERVED）
  → external_integration_logs 记录

A6 预约桌台临时故障（如设备坏了）:
  store_tables.table_status → DISABLED
  → 系统自动为该预约重新分配桌台
  → 无可用桌 → 通知服务员人工处理

**State Changes:**
| 表 | 字段 | 变化 |
|---|---|---|
| reservations | status | CONFIRMED → SEATED / NO_SHOW / CANCELLED / WAIT_LISTED |
| reservations | reserved_table_id | 创建时绑定桌台 ID |
| store_tables | table_status | AVAILABLE → RESERVED（预约锁桌）→ OCCUPIED（入座） |
| store_tables | table_status | RESERVED → AVAILABLE（NO_SHOW/取消释放） |
| queue_tickets | status | WAITING → CALLED → SEATED / SKIPPED（仅 Walk-in） |

**Touched Modules:** pos-core(reservations, queue, tables), pos-integration(Google)

**APIs:**
- POST /api/v2/stores/{storeId}/reservations（创建即锁桌）
- POST /api/v2/stores/{storeId}/reservations/{id}/seat（入座）
- POST /api/v2/stores/{storeId}/reservations/{id}/cancel（取消释放桌）
- POST /api/v2/stores/{storeId}/queue/take-number（Walk-in 取号）
- POST /api/v2/stores/{storeId}/queue/{ticketId}/call（叫号）
- POST /api/v2/stores/{storeId}/queue/{ticketId}/seat（Walk-in 入座）

**DDL Impact:** 现有 reservations + queue_tickets 表（docs/66 已设计），reservations 加 reserved_table_id 字段

**Done Criteria:**
- 预约创建即锁桌，到店直接入座，无需等待
- Walk-in 无预约走候位队列，叫号→入座
- 预约锁桌失败时建议其他时段或进 WAIT_LIST
- NO_SHOW 超时释放桌台
- 取消预约自动释放桌台
- Google 预约同步也走锁桌逻辑
- 候位叫号→过号→重新排队（Walk-in only）

---

## 附录：Journey → Migration 交叉引用

| Journey | V070 | V071 | V072 | V073 | V074 | V075 | V076-V081 | V082-V085 | V086-V089 | V090-V093 | V094-V095 |
|---------|------|------|------|------|------|------|-----------|-----------|-----------|-----------|-----------|
| J01 单点堂食 | | ✅ | | ✅ | | | | | | ✅ | |
| J02 自助餐 | | | | | ✅ | ✅ | | ✅ | | | |
| J03 外卖 | | | | | | | | | | ✅ | |
| J04 会员 | | | | | | | ✅ | | | | |
| J05 收银员 | | | | ✅ | | | ✅ | | | | |
| J06 厨房 | | | | | | | | | | | ✅ |
| J07 店长 | | | | ✅ | | | | ✅ | ✅ | ✅ | ✅ |
| J08 库存 | | | | | | | | | ✅ | | |
| J09 老板 | | | | ✅ | | | | | | | ✅ |
| J10 财务 | | | | ✅ | | | | | | ✅ | |
| J11 并台 | ✅ | ✅ | ✅ | | ✅ | ✅ | ✅ | | | | |
| J12 预约 | | ✅ | | | | | | | | | |

---

*12 journeys complete. All aligned with final-executable-spec.*
