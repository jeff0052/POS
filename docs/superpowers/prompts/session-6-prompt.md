# Session 6 启动 Prompt

复制以下内容到新 Claude Code session：

---

## FounderPOS Phase 6: 联调收尾

项目路径：`/Users/ontanetwork/Documents/Codex`
技术栈：Java 17 + Spring Boot 3.3.3 + Spring Data JPA + MySQL 8 + Flyway + Maven Wrapper
编译命令：`JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./mvnw compile -q`
测试命令：`JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./mvnw test -Dtest=ClassName`（全量测试因 Mockito ByteBuddy hang 需单独跑）

### 背景

Phase 1-5 已全部完成并 merge 到 main：
- 1.1 RBAC + 1.2 审计 → 统一用户体系 + audit trail
- 2.1 订单引擎 + 2.2 支付叠加 + 2.3 退款/预约 → 完整交易链路
- 3.1 SKU/修饰符/时段 + 3.2 自助餐引擎 → 菜单定价体系
- 4.1 KDS 厨房票 + 4.2 库存/采购 → 厨房出单 + 进销存
- 5.x 会员/积分/储值/券/促销/渠道

现在进入 Phase 6 联调收尾，分两个 sub-session。

### Session 6.1 — 外卖对接 + AI 层

**范围（来自 roadmap `docs/84-implementation-plan-and-roadmap.md` lines 337-400）：**

1. **DeliveryWebhookController**
   - `POST /webhooks/delivery/{platform}` 接收外卖平台订单
   - 创建 `submitted_order(DELIVERY)` + `external_integration_logs` 追踪
   - HMAC 签名校验（参考 `PaymentCallbackController` 的 secure-by-default 模式）

2. **DeliveryStatusService**
   - 状态机：READY_FOR_PICKUP → PICKED_UP → DELIVERED
   - 与 kitchen ticket 联动

3. **ReportSnapshotService**
   - `generateDaily(storeId)` 定时任务
   - 计算 `metrics_json` + AI 生成摘要/建议
   - `report_snapshots` 表持久化

4. **MCP Tools 接真数据**
   - 现有 MCP read tools 连接真实 Repository

5. **AI Recommendation 审批**
   - `ai_recommendations` 状态机：PROPOSED → APPROVED/REJECTED

**验收标准：**
- 外卖 webhook → 订单创建 → 厨房出单
- AI 日报生成有意义内容
- MCP tools 返回真实数据

### Session 6.2 — 部署 + 前端联调 + 全链路测试

**范围：**

1. **E2E 集成测试**
   - 12 条 Journey (J01-J12) 主流覆盖
   - `@SpringBootTest` + TestContainers MySQL

2. **部署**
   - docker-compose.yml 更新
   - Flyway migration on AWS MySQL
   - Legacy 数据迁移脚本
   - Nginx 配置

3. **前端联调**
   - API 响应格式对齐
   - CORS 配置
   - Health check 端点

### 必须在 Phase 6 解决的跨 Session P1 遗留

以下 P1 来自 `docs/milestones/` 下的所有 milestone 文档，**必须在 Phase 6 关闭**：

| # | 问题 | 来源 | 建议时机 |
|---|------|------|----------|
| 1 | `startPayment()` (legacy) 仍是 `@Transactional` 包 HTTP call | Session 2.2 | 6.1 触碰 VibeCash 时顺带修 |
| 2 | Mockito ByteBuddy 阻塞全量测试（Homebrew JDK 17） | Session 2.2 | 6.2 开始前修，否则 E2E 无法跑 |
| 3 | `/api/v2/stores/**` broad permitAll 暴露风险 | Session 2.2 + 2.3 | 6.2 安全收口 |
| 4 | 外部支付退款 provider 调用未实现（只有 webhook 入口，无主动发起） | Session 2.3 | 6.1 DeliveryService 同期实现 |
| 5 | `refundItems` ownership 对普通 settlement 只是 best-effort（缺 `table_session_id` FK） | Session 2.3 | 6.1 DDL 补加 |
| 6 | 预约提醒未实现（只有过期 NO_SHOW） | Session 2.3 | 6.1 或独立小 task |

### 建议顺带解决的 P2 遗留

| # | 问题 | 来源 |
|---|------|------|
| 1 | `settlement_records` 缺 `table_session_id` FK（退款/报表追溯） | 2.2 + 2.3 |
| 2 | coupon template 无缓存（每次 calculateCouponDiscount 查 DB） | 2.2 |
| 3 | scheduler 超时硬编码（coupon 10min, settlement 30min） | 2.2 |
| 4 | `handleWebhook` 里 confirmStacking/releaseStacking 跑在 webhook 事务里 | 2.2 |
| 5 | 退款 item 金额仍是客户端传入，未校验 vs 实际单价 | 2.3 |
| 6 | KDS ticket cancel 不联动 submitted_order_items | 4.1 |

### 关键参考文件

- Roadmap: `docs/84-implementation-plan-and-roadmap.md`
- Session 2.2 Milestone: `docs/milestones/session-2.2-payment-stacking.md`
- Session 2.3 Milestone: `docs/milestones/session-2.3-refund-reservation.md`
- Session 4.1 Milestone: `docs/superpowers/archive/2026-03-29-session-4.1-milestone.md`
- Payment Stacking Design: `docs/superpowers/specs/2026-03-29-session-2.2-payment-stacking-design.md`
- SecurityConfig: `pos-backend/src/main/java/com/developer/pos/auth/security/SecurityConfig.java`
- VibeCashPaymentApplicationService: `pos-backend/src/main/java/com/developer/pos/v2/settlement/application/service/VibeCashPaymentApplicationService.java`

请先用 brainstorming skill 确认 scope，然后用 writing-plans skill 写实现计划。
