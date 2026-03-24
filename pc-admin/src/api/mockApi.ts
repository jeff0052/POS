import {
  mockCategories,
  mockDashboardSummary,
  mockGtoBatches,
  mockMembers,
  mockMemberTiers,
  mockOrders,
  mockPointsRecords,
  mockProducts,
  mockPromotionRules,
  mockRechargeRecords,
  mockRefunds,
  mockSalesReportSummary,
  mockUser
} from "../mocks/data";
import type {
  AuthUser,
  Category,
  DashboardSummary,
  GtoBatch,
  Member,
  MemberTier,
  Order,
  PointsRecord,
  Product,
  PromotionRule,
  RechargeRecord,
  RefundRecord,
  SalesReportSummary
} from "../types";

function delay<T>(value: T, ms = 250): Promise<T> {
  return new Promise((resolve) => {
    window.setTimeout(() => resolve(value), ms);
  });
}

export async function login(username: string, password: string): Promise<AuthUser> {
  if (!username || !password) {
    throw new Error("Username and password are required");
  }
  return delay(mockUser);
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  return delay(mockDashboardSummary);
}

export async function getProducts(): Promise<Product[]> {
  return delay(mockProducts);
}

export async function getCategories(): Promise<Category[]> {
  return delay(mockCategories);
}

export async function getOrders(): Promise<Order[]> {
  return delay(mockOrders);
}

export async function getRefunds(): Promise<RefundRecord[]> {
  return delay(mockRefunds);
}

export async function getMembers(): Promise<Member[]> {
  return delay(mockMembers);
}

export async function getMemberTiers(): Promise<MemberTier[]> {
  return delay(mockMemberTiers);
}

export async function getRechargeRecords(): Promise<RechargeRecord[]> {
  return delay(mockRechargeRecords);
}

export async function getPointsRecords(): Promise<PointsRecord[]> {
  return delay(mockPointsRecords);
}

export async function getPromotionRules(): Promise<PromotionRule[]> {
  return delay(mockPromotionRules);
}

export async function getGtoBatches(): Promise<GtoBatch[]> {
  return delay(mockGtoBatches);
}

export async function getSalesReportSummary(): Promise<SalesReportSummary> {
  return delay(mockSalesReportSummary);
}
