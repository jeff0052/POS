import { apiGetV2, apiPostV2, apiPutV2 } from "../client";
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

type UpsertCategoryRequest = {
  storeCode: string;
  categoryCode?: string;
  name: string;
  enabled: boolean;
  sortOrder: number;
};

type CategoryResponse = {
  id: number;
  name: string;
  sortOrder: number;
  status: "ENABLED" | "DISABLED";
};

function mapCategory(item: CategoryResponse): Category {
  return {
    id: item.id,
    name: item.name,
    sortOrder: item.sortOrder,
    status: item.status === "ENABLED" ? "Enabled" : "Disabled"
  };
}

export async function createCategory(values: UpsertCategoryRequest): Promise<Category> {
  const response = await apiPostV2<CategoryResponse>("/admin/catalog/categories", values);
  return mapCategory(response);
}

export async function updateCategory(
  categoryId: number,
  values: UpsertCategoryRequest
): Promise<Category> {
  const response = await apiPutV2<CategoryResponse>(`/admin/catalog/categories/${categoryId}`, values);
  return mapCategory(response);
}
