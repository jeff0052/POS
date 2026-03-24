# Market Requirements Document (MRD)

## 1. Document Purpose

本 MRD 用于明确 Restaurant POS 的市场需求、目标客户、业务问题、竞争差异与产品机会，为产品规划和商业推进提供依据。

## 2. Market Background

中小型餐饮商户数字化需求正在从“收银工具”升级为“交易 + 会员 + 经营 + 对账”的组合需求。

传统 POS 常见问题：

- 只解决收银，不解决扫码点餐与桌台协同
- 会员能力弱，无法支撑积分、充值、等级与专属权益
- 促销规则散，活动结果无法沉淀到报表
- 与商场 GTO 等外部数据系统对接困难
- 前台点单、顾客扫码点餐、收银结账各自独立，订单容易断裂

## 3. Target Market

### 3.1 Primary Market

- 购物中心内餐饮商户
- 中小连锁餐饮品牌
- 单店或区域型餐饮经营者

### 3.2 Secondary Market

- 茶饮
- 咖啡
- 快餐
- 轻食
- 烘焙

## 4. Target Customers

### 4.1 Merchant Type

- 需要桌台经营的堂食餐饮
- 需要会员运营能力的品牌门店
- 需要和商场或第三方系统做销售同步的商户

### 4.2 Internal Users

- cashier
- 服务员
- 店长
- 运营负责人
- 品牌老板

## 5. Core Market Problems

### Problem 1: QR Ordering and POS Ordering Are Split

顾客扫码点餐和前台点餐经常属于两套系统，导致：

- 一桌多单
- 菜品冲突
- 收银困难
- 订单状态混乱

### Problem 2: Membership Is Too Shallow

很多 POS 只支持“手机号识别会员”，但无法真正支撑：

- 积分
- 充值
- 余额消费
- 等级体系
- 会员专属价
- 升级规则

### Problem 3: Promotions Are Not Structured

门店常见活动包括：

- 满减
- 满赠
- 会员价
- 充值赠送

但多数系统没有标准规则中心，导致：

- 结账不透明
- 报表口径不清
- 优惠让利无法复盘

### Problem 4: Mall Data Sync Is Hard

购物中心对品牌门店常有日结数据同步要求，但传统系统常出现：

- 数据格式不标准
- 口径不一致
- 没有批次概念
- 失败重试困难

## 6. Market Requirements

市场对本项目的一期要求主要集中在以下四类：

### 6.1 Unified Table Order

必须具备：

- 一桌一张活动订单
- 顾客扫码与 POS 共用订单
- cashier 统一结账

### 6.2 Operational CRM

必须具备：

- 会员识别
- 积分
- 充值
- 等级
- 权益
- 会员专属价

### 6.3 Auditable Promotions

必须具备：

- 规则配置
- 订单命中结果可追溯
- 报表可统计优惠影响

### 6.4 Mall-Oriented Reporting

必须具备：

- 日结
- 销售汇总
- 优惠汇总
- 退款汇总
- GTO 批次同步

## 7. Competitive Opportunity

Restaurant POS 的差异机会不在于“再做一个普通收银台”，而在于：

- 用统一桌台活动订单打通顾客侧和前台侧
- 把 CRM 做成真正可运营能力，而不是附属字段
- 把促销和报表做成有规则、有留痕、可同步
- 从一开始考虑商场 GTO 同步

## 8. Market Entry Strategy

一期建议优先切入以下门店类型：

- 有堂食桌台
- 已经开始做会员
- 有扫码点餐需求
- 被商场要求定期同步销售数据

这类客户的痛点集中、预算明确、上线价值明显。

## 9. Success Criteria

从市场角度看，一期成功至少应满足：

- 顾客可扫码点单
- cashier 可统一结账
- 一桌一单模型稳定
- 会员体系可运营
- 满减/满赠/会员价可配置
- 销售报表和 GTO 同步可落地

## 10. Phase 1 Market Focus

一期市场主张建议为：

**“面向购物中心与中小餐饮门店的一体化桌台点单、会员经营与结账同步系统。”**

不要在一期市场表达中扩展到：

- 全业态零售
- 总部级大型中台
- 复杂供应链
- 全渠道营销自动化
