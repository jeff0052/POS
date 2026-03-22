import { apiGet } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { Product } from "../../types";

interface ProductListResponse {
  list: Array<{
    id: number;
    name: string;
    barcode: string;
    priceCents: number;
    stockQty: number;
    status: "ENABLED" | "DISABLED";
    categoryName?: string;
  }>;
}

export async function getProducts(): Promise<Product[]> {
  if (USE_MOCK_API) {
    return mockApi.getProducts();
  }

  const response = await apiGet<ProductListResponse>("/products?storeId=1001&page=1&pageSize=50");
  return response.list.map((item) => ({
    id: item.id,
    name: item.name,
    barcode: item.barcode,
    price: `CNY ${(item.priceCents / 100).toFixed(2)}`,
    stock: item.stockQty,
    status: item.status === "ENABLED" ? "Enabled" : "Disabled",
    categoryName: item.categoryName ?? "-"
  }));
}
