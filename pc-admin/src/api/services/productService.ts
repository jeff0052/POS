import { apiGetV2 } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { Product } from "../../types";

type ProductListResponse = Array<{
  id: number;
  name: string;
  barcode: string;
  priceCents: number;
  stockQty: number;
  status: "ENABLED" | "DISABLED";
  categoryName?: string;
}>;

export async function getProducts(): Promise<Product[]> {
  if (USE_MOCK_API) {
    return mockApi.getProducts();
  }

  const response = await apiGetV2<ProductListResponse>("/admin/catalog/products?storeCode=1001");
  return response.map((item) => ({
    id: item.id,
    name: item.name,
    barcode: item.barcode,
    price: `CNY ${(item.priceCents / 100).toFixed(2)}`,
    stock: item.stockQty,
    status: item.status === "ENABLED" ? "Enabled" : "Disabled",
    categoryName: item.categoryName ?? "-"
  }));
}
