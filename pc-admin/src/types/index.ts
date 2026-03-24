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
  originalAmount?: string;
  memberBenefit?: string;
  promotionBenefit?: string;
  gift?: boolean;
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
  tableCode?: string;
  orderType?: "POS" | "QR";
  memberName?: string;
  memberTier?: string;
  originalAmount?: string;
  memberDiscount?: string;
  promotionDiscount?: string;
  giftItems?: string[];
  payableAmount?: string;
}

export interface DashboardSummary {
  revenue: string;
  orders: string;
  refunds: string;
  cashiers: string;
}

export interface Member {
  id: number;
  name: string;
  phone: string;
  tierName: string;
  points: number;
  balance: string;
  totalSpent: string;
  totalRecharge: string;
  status: "ACTIVE" | "INACTIVE";
}

export interface MemberTier {
  id: number;
  name: string;
  upgradeRule: string;
  benefits: string[];
}

export interface RechargeRecord {
  id: number;
  memberName: string;
  memberPhone: string;
  amount: string;
  bonusAmount: string;
  status: "SUCCESS" | "PENDING";
  time: string;
}

export interface PointsRecord {
  id: number;
  memberName: string;
  changeType: "EARN" | "REDEEM" | "REFUND" | "ADJUST";
  points: number;
  source: string;
  time: string;
}

export interface PromotionRule {
  id: number;
  name: string;
  type: "FULL_REDUCTION" | "GIFT" | "MEMBER_PRICE" | "TIER_DISCOUNT" | "RECHARGE_BONUS";
  status: "ACTIVE" | "INACTIVE";
  ruleSummary: string;
  priority: number;
}

export interface GtoBatch {
  id: number;
  batchNo: string;
  businessDate: string;
  storeName: string;
  tradeCount: number;
  netSales: string;
  discountAmount: string;
  syncStatus: "PENDING" | "SUCCESS" | "FAILED";
  exportTime: string;
}

export interface SalesReportSummary {
  sales: string;
  discounts: string;
  memberSales: string;
  rechargeSales: string;
  tableTurnover: string;
  pendingGtoBatches: string;
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
