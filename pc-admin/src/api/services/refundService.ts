import { apiGet } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { RefundRecord } from "../../types";

// Backend returns Page<RefundRecordDto> with Spring pagination
interface BackendRefundRecord {
  id: number;
  refundNo: string;
  settlementId: number;
  settlementNo: string;
  merchantId: number;
  storeId: number;
  refundAmountCents: number;
  refundType: string;
  refundReason: string;
  refundStatus: string;
  paymentMethod: string;
  operatedBy: number | null;
  approvedBy: number | null;
  createdAt: string;
}

export async function getRefunds(): Promise<RefundRecord[]> {
  if (USE_MOCK_API) {
    return mockApi.getRefunds();
  }

  try {
    // Backend returns Page object with content array
    const response = await apiGet<{ content: BackendRefundRecord[]; totalElements: number }>("/refunds?storeId=1&page=0&size=50");
    const items = response.content ?? [];
    return items.map((item) => ({
      id: item.id,
      refundNo: item.refundNo,
      orderNo: item.settlementNo,
      refundAmount: `SGD ${(item.refundAmountCents / 100).toFixed(2)}`,
      status: mapRefundStatus(item.refundStatus),
      time: item.createdAt,
      operator: item.operatedBy ? String(item.operatedBy) : "-"
    }));
  } catch (error) {
    if (error instanceof Error && error.message.includes("404")) {
      return [];
    }
    throw error;
  }
}

function mapRefundStatus(backendStatus: string): "PROCESSING" | "SUCCESS" | "FAILED" {
  switch (backendStatus) {
    case "COMPLETED": return "SUCCESS";
    case "PENDING": return "PROCESSING";
    case "REJECTED": return "FAILED";
    default: return "SUCCESS";
  }
}
