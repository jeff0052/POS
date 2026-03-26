import { apiGet } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { RefundRecord } from "../../types";

interface RefundListResponse {
  list: Array<{
    refundId: number;
    refundNo: string;
    orderNo: string;
    refundAmountCents: number;
    status: "PROCESSING" | "SUCCESS" | "FAILED";
    createdAt: string | number;
  }>;
}

export async function getRefunds(): Promise<RefundRecord[]> {
  if (USE_MOCK_API) {
    return mockApi.getRefunds();
  }

  try {
    const response = await apiGet<RefundListResponse>("/refunds?storeId=1001&page=1&pageSize=50");
    return response.list.map((item) => ({
      id: item.refundId,
      refundNo: item.refundNo,
      orderNo: item.orderNo,
      refundAmount: `SGD ${(item.refundAmountCents / 100).toFixed(2)}`,
      status: item.status,
      time: String(item.createdAt),
      operator: "-"
    }));
  } catch (error) {
    if (error instanceof Error && error.message.includes("404")) {
      return [];
    }
    throw error;
  }
}
