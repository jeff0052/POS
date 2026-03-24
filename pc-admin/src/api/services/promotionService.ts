import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { PromotionRule } from "../../types";

export async function getPromotionRules(): Promise<PromotionRule[]> {
  if (USE_MOCK_API) {
    return mockApi.getPromotionRules();
  }

  return mockApi.getPromotionRules();
}
