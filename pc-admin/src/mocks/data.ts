import type { AuthUser, Category, DashboardSummary, Order, Product, RefundRecord } from "../types";

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
    items: [
      { productName: "Fried Rice", quantity: 1, amount: "CNY 18.00" },
      { productName: "Coke", quantity: 2, amount: "CNY 10.00" }
    ]
  },
  {
    id: 2,
    orderNo: "POS202603200002",
    amount: "CNY 12.00",
    status: "PENDING",
    payment: "CASH",
    time: "2026-03-20 09:34",
    cashier: "Tom",
    printStatus: "NOT_PRINTED",
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
