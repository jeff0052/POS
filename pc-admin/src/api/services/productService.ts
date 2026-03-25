import { apiGetV2, apiPostV2, apiPutV2 } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { Product } from "../../types";

type ProductListResponse = Array<{
  id: number;
  categoryId?: number;
  name: string;
  barcode: string;
  priceCents: number;
  stockQty: number;
  status: "ENABLED" | "DISABLED";
  categoryName?: string;
  skus?: Array<{
    id: number;
    code: string;
    name: string;
    priceCents: number;
    status: "ENABLED" | "DISABLED";
    available: boolean;
  }>;
}>;

type ProductResponse = ProductListResponse[number];

type UpsertProductRequest = {
  storeCode: string;
  categoryId: number;
  name: string;
  barcode?: string;
  status: "ENABLED" | "DISABLED";
  skus: Array<{
    skuId?: number;
    skuCode?: string;
    name: string;
    priceCents: number;
    status: "ENABLED" | "DISABLED";
    available: boolean;
  }>;
};

function formatMoney(value: number): string {
  return `CNY ${(value / 100).toFixed(2)}`;
}

function mapStatus(status: string): "Enabled" | "Disabled" {
  return status === "ENABLED" || status === "ACTIVE" ? "Enabled" : "Disabled";
}

function mapProduct(item: ProductResponse): Product {
  const skus = item.skus ?? [];
  return {
    id: item.id,
    categoryId: item.categoryId,
    name: item.name,
    barcode: item.barcode,
    price: formatMoney(item.priceCents),
    stock: item.stockQty,
    status: mapStatus(item.status),
    categoryName: item.categoryName ?? "-",
    skus: skus.map((sku) => ({
      id: sku.id,
      code: sku.code,
      name: sku.name,
      price: formatMoney(sku.priceCents),
      status: mapStatus(sku.status),
      available: sku.available
    }))
  };
}

export async function getProducts(): Promise<Product[]> {
  if (USE_MOCK_API) {
    return (await mockApi.getProducts()).map((item) => ({
      ...item,
      skus: []
    }));
  }

  const response = await apiGetV2<ProductListResponse>("/admin/catalog/products?storeCode=1001");
  return response.map(mapProduct);
}

export async function createProduct(values: UpsertProductRequest): Promise<Product> {
  const response = await apiPostV2<ProductResponse>("/admin/catalog/products", values);
  return mapProduct(response);
}

export async function updateProduct(productId: number, values: UpsertProductRequest): Promise<Product> {
  const response = await apiPutV2<ProductResponse>(`/admin/catalog/products/${productId}`, values);
  return mapProduct(response);
}
