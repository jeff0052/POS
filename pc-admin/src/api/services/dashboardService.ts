import { apiGetV2 } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { DashboardSummary, MemberConsumptionSummary, Order, SalesReportSummary } from "../../types";

// Aligned with backend MerchantAdminOrderDto
type RecentOrderResponseItem = {
  orderId: string;
  orderNo: string;
  storeId: number;
  tableId: number;
  tableCode?: string;
  orderType?: string;
  orderStatus: string;
  paymentMethod?: string;
  createdAt: string;
  memberName?: string;
  memberTier?: string;
  originalAmountCents: number;
  memberDiscountCents: number;
  promotionDiscountCents: number;
  payableAmountCents: number;
  giftItems?: string[];
};

export async function getDashboardSummary(): Promise<DashboardSummary> {
  if (USE_MOCK_API) {
    return mockApi.getDashboardSummary();
  }

  const summary = await apiGetV2<{
    totalRevenueCents: number;
    orderCount: number;
    refundAmountCents: number;
    cashAmountCents: number;
    sdkPayAmountCents: number;
  }>("/reports/daily-summary?storeId=101");

  return {
    revenue: centsToText(summary.totalRevenueCents),
    orders: String(summary.orderCount),
    refunds: centsToText(summary.refundAmountCents),
    cashiers: "0"
  };
}

export async function getRecentOrders(): Promise<Order[]> {
  if (USE_MOCK_API) {
    return mockApi.getOrders();
  }

  const response = await apiGetV2<RecentOrderResponseItem[]>("/admin/orders?storeId=1");
  return response.map((item) => ({
    id: item.orderId ?? item.orderNo,
    orderNo: item.orderNo,
    amount: centsToText(item.payableAmountCents),
    status: item.orderStatus,
    payment: item.paymentMethod ?? "UNPAID",
    time: item.createdAt,
    cashier: "-",
    printStatus: "NOT_PRINTED" as const,
    items: [],
    tableCode: item.tableCode,
    orderType: item.orderType,
    memberName: item.memberName,
    memberTier: item.memberTier,
    originalAmount: centsToText(item.originalAmountCents),
    memberDiscount: centsToText(item.memberDiscountCents),
    promotionDiscount: centsToText(item.promotionDiscountCents),
    payableAmount: centsToText(item.payableAmountCents),
    giftItems: item.giftItems ?? []
  }));
}

export async function getSalesReportSummary(): Promise<SalesReportSummary> {
  if (USE_MOCK_API) {
    return mockApi.getSalesReportSummary();
  }

  const summary = await apiGetV2<{
    totalSalesCents: number;
    totalDiscountCents: number;
    memberSalesCents: number;
    rechargeSalesCents: number;
    tableTurnoverRate: number;
    pendingGtoBatches: number;
  }>("/reports/sales-summary?storeId=101&merchantId=1");

  return {
    sales: centsToText(summary.totalSalesCents),
    discounts: centsToText(summary.totalDiscountCents),
    memberSales: centsToText(summary.memberSalesCents),
    rechargeSales: centsToText(summary.rechargeSalesCents),
    tableTurnover: String(summary.tableTurnoverRate),
    pendingGtoBatches: String(summary.pendingGtoBatches)
  };
}

export async function getMemberConsumptionSummary(): Promise<MemberConsumptionSummary> {
  if (USE_MOCK_API) {
    return {
      totalMemberSales: "SGD 0.00",
      totalMemberDiscounts: "SGD 0.00",
      memberOrderCount: "0",
      activeMemberCount: "0",
      totalRecharge: "SGD 0.00",
      totalBonus: "SGD 0.00",
      rechargeOrderCount: "0",
      averageRecharge: "SGD 0.00",
      topMembers: []
    };
  }

  const summary = await apiGetV2<{
    overview: {
      totalMemberSalesCents: number;
      totalMemberDiscountCents: number;
      memberOrderCount: number;
      activeMemberCount: number;
      totalRechargeCents: number;
      totalBonusCents: number;
      rechargeOrderCount: number;
      averageRechargeCents: number;
    };
    topMembers: Array<{
      memberId: number;
      memberName: string;
      tierCode: string;
      orderCount: number;
      totalSalesCents: number;
      totalRechargeCents: number;
      memberDiscountCents: number;
    }>;
  }>("/reports/member-consumption-summary?storeId=101&merchantId=1");

  return {
    totalMemberSales: centsToText(summary.overview.totalMemberSalesCents),
    totalMemberDiscounts: centsToText(summary.overview.totalMemberDiscountCents),
    memberOrderCount: String(summary.overview.memberOrderCount),
    activeMemberCount: String(summary.overview.activeMemberCount),
    totalRecharge: centsToText(summary.overview.totalRechargeCents),
    totalBonus: centsToText(summary.overview.totalBonusCents),
    rechargeOrderCount: String(summary.overview.rechargeOrderCount),
    averageRecharge: centsToText(summary.overview.averageRechargeCents),
    topMembers: summary.topMembers.map((item) => ({
      memberId: item.memberId,
      memberName: item.memberName,
      tierCode: item.tierCode,
      orderCount: item.orderCount,
      totalSales: centsToText(item.totalSalesCents),
      totalRecharge: centsToText(item.totalRechargeCents),
      memberDiscount: centsToText(item.memberDiscountCents)
    }))
  };
}

function centsToText(value: number) {
  return `SGD ${(value / 100).toFixed(2)}`;
}
