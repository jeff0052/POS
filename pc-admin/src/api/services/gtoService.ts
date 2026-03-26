import { apiGetV2 } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { GtoBatch } from "../../types";

interface BackendGtoBatch {
  batchId: string;
  storeId: number;
  exportDate: string;
  batchStatus: string;
  totalRecords: number;
  totalSalesCents: number;
  totalTaxCents: number;
  createdAt: string;
}

export async function getGtoBatches(): Promise<GtoBatch[]> {
  if (USE_MOCK_API) {
    return mockApi.getGtoBatches();
  }

  try {
    const response = await apiGetV2<{ content: BackendGtoBatch[] }>("/gto/batches?merchantId=1&page=0&size=50");
    const items = response.content ?? [];
    return items.map((item) => ({
      id: item.batchId,
      exportDate: item.exportDate,
      status: item.batchStatus,
      totalRecords: item.totalRecords,
      totalSales: `SGD ${(item.totalSalesCents / 100).toFixed(2)}`,
      totalTax: `SGD ${(item.totalTaxCents / 100).toFixed(2)}`,
      createdAt: item.createdAt
    }));
  } catch {
    return mockApi.getGtoBatches();
  }
}
