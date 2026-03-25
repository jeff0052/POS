import { apiGetV2, apiPostV2, apiPutV2 } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { PromotionRule } from "../../types";

export async function getPromotionRules(): Promise<PromotionRule[]> {
  if (USE_MOCK_API) {
    return mockApi.getPromotionRules();
  }

  const response = await apiGetV2<
    Array<{
      id: number;
      ruleCode: string;
      ruleName: string;
      ruleType: "FULL_REDUCTION" | "GIFT_SKU";
      thresholdAmountCents: number;
      discountAmountCents: number;
      priority: number;
    }>
  >("/promotions?storeId=101");

  return response.map((item) => ({
    id: item.id,
    name: item.ruleName,
    type: item.ruleType === "GIFT_SKU" ? "GIFT" : "FULL_REDUCTION",
    status: "ACTIVE",
    ruleSummary:
      item.ruleType === "GIFT_SKU"
        ? `Spend CNY ${(item.thresholdAmountCents / 100).toFixed(2)} get gift`
        : `Spend CNY ${(item.thresholdAmountCents / 100).toFixed(2)} save CNY ${(item.discountAmountCents / 100).toFixed(2)}`,
    priority: item.priority
  }));
}

export async function getPromotionRuleDetail(ruleId: number) {
  return apiGetV2<{
    id: number;
    merchantId: number;
    storeId: number;
    ruleCode: string;
    ruleName: string;
    ruleType: string;
    ruleStatus: string;
    priority: number;
    conditionType: string;
    thresholdAmountCents: number | null;
    rewardType: string;
    discountAmountCents: number | null;
    giftSkuId: number | null;
    giftQuantity: number | null;
  }>(`/promotions/${ruleId}`);
}

type UpsertPromotionPayload = {
  merchantId: number;
  storeId: number;
  ruleCode: string;
  ruleName: string;
  ruleType: "FULL_REDUCTION" | "GIFT_SKU";
  ruleStatus: "ACTIVE" | "INACTIVE";
  priority: number;
  conditionType: "MIN_SUBTOTAL";
  thresholdAmountCents: number;
  rewardType: "DISCOUNT_AMOUNT" | "GIFT_SKU";
  discountAmountCents?: number;
  giftSkuId?: number;
  giftQuantity?: number;
};

export async function createPromotionRule(payload: UpsertPromotionPayload) {
  return apiPostV2("/promotions", payload);
}

export async function updatePromotionRule(ruleId: number, payload: UpsertPromotionPayload) {
  return apiPutV2(`/promotions/${ruleId}`, payload);
}
