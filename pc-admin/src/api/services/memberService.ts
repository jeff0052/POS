import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { Member, MemberTier, PointsRecord, RechargeRecord } from "../../types";

export async function getMembers(): Promise<Member[]> {
  if (USE_MOCK_API) {
    return mockApi.getMembers();
  }

  return mockApi.getMembers();
}

export async function getMemberTiers(): Promise<MemberTier[]> {
  if (USE_MOCK_API) {
    return mockApi.getMemberTiers();
  }

  return mockApi.getMemberTiers();
}

export async function getRechargeRecords(): Promise<RechargeRecord[]> {
  if (USE_MOCK_API) {
    return mockApi.getRechargeRecords();
  }

  return mockApi.getRechargeRecords();
}

export async function getPointsRecords(): Promise<PointsRecord[]> {
  if (USE_MOCK_API) {
    return mockApi.getPointsRecords();
  }

  return mockApi.getPointsRecords();
}
