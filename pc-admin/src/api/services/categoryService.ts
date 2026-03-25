import { apiGetV2 } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { Category } from "../../types";

type CategoryListResponse = Array<{
  id: number;
  name: string;
  sortOrder: number;
  status: "ENABLED" | "DISABLED";
}>;

export async function getCategories(): Promise<Category[]> {
  if (USE_MOCK_API) {
    return mockApi.getCategories();
  }

  const response = await apiGetV2<CategoryListResponse>("/admin/catalog/categories?storeCode=1001");
  return response.map((item) => ({
    id: item.id,
    name: item.name,
    sortOrder: item.sortOrder,
    status: item.status === "ENABLED" ? "Enabled" : "Disabled"
  }));
}
