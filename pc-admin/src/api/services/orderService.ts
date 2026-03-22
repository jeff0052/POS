import { apiGet } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { Order } from "../../types";

interface OrderListResponse {
  list: Array<{
    id: number;
    orderNo: string;
    paidAmountCents: number;
    orderStatus: "PENDING" | "PAID" | "REFUNDED";
    paymentMethod?: "CASH" | "SDK_PAY";
    createdAt: number | string;
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
    amount: `CNY ${(item.paidAmountCents / 100).toFixed(2)}`,
    status: item.orderStatus,
    payment: item.paymentMethod ?? "SDK_PAY",
    time: String(item.createdAt),
    cashier: "-",
    printStatus: "NOT_PRINTED",
    items: []
  }));
}
