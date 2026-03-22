export type UserRole = "ADMIN" | "OWNER" | "CASHIER";

export interface AuthUser {
  id: number;
  username: string;
  displayName: string;
  role: UserRole;
  storeId: number;
}

export interface Category {
  id: number;
  name: string;
  sortOrder: number;
  status: "Enabled" | "Disabled";
}

export interface Product {
  id: number;
  name: string;
  barcode: string;
  price: string;
  stock: number;
  status: "Enabled" | "Disabled";
  categoryName: string;
}

export interface OrderItem {
  productName: string;
  quantity: number;
  amount: string;
}

export interface Order {
  id: number;
  orderNo: string;
  amount: string;
  status: "PENDING" | "PAID" | "REFUNDED";
  payment: "CASH" | "SDK_PAY";
  time: string;
  cashier: string;
  printStatus: "PRINT_SUCCESS" | "PRINT_FAILED" | "NOT_PRINTED";
  items: OrderItem[];
}

export interface DashboardSummary {
  revenue: string;
  orders: string;
  refunds: string;
  cashiers: string;
}

export interface RefundRecord {
  id: number;
  refundNo: string;
  orderNo: string;
  refundAmount: string;
  status: "PROCESSING" | "SUCCESS" | "FAILED";
  time: string;
  operator: string;
}

export interface StoreSettings {
  storeId: number;
  receiptTitle: string;
  receiptFooter: string;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}
