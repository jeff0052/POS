import { apiGetV2, apiPostV2 } from "../client";
import { USE_MOCK_API } from "../config";
import * as mockApi from "../mockApi";
import type { Member, MemberTier, PointsRecord, RechargeRecord } from "../../types";

export async function getMembers(): Promise<Member[]> {
  if (USE_MOCK_API) {
    return mockApi.getMembers();
  }

  const response = await apiGetV2<
    Array<{
      memberId: number;
      memberName: string;
      phone: string;
      tierCode: string;
      pointsBalance: number;
      cashBalanceCents: number;
    }>
  >("/members?keyword=");

  return response.map((item) => ({
    id: item.memberId,
    name: item.memberName,
    phone: item.phone,
    tierName: item.tierCode,
    points: item.pointsBalance,
    balance: `CNY ${(item.cashBalanceCents / 100).toFixed(2)}`,
    totalSpent: "CNY 0.00",
    totalRecharge: "CNY 0.00",
    status: "ACTIVE"
  }));
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

  const response = await apiGetV2<
    Array<{
      id: number;
      memberName: string;
      memberPhone: string;
      amountCents: number;
      bonusAmountCents: number;
      status: "SUCCESS" | "PENDING";
      createdAt: string;
    }>
  >("/members/recharge-records?merchantId=1");

  return response.map((item) => ({
    id: item.id,
    memberName: item.memberName,
    memberPhone: item.memberPhone,
    amount: `CNY ${(item.amountCents / 100).toFixed(2)}`,
    bonusAmount: `CNY ${(item.bonusAmountCents / 100).toFixed(2)}`,
    status: item.status,
    time: item.createdAt
  }));
}

export async function getPointsRecords(): Promise<PointsRecord[]> {
  if (USE_MOCK_API) {
    return mockApi.getPointsRecords();
  }

  const response = await apiGetV2<
    Array<{
      id: number;
      memberName: string;
      changeType: "EARN" | "REDEEM" | "REFUND" | "ADJUST";
      points: number;
      source: string;
      createdAt: string;
    }>
  >("/members/points-ledger?merchantId=1");

  return response.map((item) => ({
    id: item.id,
    memberName: item.memberName,
    changeType: item.changeType,
    points: item.points,
    source: item.source,
    time: item.createdAt
  }));
}

export async function rechargeMember(memberId: number, amountCents: number, bonusAmountCents: number, operatorName: string) {
  return apiPostV2<{
    memberId: number;
    rechargeNo: string;
    amountCents: number;
    bonusAmountCents: number;
    cashBalanceCents: number;
  }>(`/members/${memberId}/recharge`, {
    amountCents,
    bonusAmountCents,
    operatorName
  });
}

export async function adjustMemberPoints(
  memberId: number,
  pointsDelta: number,
  changeType: "ADJUST",
  source: string,
  operatorName: string
) {
  return apiPostV2<{
    memberId: number;
    ledgerNo: string;
    pointsDelta: number;
    balanceAfter: number;
  }>(`/members/${memberId}/points-adjustment`, {
    pointsDelta,
    changeType,
    source,
    operatorName
  });
}
