import { apiGet } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { Order } from "../../types";

interface OrderListResponse {
  list: Array<{
    id: number;
    orderNo: string;
    paidAmountCents: number;
    originalAmountCents?: number;
    memberDiscountCents?: number;
    promotionDiscountCents?: number;
    payableAmountCents?: number;
    orderStatus: "PENDING" | "PAID" | "REFUNDED" | "PENDING_SETTLEMENT";
    paymentMethod?: "CASH" | "SDK_PAY" | "UNPAID";
    createdAt: number | string;
    tableCode?: string;
    orderType?: "POS" | "QR";
    memberName?: string;
    memberTier?: string;
    giftItems?: string[];
  }>;
}

export async function getOrders(): Promise<Order[]> {
  if (USE_MOCK_API) {
    return mockApi.getOrders();
  }

  const response = await apiGet<OrderListResponse>("/orders?storeId=1001&page=1&pageSize=50");
  return response.list.map((item) => ({
    id: item.id,
    orderNo: item.orderNo,
    amount: `CNY ${((item.payableAmountCents ?? item.paidAmountCents) / 100).toFixed(2)}`,
    status: item.orderStatus,
    payment: item.paymentMethod ?? "SDK_PAY",
    time: String(item.createdAt),
    cashier: item.orderType === "QR" ? "QR guest" : "-",
    printStatus: "NOT_PRINTED",
    items: [],
    tableCode: item.tableCode,
    orderType: item.orderType,
    memberName: item.memberName,
    memberTier: item.memberTier,
    originalAmount: item.originalAmountCents ? `CNY ${(item.originalAmountCents / 100).toFixed(2)}` : undefined,
    memberDiscount: item.memberDiscountCents ? `CNY ${(item.memberDiscountCents / 100).toFixed(2)}` : undefined,
    promotionDiscount: item.promotionDiscountCents
      ? `CNY ${(item.promotionDiscountCents / 100).toFixed(2)}`
      : undefined,
    payableAmount: item.payableAmountCents ? `CNY ${(item.payableAmountCents / 100).toFixed(2)}` : undefined,
    giftItems: item.giftItems ?? []
  }));
}
