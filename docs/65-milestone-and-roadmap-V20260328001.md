# FounderPOS Milestone & Roadmap

**Version:** V20260328001
**Date:** 2026-03-28
**Status:** Internal Development

---

## 1. Version History

| Version | Date | Summary |
|---------|------|---------|
| V20260328001 | 2026-03-28 | Baseline: Aurora gap analysis, P0 scope locked, roadmap established |

---

## 2. Project Progress — What's Done

### Core Transaction (95%)
- Unified table order (POS + QR shared)
- Active table order lifecycle: DRAFT → SUBMITTED → PENDING_SETTLEMENT → SETTLED
- Table session management
- Table transfer + reservation
- Cashier settlement (cash)
- Refund flow with pessimistic locking
- Server-side price enforcement

### Catalog / SKU (85%)
- Products, categories, SKUs
- Attribute groups and modifiers
- Store-level availability
- Image upload system (local disk storage, merchant-scoped)

### Member / CRM (80%)
- Registration, search, member binding to orders
- Points earning + ledger
- Recharge with bonus
- Tier auto-upgrade (STANDARD → SILVER → GOLD → VIP)
- Settlement hook: auto points + tier evaluation

### Promotion (75%)
- Rule CRUD (AMOUNT_DISCOUNT, PERCENT_DISCOUNT, GIFT_SKU)
- Threshold conditions
- Usage limits
- Best-rule selection engine
- Promotion hit audit trail

### Staff / Permission (80%)
- Staff CRUD with PIN
- Role-based access (CASHIER, MANAGER, ADMIN, PLATFORM_ADMIN)
- SecurityConfig enforcement

### Cashier Shift (80%)
- Open/close shift
- Cash reconciliation
- Settlement recording per shift

### GTO Tax Export (70%)
- Batch generation
- GST 9/109 extraction
- Retry mechanism

### Reporting (60%)
- Daily summary
- Sales summary
- Member consumption report
- Order state monitoring

### Authentication (90%)
- JWT + BCrypt
- Real user table with bootstrap
- Role-based SecurityConfig
- AuthenticatedActor + AuthContext

### Platform Admin (70%)
- Dashboard (partial real data)
- Merchant hierarchy (Merchant → Brand → Country → Store)
- Stores, Devices, Users, Configurations, Monitoring pages

### Payment Microservice (50%)
- Separated as `pos-payment-service` submodule
- Cash adapter working
- VibeCash adapter skeleton
- Callback + HMAC verification
- External team handoff doc (docs/55)

### AI Layer (40%)
- MCP Tool Server: 14 tools across 6 domains
- AI Operator: 5 advisor roles + recommendation lifecycle
- Agent + Wallet: entity model + interaction routing

### Infrastructure
- Docker Compose production (8 containers)
- Nginx reverse proxy with CSP
- AWS EC2 deployment (54.237.230.5)
- CI pipeline (GitHub Actions, 6 jobs)
- Local git version control

---

## 3. Aurora Client Gap Analysis — P0 Requirements

Client: Aurora Restaurant (购物中心餐饮门店)
Business model: 堂食单点 + 堂食自助餐 + 平台外卖 + 自有外卖

### P0 Completion Matrix

| P0 Requirement | Our Status | Gap |
|----------------|-----------|-----|
| P0.1 桌台与扫码点单 | ✅ 100% | None |
| P0.2 三类菜单入口（自助/单点/外卖） | ⚠️ 30% | Only 单点 exists. 自助餐 and 外卖 menu modes missing |
| P0.3 自助餐核心规则 | ❌ 0% | Entirely new: 档位, 套餐内外, 差价, 计时 |
| P0.4 厨房路由（KDS + 工作站） | ❌ 0% | Design docs exist, code missing |
| P0.5 基础 CRM | ✅ 80% | Needs: 积分抵扣, 兑换 at checkout |
| P0.6 结账支付（多渠道） | ⚠️ 40% | Only cash. Visa/ApplePay/GooglePay/积分/储值/混合 missing |
| P0.7 库存基础链路 | ❌ 0% | Entirely new module: 原料, SOP, 送货单, 扣减, 预警 |
| P0.8 报表基础能力 | ⚠️ 50% | Needs: 客单价, 人效, 分时段, 堂食/外卖占比, 环比 |
| P0.9 外部对接（GTO/Grab/Google） | ⚠️ 20% | GTO skeleton. 商场CRM/Grab/Foodpanda/Google all missing |
| P0.10 推荐系统基础版 | ❌ 0% | Entirely new: 推荐位, 加购推荐, 爆款标签 |

### Gap Summary
- **2/10 P0 items at 80%+** (桌台点单, CRM)
- **4/10 P0 items at 0%** (自助餐, KDS, 库存, 推荐)
- **4/10 P0 items at 20-50%** (菜单入口, 支付, 报表, 外部对接)

---

## 4. Roadmap — Path to Aurora P0

### Phase 1: 自助餐模式 (Week 1-2)
**Priority: #1 — Client's core differentiator**

| Task | Description | Est |
|------|-------------|-----|
| Buffet package model | 档位表, 套餐-商品关联, 价格规则 | 3d |
| Menu mode routing | QR扫码选择"单点/自助", 不同菜单展示 | 2d |
| Buffet timer | 开始计时, 后台颜色提示, 超时预警 | 2d |
| 套餐外加点 | 额外收费商品, 差价计算 | 2d |
| 结账单据 | 套餐金额 + 加点金额 分开显示 | 1d |

### Phase 2: 厨房 KDS + 工作站路由 (Week 2-3)
**Priority: #2 — Without this kitchen can't function**

| Task | Description | Est |
|------|-------------|-----|
| 工作站 Station model | Station 表, SKU-Station 绑定 | 1d |
| Kitchen submission round | 送厨 round 模型, 状态流转 | 2d |
| KDS display page | 厨房端 Web 页: New → Preparing → Ready → Served | 3d |
| POS 厨房状态同步 | 前厅看到每 round 当前状态 | 1d |
| 打印路由 (stub) | 预留打印接口, 实际打印等硬件 | 1d |

### Phase 3: 库存基础链路 (Week 3-5)
**Priority: #3 — Client explicitly requires P0**

| Task | Description | Est |
|------|-------------|-----|
| 原料主数据 | inventory_items 表, CRUD | 2d |
| SOP 配方 | recipes 表, SKU→原料映射 | 2d |
| 销售扣减 | 结账 hook → SOP → 自动扣库存 | 2d |
| 送货单录入 | purchase_invoices 表, 手工录入 (OCR phase 2) | 2d |
| 库存预警 | 安全库存 + 低于阈值告警 | 1d |
| 订货建议 | 日终汇总 → 建议订货量 | 2d |

### Phase 4: 支付多渠道 (Week 4-5)
**Priority: #4 — External team leading, we provide integration points**

| Task | Description | Est |
|------|-------------|-----|
| VibeCash QR adapter | 外部团队实现 | External |
| DCS 刷卡 adapter | 外部团队 + DCS 厂商 | External |
| 积分抵扣支付 | 结账时选择积分抵扣 | 2d |
| 储值支付 | 结账时选择余额支付 | 2d |
| 混合支付 | 多种方式组合 | 2d |

### Phase 5: 推荐系统基础 (Week 5-6)
**Priority: #5 — Client says "第一轮就要"**

| Task | Description | Est |
|------|-------------|-----|
| 推荐位配置 | 首屏主推, 本月爆款, 高毛利标签 | 2d |
| 加购推荐 | 点单过程中关联推荐 | 2d |
| 会员偏好推荐 | 历史消费 → 推荐候选 | 2d |

### Phase 6: 报表深化 + 外部对接 (Week 6-7)
**Priority: #6**

| Task | Description | Est |
|------|-------------|-----|
| 报表补完 | 客单价, 人效, 分时段, 堂食/外卖占比, 环比 | 3d |
| 商场 CRM 对接 | API 集成 (需商场方提供接口文档) | 2d |
| Google Reservation | API 集成 | 2d |
| Google Review | API 集成 | 1d |
| Grab/Foodpanda | 外卖平台订单接入 | 5d |

### Phase 7: 生产加固 (Week 7-8)
**Priority: #7**

| Task | Description | Est |
|------|-------------|-----|
| SSL/HTTPS | Let's Encrypt + 域名 | 1d |
| 监控 / 告警 | 健康检查 + 异常通知 | 2d |
| 集成测试 | 核心路径自动化测试 | 3d |
| 备份 / 恢复 | MySQL 自动备份 | 1d |

---

## 5. Timeline Summary

| Week | Phase | Deliverable |
|------|-------|-------------|
| W1-2 | 自助餐模式 | 扫码选模式, 档位选择, 计时, 加点, 结账 |
| W2-3 | Kitchen KDS | 工作站路由, KDS 页面, 前厅同步 |
| W3-5 | 库存链路 | 原料, SOP, 扣减, 送货单, 预警 |
| W4-5 | 支付 (parallel) | 积分/储值支付, 外部团队做卡/QR |
| W5-6 | 推荐系统 | 推荐位, 加购, 偏好 |
| W6-7 | 报表 + 对接 | 报表补完, 商场/Google/外卖平台 |
| W7-8 | 生产加固 | SSL, 监控, 测试, 备份 |

**Total: ~8 weeks to Aurora P0 completion**

---

## 6. P1/P2 (Post-Launch)

| Phase | Items |
|-------|-------|
| P1 | 候位取号, 自有外卖+LalaMove, WhatsApp/Email 营销, 评价体系, 智能排班, CCTV AI |
| P2 | 总部-门店订货, 供应商比价, 社交媒体自动运营, AI Agent 深化 |
| AI Layer | MCP → AI Operator 接 LLM → Agent Protocol → Restaurant Network → RWA |

---

## 7. Versioning Convention

Format: `V{YYYYMMDD}{NNN}`

- `YYYYMMDD` = date
- `NNN` = zero-padded sequence within the day (001, 002, ...)

Examples:
- `V20260328001` — first version on March 28
- `V20260328002` — second update same day
- `V20260329001` — first version on March 29
