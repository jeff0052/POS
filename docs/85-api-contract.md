# FounderPOS V3 — API Contract

**Version:** V20260328023
**Date:** 2026-03-28
**Status:** DRAFT
**Layer:** 1（实现基线）
**约定：** 所有端点前缀 `/api/v2/`，除 QR 扫码入口（公开）

---

## 通用约定

### 认证
- Header: `Authorization: Bearer {jwt}`
- QR 点单: `X-Ordering-Token: {ordering-jwt}`
- 公开端点: `GET /qr/...` 无需认证

### 响应格式

> **与现有代码 `ApiResponse<T>` 对齐**（见 `com.developer.pos.common.response.ApiResponse`）

成功：
```json
{
  "code": 0,
  "message": "ok",
  "data": { ... }
}
```

失败：
```json
{
  "code": 40001,
  "message": "优惠券已被使用",
  "data": null
}
```

**错误码约定:**
- `0` = 成功
- `400xx` = 客户端错误（参数校验、业务规则）
- `401xx` = 认证错误
- `403xx` = 权限不足
- `404xx` = 资源不存在
- `500xx` = 服务端错误

### 金额
- 所有金额字段以 `_cents` 结尾，类型 `long`
- 示例: `payableAmountCents: 12800` = $128.00

### 分页
```
GET /api/v2/...?page=0&size=20&sort=createdAt,desc
```

---

## 1. 认证 Auth

### POST /api/v2/auth/login
```
Request:  { username, password }
Response: { token, userId, displayName, permissions: ["SHIFT_OPEN_CLOSE", ...], stores: [{ storeId, storeName }] }
```

### POST /api/v2/auth/pin-login
```
Request:  { storeId, pinCode }
Response: { token, userId, displayName, permissions, currentShiftId }
```

### GET /api/v2/auth/me
```
Response: { userId, displayName, roles, permissions, stores }
```

---

## 2. 桌台 Tables

### GET /api/v2/stores/{storeId}/tables
```
Response: [{ tableId, tableCode, tableName, area, capacity, tableStatus, currentSessionId, mergedInto }]
```

### POST /api/v2/stores/{storeId}/tables/merge
```
Request:  { masterTableId, mergedTableId }
Response: { mergeRecordId, masterSessionId }
权限: TABLE_MERGE
```

### POST /api/v2/stores/{storeId}/tables/unmerge
```
Request:  { mergeRecordId }
Response: { success }
权限: TABLE_MERGE
```

### GET /api/v2/stores/{storeId}/tables/{tableId}/merge-info
```
Response: { isMerged, mergeRecordId, masterTableId, mergedTables: [{ tableId, tableName, guestCount }] }
```

### POST /api/v2/stores/{storeId}/tables/{tableId}/mark-clean
```
Response: { tableStatus: "AVAILABLE", newQrToken }
权限: TABLE_CLEAN
```

### POST /api/v2/stores/{storeId}/tables/{tableId}/qr/refresh
```
Response: { token, expiresAt, qrUrl }
权限: TABLE_MANAGE
```

---

## 3. QR 扫码点单

### GET /qr/{storeId}/{tableId}/{token} (公开)
```
验证 token → 颁发 ordering JWT → 302 重定向 /ordering?token={jwt}
JWT payload: { storeId, tableId, sessionId (nullable), exp }
```

### GET /api/v2/stores/{storeId}/menu
```
Header: X-Ordering-Token: {jwt}
Query:  diningMode=A_LA_CARTE|BUFFET|DELIVERY, packageId (optional), timeSlotId (optional)
Response: {
  categories: [{
    categoryId, categoryName, products: [{
      productId, productName, imageUrl, skus: [{
        skuId, skuCode, skuName, priceCents, modifierGroups: [{
          groupId, groupName, selectionType, isRequired, minSelect, maxSelect,
          options: [{ optionId, optionName, priceAdjustmentCents }]
        }],
        // buffet 模式额外字段
        buffetInclusionType, buffetSurchargeCents, maxQtyPerPerson
      }]
    }]
  }]
}
```

### POST /api/v2/qr-ordering/{storeId}/{tableId}/submit
```
Header: X-Ordering-Token: {jwt}
Request: {
  items: [{
    skuId, quantity, itemRemark,
    selectedOptions: [{ optionId }],
    isBuffetIncluded, buffetSurchargeCents, buffetInclusionType
  }]
}
Response: { submittedOrderId, orderNo, kitchenTicketIds: [...] }
```

---

## 4. 自助餐 Buffet

### POST /api/v2/stores/{storeId}/tables/{tableId}/buffet/start
```
Request:  { packageId, guestCount, childCount }
Response: { sessionId, buffetStartedAt, buffetEndsAt, buffetStatus: "ACTIVE" }
权限: BUFFET_START
```

### GET /api/v2/stores/{storeId}/tables/{tableId}/buffet/status
```
Response: {
  buffetStatus: "ACTIVE|WARNING|OVERTIME",
  startedAt, endsAt, remainingMinutes,
  overtimeMinutes, overtimeFeeCents,
  guestCount, childCount,
  packageName, packagePriceCents
}
```

### GET /api/v2/stores/{storeId}/buffet-packages
```
Response: [{ packageId, packageCode, packageName, priceCents, durationMinutes, overtimeFeePerMinuteCents }]
权限: MENU_MANAGE（管理端）/ 无（顾客端只返回 ACTIVE）
```

### POST /api/v2/stores/{storeId}/buffet-packages (管理端)
```
Request:  { packageCode, packageName, priceCents, durationMinutes, ... }
权限: MENU_MANAGE
```

---

## 5. 结算 Settlement

### POST /api/v2/stores/{storeId}/tables/{tableId}/settlement/preview-stacking
```
Request:  { usePoints: true, useCash: true, couponIds: [1, 2], paymentMethod: "CASH" }
Response: {
  totalPayableCents,
  breakdown: {
    promotionDiscountCents,
    couponDiscountCents,
    pointsDeductCents,
    cashBalanceDeductCents,
    externalPaymentCents
  },
  frozenPreview: { pointsToFreeze, cashToFreezeCents, couponsToLock: [...] },
  orders: [{ orderId, orderNo, payableAmountCents, items: [...] }]
}
不冻结，只预览。
```

### POST /api/v2/stores/{storeId}/tables/{tableId}/settlement/collect-stacking
```
Request:  { usePoints: true, useCash: true, couponIds: [1, 2], paymentMethod: "VIBECASH", collectedAmountCents }
Response: {
  settlementId, settlementNo,
  holdIds: [...],
  paymentAttemptId (如果是外部支付),
  checkoutUrl (VibeCash QR 场景)
}
冻结积分/储值 + 锁券 + 创建 holds。如果是现金，直接 confirm。
权限: SETTLEMENT_COLLECT
```

### POST /api/v2/stores/{storeId}/tables/{tableId}/settlement/collect
```
简化版（不叠加，纯现金/纯卡）:
Request:  { paymentMethod: "CASH", collectedAmountCents }
Response: { settlementId, settlementNo }
权限: SETTLEMENT_COLLECT
```

### POST /api/v2/payments/{attemptId}/switch-method
```
Request:  { newPaymentMethod: "CASH", collectedAmountCents }
Response: { newAttemptId, oldAttemptStatus: "REPLACED" }
权限: SETTLEMENT_COLLECT
```

---

## 6. 退款 Refund

### POST /api/v2/refunds
```
Request:  { settlementId, refundAmountCents, reason, refundItems: [{ itemId, quantity }] }
Response: { refundId, refundNo, refundStatus }
权限: REFUND_SMALL (≤ 阈值) 或 REFUND_LARGE（需审批）
审计: risk_level=MEDIUM, requires_approval=(amount > role.maxRefundCents)
```

---

## 7. 班次 Shifts

### POST /api/v2/shifts/open
```
Request:  { initialCashCents }
Response: { shiftId, openedAt }
权限: SHIFT_OPEN_CLOSE
```

### POST /api/v2/shifts/{shiftId}/close
```
Request:  { actualCashCents }
Response: { shiftId, expectedCashCents, actualCashCents, differenceCents, closedAt }
权限: SHIFT_OPEN_CLOSE
```

### GET /api/v2/shifts/current
```
Response: { shiftId, status, openedAt, totalSalesCents, totalRefundsCents, transactionCount }
```

---

## 8. 厨房 Kitchen

### POST /api/v2/stations/{stationId}/heartbeat
```
Response: { kdsHealthStatus: "ONLINE", lastHeartbeatAt }
```

### PUT /api/v2/kitchen-tickets/{ticketId}/status
```
Request:  { newStatus: "PREPARING|READY|SERVED|CANCELLED" }
Response: { ticketId, ticketStatus, updatedAt }
权限: KDS_OPERATE
```

### GET /api/v2/stores/{storeId}/kitchen-tickets
```
Query: stationId, status, limit
Response: [{ ticketId, ticketNo, tableCode, roundNumber, ticketStatus, items: [...], submittedAt }]
```

---

## 9. 库存 Inventory

### POST /api/v2/stores/{storeId}/invoices/{invoiceId}/ocr-scan
```
Request:  { imageAssetId }
Response: { invoiceId, ocrStatus: "PROCESSING" }
异步处理，完成后 ocrStatus → COMPLETED。
权限: INVENTORY_MANAGE
```

### POST /api/v2/stores/{storeId}/invoices/{invoiceId}/ocr-confirm
```
Request:  { items: [{ inventoryItemId, quantity, unit, unitPriceCents }] }
Response: { batchesCreated: [...], stockUpdated: true }
权限: INVENTORY_MANAGE
```

### POST /api/v2/stores/{storeId}/sop/import
```
Request:  multipart/form-data { file: CSV }
Response: { batchId, batchStatus: "VALIDATING", totalRows }
权限: INVENTORY_MANAGE
```

### GET /api/v2/stores/{storeId}/inventory-alerts
```
Response: [{
  inventoryItemId, itemName, currentStock, safetyStock,
  alertType: "LOW_STOCK|NEAR_EXPIRY",
  suggestedOrderQty, estimatedCostCents
}]
```

---

## 10. 会员 Members

### POST /api/v2/members/register
```
Request:  { phone, name, referralCode (optional) }
Response: { memberId, memberNo, tierCode: "STANDARD" }
```

### GET /api/v2/members/{memberId}/profile
```
Response: { memberId, name, phone, tierCode, pointsBalance, availablePoints, cashBalanceCents, availableCashCents, lifetimeSpendCents, totalVisitCount }
```

### POST /api/v2/members/{memberId}/recharge
```
Request:  { amountCents, campaignId (optional) }
Response: { rechargeOrderNo, amountCents, bonusCents, bonusPoints, newCashBalanceCents }
权限: MEMBER_RECHARGE
```

---

## 11. 预约与候位 Reservations

### POST /api/v2/stores/{storeId}/reservations
```
Request:  { date, time, guestCount, contactName, contactPhone, source, notes }
Response: { reservationId, reservationNo, reservationStatus: "CONFIRMED", tableId (if assigned) }
```

### POST /api/v2/stores/{storeId}/reservations/{reservationId}/seat
```
Request:  { tableId }
Response: { sessionId, tableStatus: "OCCUPIED" }
权限: RESERVATION_MANAGE
```

### POST /api/v2/stores/{storeId}/queue/take-number
```
Request:  { guestCount, phone (optional) }
Response: { ticketId, ticketNo, queuePosition, estimatedWaitMinutes }
```

### POST /api/v2/stores/{storeId}/queue/{ticketId}/call
```
Response: { ticketStatus: "CALLED", calledCount }
权限: QUEUE_MANAGE
```

### POST /api/v2/stores/{storeId}/queue/{ticketId}/seat
```
Request:  { tableId }
Response: { sessionId, tableStatus: "OCCUPIED" }
权限: QUEUE_MANAGE
```

---

## 12. 渠道与外卖 Channels

### POST /api/v2/webhooks/delivery/{platform} (Webhook, 无认证/平台签名验证)
```
Request:  平台 specific payload
Response: { received: true }
记录 external_integration_logs。
```

### POST /api/v2/merchants/{merchantId}/channel-settlements/generate
```
Request:  { channelId, periodStart, periodEnd }
Response: { batchId, batchNo, totalCommissionCents }
权限: CHANNEL_SETTLEMENT
```

---

## 13. 报表 Reports

### GET /api/v2/stores/{storeId}/reports/snapshot
```
Query: type=DAILY_SUMMARY, date=2026-03-28
Response: {
  metricsJson: { revenueCents, orderCount, avgTicketCents, tableTurnoverRate, ... },
  aiSummary, aiHighlights: [...], aiWarnings: [...], aiSuggestions: [...]
}
```

### POST /api/v2/merchants/{merchantId}/reports/multi-store-compare
```
Request:  { storeIds: [...], dateRange: { from, to } }
Response: { stores: [{ storeId, storeName, revenueCents, orderCount, ... }], aiCompareSummary }
权限: REPORT_VIEW_ALL
```

---

## 14. 审计 Audit

### GET /api/v2/stores/{storeId}/audit-logs
```
Query: action, riskLevel, approvalStatus, page, size
Response: [{ trailNo, actorName, action, targetType, targetId, riskLevel, approvalStatus, createdAt }]
权限: AUDIT_VIEW
```

### POST /api/v2/stores/{storeId}/audit-logs/{trailId}/approve
```
Request:  { decision: "APPROVED|REJECTED", note }
Response: { trailId, approvalStatus, approvedAt }
权限: AUDIT_APPROVE
```

---

## 15. 顾客反馈 Feedback

### POST /api/v2/stores/{storeId}/feedback
```
Request:  { sessionId, overallRating, foodRating, serviceRating, content, memberIdOptional }
Response: { feedbackId, feedbackNo }
公开（QR 点单用户可提交）或已登录会员。
```

---

## 16. 外卖对接日志 Integration

### GET /api/v2/stores/{storeId}/integration-logs
```
Query: platform, resultStatus, dateFrom, dateTo, page, size
Response: [{ logId, platform, direction, endpoint, resultStatus, durationMs, createdAt }]
权限: INTEGRATION_VIEW
```

---

## API 端点总览

| 模块 | 端点数 | 需要认证 |
|------|--------|---------|
| Auth | 3 | 部分 |
| Tables | 6 | 全部 |
| QR Ordering | 3 | QR JWT |
| Buffet | 4 | 混合 |
| Settlement | 4 | 全部 |
| Refund | 1 | 全部 |
| Shifts | 3 | 全部 |
| Kitchen | 3 | 全部 |
| Inventory | 4 | 全部 |
| Members | 3 | 混合 |
| Reservations | 5 | 混合 |
| Channels | 2 | 混合 |
| Reports | 2 | 全部 |
| Audit | 2 | 全部 |
| Feedback | 1 | 公开 |
| Integration | 1 | 全部 |
| **Total** | **47** | |
