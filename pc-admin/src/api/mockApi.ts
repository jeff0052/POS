import {
  mockCategories,
  mockDashboardSummary,
  mockOrders,
  mockProducts,
  mockRefunds,
  mockUser
} from "../mocks/data";
import type { AuthUser, Category, DashboardSummary, Order, Product, RefundRecord } from "../types";

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
