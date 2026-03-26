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

export interface ProductSku {
  id: number;
  code: string;
  name: string;
  price: string;
  status: "Enabled" | "Disabled";
  available: boolean;
}

export interface ProductAttributeValue {
  code?: string;
  label: string;
  priceDeltaCents: number;
  defaultSelected: boolean;
  kitchenLabel?: string;
}

export interface ProductAttributeGroup {
  code?: string;
  name: string;
  selectionMode: "SINGLE" | "MULTIPLE";
  required: boolean;
  minSelect?: number;
  maxSelect?: number;
  values: ProductAttributeValue[];
}

export interface ProductModifierOption {
  code?: string;
  label: string;
  priceDeltaCents: number;
  defaultSelected: boolean;
  kitchenLabel?: string;
}

export interface ProductModifierGroup {
  code?: string;
  name: string;
  freeQuantity?: number;
  minSelect?: number;
  maxSelect?: number;
  options: ProductModifierOption[];
}

export interface ProductComboSlot {
  code?: string;
  name: string;
  minSelect?: number;
  maxSelect?: number;
  allowedSkuCodes: string[];
}

export interface Product {
  id: number;
  categoryId?: number;
  name: string;
  barcode: string;
  price: string;
  stock: number;
  status: "Enabled" | "Disabled";
  categoryName: string;
  skus: ProductSku[];
  attributeGroups: ProductAttributeGroup[];
  modifierGroups: ProductModifierGroup[];
  comboSlots: ProductComboSlot[];
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
  orderId?: string;
  orderNo: string;
  amount: string;
  status: "DRAFT" | "SUBMITTED" | "PENDING_SETTLEMENT" | "PAID" | "REFUNDED";
  payment:
    | "CASH"
    | "CARD_TERMINAL"
    | "WECHAT_QR"
    | "ALIPAY_QR"
    | "PAYNOW_QR"
    | "UNPAID";
  time: string;
  cashier: string;
  printStatus: "PRINT_SUCCESS" | "PRINT_FAILED" | "NOT_PRINTED";
  items: OrderItem[];
  storeId?: number;
  tableId?: number;
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

export interface MemberDetail {
  id: number;
  memberNo: string;
  name: string;
  phone: string;
  tierName: string;
  status: "ACTIVE" | "INACTIVE";
  points: number;
  balance: string;
  totalSpent: string;
  totalRecharge: string;
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

export interface MemberConsumptionTopMember {
  memberId: number;
  memberName: string;
  tierCode: string;
  orderCount: number;
  totalSales: string;
  totalRecharge: string;
  memberDiscount: string;
}

export interface MemberConsumptionSummary {
  totalMemberSales: string;
  totalMemberDiscounts: string;
  memberOrderCount: string;
  activeMemberCount: string;
  totalRecharge: string;
  totalBonus: string;
  rechargeOrderCount: string;
  averageRecharge: string;
  topMembers: MemberConsumptionTopMember[];
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
