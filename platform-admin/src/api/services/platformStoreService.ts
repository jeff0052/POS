import { apiGetV2 } from "../client";

export type PlatformStoreOverview = {
  storeId: number;
  merchantId: number;
  storeCode: string;
  storeName: string;
  tableCount: number;
  availableTables: number;
  occupiedTables: number;
  reservedTables: number;
  pendingSettlementTables: number;
};

export function fetchPlatformStoreOverview() {
  return apiGetV2<PlatformStoreOverview[]>("/platform/stores/overview");
}
