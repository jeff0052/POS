import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { GtoBatch } from "../../types";

export async function getGtoBatches(): Promise<GtoBatch[]> {
  if (USE_MOCK_API) {
    return mockApi.getGtoBatches();
  }

  return mockApi.getGtoBatches();
}
