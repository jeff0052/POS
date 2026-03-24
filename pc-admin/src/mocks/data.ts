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

export const mockUser: AuthUser = {
  id: 1,
  username: "admin",
  displayName: "Store Admin",
  role: "ADMIN",
  storeId: 1001
};

export const mockDashboardSummary: DashboardSummary = {
  revenue: "CNY 12,680.00",
  orders: "128",
  refunds: "CNY 320.00",
  cashiers: "3"
};

export const mockCategories: Category[] = [
  { id: 1, name: "Drinks", sortOrder: 1, status: "Enabled" },
  { id: 2, name: "Meals", sortOrder: 2, status: "Enabled" },
  { id: 3, name: "Snacks", sortOrder: 3, status: "Disabled" }
];

export const mockProducts: Product[] = [
  {
    id: 1,
    name: "Coke",
    barcode: "692000000001",
    price: "CNY 5.00",
    stock: 100,
    status: "Enabled",
    categoryName: "Drinks"
  },
  {
    id: 2,
    name: "Fried Rice",
    barcode: "692000000002",
    price: "CNY 18.00",
    stock: 40,
    status: "Enabled",
    categoryName: "Meals"
  }
];

export const mockOrders: Order[] = [
  {
    id: 1,
    orderNo: "POS202603200001",
    amount: "CNY 28.00",
    status: "PAID",
    payment: "SDK_PAY",
    time: "2026-03-20 09:21",
    cashier: "Amy",
    printStatus: "PRINT_SUCCESS",
    tableCode: "T2",
    orderType: "POS",
    memberName: "Lina Chen",
    memberTier: "Gold",
    originalAmount: "CNY 32.00",
    memberDiscount: "CNY 2.00",
    promotionDiscount: "CNY 2.00",
    payableAmount: "CNY 28.00",
    giftItems: ["Peach Soda"],
    items: [
      {
        productName: "Fried Rice",
        quantity: 1,
        amount: "CNY 18.00",
        originalAmount: "CNY 20.00",
        memberBenefit: "Gold member price"
      },
      {
        productName: "Coke",
        quantity: 2,
        amount: "CNY 10.00",
        promotionBenefit: "Buy 2 discount"
      }
    ]
  },
  {
    id: 2,
    orderNo: "POS202603200002",
    amount: "CNY 12.00",
    status: "DRAFT",
    payment: "CASH",
    time: "2026-03-20 09:34",
    cashier: "Tom",
    printStatus: "NOT_PRINTED",
    tableCode: "T8",
    orderType: "QR",
    originalAmount: "CNY 12.00",
    memberDiscount: "CNY 0.00",
    promotionDiscount: "CNY 0.00",
    payableAmount: "CNY 12.00",
    items: [{ productName: "Milk Tea", quantity: 1, amount: "CNY 12.00" }]
  }
];

export const mockRefunds: RefundRecord[] = [
  {
    id: 1,
    refundNo: "REF202603200001",
    orderNo: "POS202603200001",
    refundAmount: "CNY 28.00",
    status: "SUCCESS",
    time: "2026-03-20 10:03",
    operator: "Amy"
  },
  {
    id: 2,
    refundNo: "REF202603200002",
    orderNo: "POS202603200018",
    refundAmount: "CNY 12.00",
    status: "PROCESSING",
    time: "2026-03-20 14:22",
    operator: "Tom"
  }
];

export const mockMembers: Member[] = [
  {
    id: 1,
    name: "Lina Chen",
    phone: "13800000001",
    tierName: "Gold",
    points: 2860,
    balance: "CNY 320.00",
    totalSpent: "CNY 8,620.00",
    totalRecharge: "CNY 2,000.00",
    status: "ACTIVE"
  },
  {
    id: 2,
    name: "Eric Wang",
    phone: "13800000002",
    tierName: "Silver",
    points: 940,
    balance: "CNY 88.00",
    totalSpent: "CNY 2,480.00",
    totalRecharge: "CNY 500.00",
    status: "ACTIVE"
  }
];

export const mockMemberTiers: MemberTier[] = [
  { id: 1, name: "Silver", upgradeRule: "Spend over CNY 2,000", benefits: ["95折", "基础积分"] },
  { id: 2, name: "Gold", upgradeRule: "Spend over CNY 8,000", benefits: ["9折", "会员价", "充值赠送"] },
  { id: 3, name: "Diamond", upgradeRule: "Spend over CNY 20,000", benefits: ["88折", "专属套餐", "双倍积分"] }
];

export const mockRechargeRecords: RechargeRecord[] = [
  {
    id: 1,
    memberName: "Lina Chen",
    memberPhone: "13800000001",
    amount: "CNY 500.00",
    bonusAmount: "CNY 80.00",
    status: "SUCCESS",
    time: "2026-03-20 13:10"
  },
  {
    id: 2,
    memberName: "Eric Wang",
    memberPhone: "13800000002",
    amount: "CNY 200.00",
    bonusAmount: "CNY 20.00",
    status: "SUCCESS",
    time: "2026-03-19 19:20"
  }
];

export const mockPointsRecords: PointsRecord[] = [
  {
    id: 1,
    memberName: "Lina Chen",
    changeType: "EARN",
    points: 120,
    source: "POS202603200001",
    time: "2026-03-20 09:22"
  },
  {
    id: 2,
    memberName: "Lina Chen",
    changeType: "REDEEM",
    points: -200,
    source: "Manual order settlement",
    time: "2026-03-20 12:11"
  }
];

export const mockPromotionRules: PromotionRule[] = [
  {
    id: 1,
    name: "Lunch full reduction",
    type: "FULL_REDUCTION",
    status: "ACTIVE",
    ruleSummary: "Spend CNY 100 save CNY 10",
    priority: 10
  },
  {
    id: 2,
    name: "Gold member fried rice price",
    type: "MEMBER_PRICE",
    status: "ACTIVE",
    ruleSummary: "Gold members get CNY 18 fried rice",
    priority: 20
  },
  {
    id: 3,
    name: "Recharge bonus 500+80",
    type: "RECHARGE_BONUS",
    status: "ACTIVE",
    ruleSummary: "Recharge CNY 500 get CNY 80 bonus",
    priority: 30
  }
];

export const mockGtoBatches: GtoBatch[] = [
  {
    id: 1,
    batchNo: "GTO2026032001",
    businessDate: "2026-03-20",
    storeName: "Riverside Branch",
    tradeCount: 128,
    netSales: "CNY 12,680.00",
    discountAmount: "CNY 860.00",
    syncStatus: "SUCCESS",
    exportTime: "2026-03-20 23:59"
  },
  {
    id: 2,
    batchNo: "GTO2026032101",
    businessDate: "2026-03-21",
    storeName: "Riverside Branch",
    tradeCount: 132,
    netSales: "CNY 13,240.00",
    discountAmount: "CNY 910.00",
    syncStatus: "PENDING",
    exportTime: "2026-03-21 23:59"
  }
];

export const mockSalesReportSummary: SalesReportSummary = {
  sales: "CNY 12,680.00",
  discounts: "CNY 860.00",
  memberSales: "CNY 4,220.00",
  rechargeSales: "CNY 700.00",
  tableTurnover: "4.6",
  pendingGtoBatches: "1"
};
