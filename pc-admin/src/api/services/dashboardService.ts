import { apiGet } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { DashboardSummary, Order } from "../../types";

interface OrderListResponse {
  list: Array<{
    id: number;
    orderNo: string;
    paidAmountCents: number;
    orderStatus: "PENDING" | "PAID" | "REFUNDED";
    paymentMethod?: "CASH" | "SDK_PAY";
    createdAt: number | string;
    cashier?: string;
    printStatus?: "PRINT_SUCCESS" | "PRINT_FAILED" | "NOT_PRINTED";
  }>;
  total: number;
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  if (USE_MOCK_API) {
    return mockApi.getDashboardSummary();
  }

  const summary = await apiGet<{
    totalRevenueCents: number;
    orderCount: number;
    refundAmountCents: number;
    cashAmountCents: number;
    sdkPayAmountCents: number;
  }>("/reports/daily-summary?storeId=1001&date=2026-03-20");

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

  const response = await apiGet<OrderListResponse>("/orders?storeId=1001&page=1&pageSize=10");
  return response.list.map((item) => ({
    id: item.id,
    orderNo: item.orderNo,
    amount: centsToText(item.paidAmountCents),
    status: item.orderStatus,
    payment: item.paymentMethod ?? "SDK_PAY",
    time: String(item.createdAt),
    cashier: item.cashier ?? "-",
    printStatus: item.printStatus ?? "NOT_PRINTED",
    items: []
  }));
}

function centsToText(value: number) {
  return `CNY ${(value / 100).toFixed(2)}`;
}
