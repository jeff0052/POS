import { useEffect, useMemo, useRef, useState } from "react";

type View =
  | "reservations"
  | "tables"
  | "transfer"
  | "ordering"
  | "review"
  | "split"
  | "payment"
  | "success";

type OrderStage = "DRAFT" | "SUBMITTED" | "PENDING_SETTLEMENT" | "SETTLED";

type Category = {
  id: string;
  name: string;
  items: number;
  color: string;
};

type MenuItem = {
  id: number;
  skuId: number;
  skuCode: string;
  name: string;
  category: string;
  price: number;
  image: string;
  memberPrice?: number;
};

type OrderItem = MenuItem & {
  quantity: number;
  note?: string;
};

type SubmittedRound = {
  id: string;
  sentAt: string;
  items: OrderItem[];
  total: number;
};

type TableCard = {
  id: number;
  status: "occupied" | "reserved" | "available";
  guests: number;
  area: string;
  total: number;
  course: string;
  accent: string;
  source?: "POS" | "QR";
  settlementState?: OrderStage;
  memberName?: string;
};

type Reservation = {
  id: number;
  name: string;
  time: string;
  partySize: number;
  status: "checked-in" | "waiting" | "upcoming";
  area: string;
};

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

type ActiveTableOrderDto = {
  activeOrderId: string;
  orderNo: string;
  storeId: number;
  tableId: number;
  tableCode: string;
  orderSource: "POS" | "QR";
  status: OrderStage;
  memberId: number | null;
  items: Array<{
    skuId: number;
    skuCode: string;
    skuName: string;
    quantity: number;
    unitPriceCents: number;
    remark: string | null;
    lineTotalCents: number;
  }>;
  pricing: {
    originalAmountCents: number;
    memberDiscountCents: number;
    promotionDiscountCents: number;
    payableAmountCents: number;
  };
};

type TableContextResponse = {
  storeId: number;
  storeCode: string;
  storeName: string;
  tableId: number;
  tableCode: string;
  tableName: string;
  tableStatus: string;
  currentActiveOrder: ActiveTableOrderDto | null;
};

type SettlementPreviewResponse = {
  activeOrderId: string;
  status: OrderStage;
  member: {
    id: number;
    name: string;
    tier: string;
  } | null;
  pricing: {
    originalAmountCents: number;
    memberDiscountCents: number;
    promotionDiscountCents: number;
    payableAmountCents: number;
  };
  giftItems: Array<{
    skuName: string;
    quantity: number;
  }>;
};

type SubmittedOrderResponse = {
  submittedOrderId: string;
  orderNo: string;
  sourceOrderType: "POS" | "QR";
  fulfillmentStatus: string;
  settlementStatus: string;
  memberId: number | null;
  pricing: {
    originalAmountCents: number;
    memberDiscountCents: number;
    promotionDiscountCents: number;
    payableAmountCents: number;
  };
  items: Array<{
    skuId: number;
    skuCode: string;
    skuName: string;
    quantity: number;
    unitPriceCents: number;
    remark: string | null;
    lineTotalCents: number;
  }>;
};

type QrCurrentOrderResponse = {
  activeOrderId: string;
  orderNo: string;
  storeId: number;
  tableId: number;
  tableCode: string;
  settlementStatus: OrderStage;
  memberId: number | null;
  memberName: string | null;
  memberTier?: string | null;
  originalAmountCents?: number;
  memberDiscountCents?: number;
  promotionDiscountCents?: number;
  payableAmountCents: number;
  orderSource: "POS" | "QR";
  items: Array<{
    skuId: number;
    skuCode: string;
    productName: string;
    quantity: number;
    unitPriceCents?: number;
    memberPriceCents?: number | null;
  }>;
};

const categories: Category[] = [
  { id: "meals", name: "热销饭类", items: 3, color: "amber" },
  { id: "snacks", name: "现炸小食", items: 2, color: "coral" },
  { id: "drinks", name: "饮品甜水", items: 2, color: "blue" },
  { id: "desserts", name: "甜点轻食", items: 1, color: "pink" },
  { id: "popular", name: "主厨推荐", items: 1, color: "green" }
];

const tables: TableCard[] = [
  { id: 1, status: "occupied", guests: 2, area: "Window", total: 48, course: "Sent to kitchen", accent: "sky" },
  { id: 2, status: "occupied", guests: 4, area: "Main hall", total: 92.5, course: "QR order waiting for cashier", accent: "peach", source: "QR", settlementState: "PENDING_SETTLEMENT", memberName: "Lina Chen" },
  { id: 3, status: "available", guests: 0, area: "Main hall", total: 0, course: "Ready", accent: "mint" },
  { id: 4, status: "reserved", guests: 2, area: "Patio", total: 0, course: "19:00 hold", accent: "lilac" },
  { id: 5, status: "occupied", guests: 6, area: "Private booth", total: 168, course: "Sent to kitchen", accent: "gold" },
  { id: 6, status: "available", guests: 0, area: "Window", total: 0, course: "Ready", accent: "mint" },
  { id: 7, status: "occupied", guests: 3, area: "Chef counter", total: 71, course: "QR order waiting for cashier", accent: "rose", source: "QR", settlementState: "PENDING_SETTLEMENT" },
  { id: 8, status: "reserved", guests: 5, area: "Main hall", total: 0, course: "19:30 hold", accent: "lilac" },
  { id: 9, status: "occupied", guests: 4, area: "Main hall", total: 88, course: "Pending settlement", accent: "sky" },
  { id: 10, status: "available", guests: 0, area: "Patio", total: 0, course: "Ready", accent: "mint" },
  { id: 11, status: "occupied", guests: 2, area: "Window", total: 46, course: "Sent to kitchen", accent: "peach" },
  { id: 12, status: "available", guests: 0, area: "Patio", total: 0, course: "Ready", accent: "mint" },
  { id: 13, status: "occupied", guests: 5, area: "Main hall", total: 124, course: "Sent to kitchen", accent: "gold", source: "QR", settlementState: "SUBMITTED", memberName: "Gold Member" },
  { id: 14, status: "reserved", guests: 2, area: "Chef counter", total: 0, course: "20:00 hold", accent: "lilac" },
  { id: 15, status: "available", guests: 0, area: "Main hall", total: 0, course: "Ready", accent: "mint" },
  { id: 16, status: "occupied", guests: 4, area: "Private booth", total: 136, course: "Sent to kitchen", accent: "rose" },
  { id: 17, status: "available", guests: 0, area: "Window", total: 0, course: "Ready", accent: "mint" },
  { id: 18, status: "reserved", guests: 2, area: "Window", total: 0, course: "19:30 hold", accent: "lilac" },
  { id: 19, status: "occupied", guests: 3, area: "Patio", total: 63, course: "Sent to kitchen", accent: "peach" },
  { id: 20, status: "available", guests: 0, area: "Main hall", total: 0, course: "Ready", accent: "mint" },
  { id: 21, status: "occupied", guests: 4, area: "Main hall", total: 104, course: "Sent to kitchen", accent: "sky" },
  { id: 22, status: "reserved", guests: 6, area: "Private booth", total: 0, course: "20:15 hold", accent: "lilac" },
  { id: 23, status: "occupied", guests: 2, area: "Chef counter", total: 58, course: "Pending settlement", accent: "rose" },
  { id: 24, status: "occupied", guests: 4, area: "Indoor", total: 78.5, course: "Pending settlement", accent: "gold" }
];

const reservations: Reservation[] = [
  { id: 1, name: "Olivia Chen", time: "18:45", partySize: 2, status: "checked-in", area: "Window" },
  { id: 2, name: "Luca Martin", time: "19:00", partySize: 4, status: "waiting", area: "Main hall" },
  { id: 3, name: "Mia Rodriguez", time: "19:15", partySize: 6, status: "upcoming", area: "Private booth" },
  { id: 4, name: "Noah Patel", time: "19:30", partySize: 3, status: "waiting", area: "Patio" }
];

const menu: MenuItem[] = [
  {
    id: 1,
    skuId: 401,
    skuCode: "fried-rice-default",
    name: "招牌炒饭",
    category: "meals",
    price: 18,
    memberPrice: 16,
    image:
      "https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 2,
    skuId: 402,
    skuCode: "black-pepper-beef-rice-default",
    name: "黑椒牛肉饭",
    category: "meals",
    price: 34,
    memberPrice: 31,
    image:
      "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 3,
    skuId: 403,
    skuCode: "crispy-chicken-bites-default",
    name: "酥脆鸡块",
    category: "snacks",
    price: 16,
    image:
      "https://images.unsplash.com/photo-1562967916-eb82221dfb92?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 4,
    skuId: 404,
    skuCode: "white-peach-soda-default",
    name: "白桃气泡饮",
    category: "drinks",
    price: 12,
    memberPrice: 10,
    image:
      "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 5,
    skuId: 405,
    skuCode: "brown-sugar-milk-tea-default",
    name: "黑糖奶茶",
    category: "drinks",
    price: 14,
    memberPrice: 12,
    image:
      "https://images.unsplash.com/photo-1558857563-b371033873b8?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 6,
    skuId: 406,
    skuCode: "mango-pudding-default",
    name: "芒果布丁",
    category: "desserts",
    price: 15,
    image:
      "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 7,
    skuId: 407,
    skuCode: "chef-combo-default",
    name: "主厨套餐",
    category: "popular",
    price: 46,
    memberPrice: 42,
    image:
      "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 8,
    skuId: 408,
    skuCode: "truffle-fries-default",
    name: "松露薯条",
    category: "snacks",
    price: 19,
    image:
      "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=900&q=80"
  }
];

const initialOrder: OrderItem[] = [
  { ...menu[0], quantity: 1, note: "少葱" },
  { ...menu[4], quantity: 2 },
  { ...menu[3], quantity: 2, note: "少冰" }
];

const initialDraftOrders: Record<number, OrderItem[]> = {
  1: [
    { ...menu[1], quantity: 1, note: "加蛋" },
    { ...menu[3], quantity: 2 }
  ],
  5: [
    { ...menu[6], quantity: 2 },
    { ...menu[7], quantity: 1 }
  ],
  6: initialOrder,
  9: [
    { ...menu[0], quantity: 2 },
    { ...menu[5], quantity: 1 }
  ],
  11: [
    { ...menu[4], quantity: 2 },
    { ...menu[2], quantity: 1 }
  ]
};

const DRAFT_ORDERS_STORAGE_KEY = "pos-preview-table-draft-orders-v1";
const ORDER_STAGES_STORAGE_KEY = "pos-preview-table-order-stages-v1";
const STORE_ID = 101;
const QR_ORDERING_BASE_URL = import.meta.env.VITE_QR_ORDERING_URL_BASE ?? "http://localhost:4179";

function toQrOrderResponse(
  context: TableContextResponse,
  preview?: SettlementPreviewResponse | null
): QrCurrentOrderResponse | null {
  if (!context.currentActiveOrder) {
    return null;
  }

  return {
    activeOrderId: context.currentActiveOrder.activeOrderId,
    orderNo: context.currentActiveOrder.orderNo,
    storeId: context.currentActiveOrder.storeId,
    tableId: context.currentActiveOrder.tableId,
    tableCode: context.currentActiveOrder.tableCode,
    settlementStatus: context.currentActiveOrder.status,
    memberId: context.currentActiveOrder.memberId,
    memberName: preview?.member?.name ?? null,
    memberTier: preview?.member?.tier ?? null,
    originalAmountCents: preview?.pricing.originalAmountCents ?? context.currentActiveOrder.pricing.originalAmountCents,
    memberDiscountCents: preview?.pricing.memberDiscountCents ?? context.currentActiveOrder.pricing.memberDiscountCents,
    promotionDiscountCents:
      preview?.pricing.promotionDiscountCents ?? context.currentActiveOrder.pricing.promotionDiscountCents,
    payableAmountCents: preview?.pricing.payableAmountCents ?? context.currentActiveOrder.pricing.payableAmountCents,
    orderSource: context.currentActiveOrder.orderSource,
    items: context.currentActiveOrder.items.map((item) => ({
      skuId: item.skuId,
      skuCode: item.skuCode,
      productName: item.skuName,
      quantity: item.quantity,
      unitPriceCents: item.unitPriceCents,
      memberPriceCents: null
    }))
  };
}

function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY"
  }).format(value);
}

function getDraftOrderTotal(items: OrderItem[]) {
  return Number(items.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2));
}

function getInitialOrderStage(table: TableCard): OrderStage {
  if (table.source === "QR") {
    if (table.settlementState === "SETTLED") return "SETTLED";
    if (table.settlementState === "PENDING_SETTLEMENT") return "PENDING_SETTLEMENT";
    return "SUBMITTED";
  }

  if (table.status === "available") {
    return "DRAFT";
  }

  if (table.course.toLowerCase().includes("pending settlement")) {
    return "PENDING_SETTLEMENT";
  }

  return "SUBMITTED";
}

function getStageLabel(stage: OrderStage) {
  switch (stage) {
    case "DRAFT":
      return "Ordering";
    case "SUBMITTED":
      return "Sent to kitchen";
    case "PENDING_SETTLEMENT":
      return "Payment Pending";
    case "SETTLED":
      return "Settled";
    default:
      return stage;
  }
}

function getStageSupportText(stage: OrderStage) {
  switch (stage) {
    case "DRAFT":
      return "Items are still being edited for this active table order.";
    case "SUBMITTED":
      return "Submitted items are locked for kitchen, and you can still add new draft items for the same table.";
    case "PENDING_SETTLEMENT":
      return "Kitchen flow is complete. This order is now waiting for cashier payment collection.";
    case "SETTLED":
      return "This order is closed. The table can be cleaned and reopened.";
    default:
      return "";
  }
}

function canCollectPayment(stage: OrderStage) {
  return stage === "PENDING_SETTLEMENT";
}

function deriveOrderStage({
  table,
  backendOrder,
  submittedOrders,
  paymentPreview,
  draftItems
}: {
  table: TableCard;
  backendOrder?: QrCurrentOrderResponse | null;
  submittedOrders?: SubmittedOrderResponse[];
  paymentPreview?: SettlementPreviewResponse | null;
  draftItems?: OrderItem[];
}): OrderStage {
  if (paymentPreview || table.settlementState === "PENDING_SETTLEMENT") {
    return "PENDING_SETTLEMENT";
  }

  if (backendOrder?.settlementStatus === "SETTLED" || table.settlementState === "SETTLED") {
    return "SETTLED";
  }

  if (backendOrder?.settlementStatus === "PENDING_SETTLEMENT") {
    return "PENDING_SETTLEMENT";
  }

  if (backendOrder?.settlementStatus === "SUBMITTED") {
    return "SUBMITTED";
  }

  if ((submittedOrders?.length ?? 0) > 0) {
    return "SUBMITTED";
  }

  if (backendOrder?.settlementStatus === "DRAFT") {
    return "DRAFT";
  }

  if ((draftItems?.length ?? 0) > 0) {
    return "DRAFT";
  }

  return "DRAFT";
}

function mergeOrderItems(existing: OrderItem[], incoming: OrderItem[]) {
  const merged = new Map<string, OrderItem>();

  for (const item of existing) {
    merged.set(item.name, { ...item });
  }

  for (const item of incoming) {
    const current = merged.get(item.name);
    if (current) {
      merged.set(item.name, {
        ...current,
        quantity: current.quantity + item.quantity,
        note: current.note ?? item.note
      });
    } else {
      merged.set(item.name, { ...item });
    }
  }

  return Array.from(merged.values());
}

function createQrPayloadFromDraft(
  tableCode: string,
  items: OrderItem[],
  settlementStatus: OrderStage = "DRAFT",
  orderSource: "POS" | "QR" = "POS"
): QrCurrentOrderResponse {
  const payloadItems = items.map((entry) => ({
    skuId: entry.skuId,
    skuCode: entry.skuCode,
    productName: entry.name,
    quantity: entry.quantity,
    unitPriceCents: Math.round(entry.price * 100),
    memberPriceCents: entry.memberPrice ? Math.round(entry.memberPrice * 100) : null
  }));
  const originalAmountCents = Math.round(getDraftOrderTotal(items) * 100);

  return {
    activeOrderId: `draft-${tableCode}`,
    orderNo: `POS-DRAFT-${tableCode}`,
    storeId: STORE_ID,
    tableId: Number(tableCode.replace("T", "")),
    tableCode,
    orderSource,
    settlementStatus,
    memberId: null,
    memberName: null,
    memberTier: null,
    originalAmountCents,
    memberDiscountCents: 0,
    promotionDiscountCents: 0,
    payableAmountCents: originalAmountCents,
    items: payloadItems
  };
}

function loadStoredDraftOrders() {
  if (typeof window === "undefined") {
    return initialDraftOrders;
  }

  try {
    const raw = window.localStorage.getItem(DRAFT_ORDERS_STORAGE_KEY);
    if (!raw) {
      return initialDraftOrders;
    }

    const parsed = JSON.parse(raw) as Record<string, OrderItem[]>;
    return Object.fromEntries(
      Object.entries(parsed).map(([key, value]) => [Number(key), value])
    ) as Record<number, OrderItem[]>;
  } catch {
    return initialDraftOrders;
  }
}

function loadStoredOrderStages() {
  const initial = Object.fromEntries(
    tables.map((table) => [table.id, getInitialOrderStage(table)])
  ) as Record<number, OrderStage>;

  if (typeof window === "undefined") {
    return initial;
  }

  try {
    const raw = window.localStorage.getItem(ORDER_STAGES_STORAGE_KEY);
    if (!raw) {
      return initial;
    }

    const parsed = JSON.parse(raw) as Record<string, OrderStage>;
    return {
      ...initial,
      ...Object.fromEntries(
        Object.entries(parsed).map(([key, value]) => [Number(key), value])
      )
    };
  } catch {
    return initial;
  }
}

function mapSubmittedOrdersToRounds(
  orders: SubmittedOrderResponse[],
  menuByName: Map<string, MenuItem>
): SubmittedRound[] {
  return orders.map((order, index) => ({
    id: `submitted-${order.submittedOrderId}`,
    sentAt: `Submitted ${index + 1}`,
    items: order.items.map((item, itemIndex) => {
      const source = menuByName.get(item.skuName);
      return {
        id: source?.id ?? 9500 + itemIndex,
        skuId: source?.skuId ?? item.skuId,
        skuCode: source?.skuCode ?? item.skuCode,
        name: item.skuName,
        category: source?.category ?? "all",
        price: item.unitPriceCents / 100,
        memberPrice: source?.memberPrice,
        image:
          source?.image ??
          "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=900&q=80",
        quantity: item.quantity,
        note: item.remark ?? "已送厨"
      } satisfies OrderItem;
    }),
    total: Number((order.pricing.payableAmountCents / 100).toFixed(2))
  }));
}

async function fetchTableContext(tableCode: string) {
  const response = await fetch(
    `/api/v2/qr-ordering/context?storeCode=1001&tableCode=${encodeURIComponent(tableCode)}`
  );

  if (!response.ok) {
    return null;
  }

  const payload = (await response.json()) as ApiResponse<TableContextResponse>;
  return payload.code === 0 ? payload.data : null;
}

async function fetchSettlementPreview(activeOrderId: string) {
  const response = await fetch(`/api/v2/active-table-orders/${encodeURIComponent(activeOrderId)}/settlement-preview`);

  if (!response.ok) {
    return null;
  }

  const payload = (await response.json()) as ApiResponse<SettlementPreviewResponse>;
  return payload.code === 0 ? payload.data : null;
}

async function fetchSubmittedOrders(storeId: number, tableId: number) {
  const response = await fetch(`/api/v2/stores/${storeId}/tables/${tableId}/active-order/submitted-orders`);

  if (!response.ok) {
    return [] as SubmittedOrderResponse[];
  }

  const payload = (await response.json()) as ApiResponse<SubmittedOrderResponse[]>;
  return payload.code === 0 ? payload.data : [];
}

async function fetchTablePaymentPreview(storeId: number, tableId: number) {
  const response = await fetch(`/api/v2/stores/${storeId}/tables/${tableId}/payment/preview`);

  if (!response.ok) {
    return null;
  }

  const payload = (await response.json()) as ApiResponse<SettlementPreviewResponse>;
  return payload.code === 0 ? payload.data : null;
}

function App() {
  const [view, setView] = useState<View>("tables");
  const [activeCategory, setActiveCategory] = useState("all");
  const [search, setSearch] = useState("");
  const [tableState, setTableState] = useState<TableCard[]>(tables);
  const [selectedTableId, setSelectedTableId] = useState<number>(6);
  const [targetTableId, setTargetTableId] = useState<number>(1);
  const [tableDraftOrders, setTableDraftOrders] = useState<Record<number, OrderItem[]>>(loadStoredDraftOrders);
  const [submittedOrdersByTable, setSubmittedOrdersByTable] = useState<Record<number, SubmittedOrderResponse[]>>({});
  const [tablePaymentPreviews, setTablePaymentPreviews] = useState<Record<number, SettlementPreviewResponse | null>>({});
  const [tableOrderStages, setTableOrderStages] = useState<Record<number, OrderStage>>(loadStoredOrderStages);
  const [qrOrders, setQrOrders] = useState<Record<string, QrCurrentOrderResponse>>({});
  const [recentQrAlert, setRecentQrAlert] = useState<QrCurrentOrderResponse | null>(null);
  const seenQrOrderNos = useRef<Set<string>>(new Set());
  const pendingQrWrites = useRef<Set<string>>(new Set());
  const draftOrdersRef = useRef<Record<number, OrderItem[]>>(initialDraftOrders);

  const selectedTable = tableState.find((table) => table.id === selectedTableId) ?? tableState[0];
  const targetTable = tableState.find((table) => table.id === targetTableId) ?? tableState[0];
  const selectedTableCode = `T${selectedTable.id}`;
  const selectedBackendOrder = qrOrders[selectedTableCode];
  const selectedQrOrder =
    selectedBackendOrder?.orderSource === "QR" && selectedBackendOrder.settlementStatus !== "DRAFT"
      ? selectedBackendOrder
      : null;
  const selectedPosDraftOrder =
    selectedBackendOrder?.orderSource === "POS" && selectedBackendOrder.settlementStatus === "DRAFT"
      ? selectedBackendOrder
      : null;
  const selectedSubmittedOrders = submittedOrdersByTable[selectedTable.id] ?? [];
  const selectedTablePaymentPreview = tablePaymentPreviews[selectedTable.id] ?? null;
  const currentDraftForStage = tableDraftOrders[selectedTable.id] ?? [];
  const selectedOrderStage = deriveOrderStage({
    table: selectedTable,
    backendOrder: selectedBackendOrder,
    submittedOrders: selectedSubmittedOrders,
    paymentPreview: selectedTablePaymentPreview,
    draftItems: currentDraftForStage
  });
  const menuByName = useMemo(
    () => new Map(menu.map((item) => [item.name, item])),
    []
  );

  const visibleMenu = useMemo(() => {
    return menu.filter((item) => {
      const matchCategory =
        activeCategory === "all" ||
        item.category.toLowerCase() === activeCategory ||
        item.category.toLowerCase().includes(activeCategory);
      const matchSearch =
        search.trim() === "" ||
        `${item.name} ${item.category}`.toLowerCase().includes(search.toLowerCase());

      return matchCategory && matchSearch;
    });
  }, [activeCategory, search]);

  const draftOrderItems = tableDraftOrders[selectedTable.id] ?? [];
  const submittedRounds = useMemo(
    () => mapSubmittedOrdersToRounds(selectedSubmittedOrders, menuByName),
    [menuByName, selectedSubmittedOrders]
  );
  const submittedOrderItems = selectedSubmittedOrders.flatMap((order) =>
    order.items.map((item, index) => {
      const source = menuByName.get(item.skuName);
      return {
        id: source?.id ?? 9800 + index,
        skuId: source?.skuId ?? item.skuId,
        skuCode: source?.skuCode ?? item.skuCode,
        name: item.skuName,
        category: source?.category ?? "all",
        price: item.unitPriceCents / 100,
        memberPrice: source?.memberPrice,
        image:
          source?.image ??
          "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=900&q=80",
        quantity: item.quantity,
        note: item.remark ?? "已送厨"
      } satisfies OrderItem;
    })
  );
  const hasSubmittedRounds = selectedSubmittedOrders.length > 0;
  const subtotal = draftOrderItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const submittedSubtotal = submittedOrderItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const tax = subtotal * 0.09;
  const service = subtotal * 0.06;
  const memberDiscount = subtotal * 0.08;
  const promotionDiscount = subtotal >= 40 ? 6 : 0;
  const grossTotal = subtotal + tax + service;
  const total = grossTotal - memberDiscount - promotionDiscount;
  const memberProfile = {
    name: "Lina Chen",
    tier: "Gold Member",
    points: 2860,
    balance: 320
  };

  const splitGuests = selectedTable.guests || 4;
  const qrPendingTables = tableState.filter((table) => table.source === "QR" && table.status === "occupied");

  useEffect(() => {
    const qrTableCodes = tables.map((table) => `T${table.id}`);
    let active = true;

    const syncQrTables = async () => {
      try {
        const contexts = await Promise.all(qrTableCodes.map((tableCode) => fetchTableContext(tableCode)));

        if (!active) {
          return;
        }

        const previewEntries = await Promise.all(
          contexts.map(async (context) => {
            if (!context?.currentActiveOrder || context.currentActiveOrder.status !== "PENDING_SETTLEMENT") {
              return null;
            }

            const preview = await fetchSettlementPreview(context.currentActiveOrder.activeOrderId);
            return [context.tableCode, preview] as const;
          })
        );

        const previewMap = Object.fromEntries(
          previewEntries.filter((entry): entry is readonly [string, SettlementPreviewResponse | null] => Boolean(entry))
        );

        const nextQrOrders = Object.fromEntries(
          contexts
            .filter((entry): entry is TableContextResponse => Boolean(entry))
            .map((entry) => {
              const mappedOrder = toQrOrderResponse(entry, previewMap[entry.tableCode]);
              return [entry.tableCode, mappedOrder];
            })
            .filter((entry): entry is [string, QrCurrentOrderResponse] => Boolean(entry[1]))
        );

        setQrOrders(nextQrOrders);

        const newIncomingOrder = Object.values(nextQrOrders).find((entry) => !seenQrOrderNos.current.has(entry.orderNo));
        if (newIncomingOrder) {
          setRecentQrAlert(newIncomingOrder);
        }

        Object.values(nextQrOrders).forEach((entry) => {
          seenQrOrderNos.current.add(entry.orderNo);
        });

        setTableState((current) =>
          current.map((table) => {
            const match = nextQrOrders[`T${table.id}`];
            if (!match) {
              return table.source === "QR"
                ? {
                    ...tables.find((baseTable) => baseTable.id === table.id)!,
                    source: undefined,
                    settlementState: undefined,
                    memberName: undefined
                  }
                : table;
            }

            return {
              ...table,
              status: "occupied",
              source: match.orderSource,
              settlementState: match.settlementStatus,
              memberName: match.memberName ?? undefined,
              total: Number((match.payableAmountCents / 100).toFixed(2)),
              course:
                match.settlementStatus === "DRAFT"
                  ? "Draft order in progress"
                  : match.settlementStatus === "SUBMITTED"
                    ? "Sent to kitchen"
                      : match.settlementStatus === "PENDING_SETTLEMENT"
                      ? `${match.items.length} items payment pending`
                      : "Settled"
            };
          })
        );
        setTableOrderStages((current) => ({
          ...current,
          ...Object.fromEntries(
            Object.values(nextQrOrders).map((entry) => [
              Number(entry.tableCode.replace("T", "")),
              entry.settlementStatus === "PENDING_SETTLEMENT"
                ? "PENDING_SETTLEMENT"
                : entry.settlementStatus === "SETTLED"
                  ? "SETTLED"
                  : "SUBMITTED"
            ])
          )
        }));
      } catch {
        // Keep preview stable if backend is not available momentarily.
      }
    };

    void syncQrTables();
    const interval = window.setInterval(syncQrTables, 5000);

    return () => {
      active = false;
      window.clearInterval(interval);
    };
  }, []);

  const backendDraftItems = selectedPosDraftOrder
    ? selectedPosDraftOrder.items.map((item, index) => {
        const source = menuByName.get(item.productName);
        const unitPrice =
          (item.memberPriceCents ??
            item.unitPriceCents ??
            Math.round(
              selectedPosDraftOrder.payableAmountCents / Math.max(selectedPosDraftOrder.items.length, 1)
            )) / 100;

        return {
          id: source?.id ?? 9000 + index,
          skuId: source?.skuId ?? item.skuId,
          skuCode: source?.skuCode ?? item.skuCode,
          name: item.productName,
          category: source?.category ?? "qr",
          price: unitPrice,
          memberPrice: item.memberPriceCents ? item.memberPriceCents / 100 : source?.memberPrice,
          image: source?.image ?? "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=900&q=80",
          quantity: item.quantity,
          note: "草稿"
        } satisfies OrderItem;
      })
    : [];
  const displayedOrderItems = selectedPosDraftOrder ? backendDraftItems : draftOrderItems;
  const effectiveDraftOrderItems = displayedOrderItems;

  const paymentOrderItems = submittedOrderItems;

  const displayedSubtotal = selectedPosDraftOrder
    ? (selectedPosDraftOrder.originalAmountCents ?? selectedPosDraftOrder.payableAmountCents) / 100
    : subtotal;
  const displayedMemberDiscount = selectedPosDraftOrder
    ? (selectedPosDraftOrder.memberDiscountCents ?? 0) / 100
    : memberDiscount;
  const displayedPromotionDiscount = selectedPosDraftOrder
    ? (selectedPosDraftOrder.promotionDiscountCents ?? 0) / 100
    : promotionDiscount;
  const displayedTax = selectedPosDraftOrder ? 0 : tax;
  const displayedService = selectedPosDraftOrder ? 0 : service;
  const displayedGrossTotal = selectedPosDraftOrder ? displayedSubtotal : grossTotal;
  const displayedTotal = selectedPosDraftOrder ? selectedPosDraftOrder.payableAmountCents / 100 : total;
  const paymentSubtotal = selectedTablePaymentPreview
    ? selectedTablePaymentPreview.pricing.originalAmountCents / 100
    : selectedSubmittedOrders.reduce((sum, order) => sum + order.pricing.originalAmountCents / 100, 0);
  const paymentMemberDiscount = selectedTablePaymentPreview
    ? selectedTablePaymentPreview.pricing.memberDiscountCents / 100
    : selectedSubmittedOrders.reduce((sum, order) => sum + order.pricing.memberDiscountCents / 100, 0);
  const paymentPromotionDiscount = selectedTablePaymentPreview
    ? selectedTablePaymentPreview.pricing.promotionDiscountCents / 100
    : selectedSubmittedOrders.reduce((sum, order) => sum + order.pricing.promotionDiscountCents / 100, 0);
  const paymentTax = 0;
  const paymentService = 0;
  const paymentGrossTotal = paymentSubtotal;
  const paymentTotal = selectedTablePaymentPreview
    ? selectedTablePaymentPreview.pricing.payableAmountCents / 100
    : Math.max(paymentSubtotal - paymentMemberDiscount - paymentPromotionDiscount, 0);
  const perGuest = paymentTotal / splitGuests;
  const submittedRoundsTotal = selectedSubmittedOrders.reduce(
    (sum, order) => sum + order.pricing.payableAmountCents / 100,
    0
  );

  useEffect(() => {
    draftOrdersRef.current = tableDraftOrders;
  }, [tableDraftOrders]);

  useEffect(() => {
    window.localStorage.setItem(DRAFT_ORDERS_STORAGE_KEY, JSON.stringify(tableDraftOrders));
  }, [tableDraftOrders]);

  useEffect(() => {
    window.localStorage.setItem(ORDER_STAGES_STORAGE_KEY, JSON.stringify(tableOrderStages));
  }, [tableOrderStages]);

  const syncTableFromV2 = async (tableCode: string, tableId: number) => {
    const refreshedContext = await fetchTableContext(tableCode);
    if (!refreshedContext) {
      return null;
    }
    const backendSubmittedOrders = await fetchSubmittedOrders(refreshedContext.storeId, refreshedContext.tableId);
    const backendSubmittedRounds = mapSubmittedOrdersToRounds(backendSubmittedOrders, menuByName);

    const preview =
      refreshedContext.tableStatus === "PENDING_SETTLEMENT"
        ? await fetchTablePaymentPreview(refreshedContext.storeId, refreshedContext.tableId)
        : refreshedContext.currentActiveOrder?.status === "PENDING_SETTLEMENT"
          ? await fetchSettlementPreview(refreshedContext.currentActiveOrder.activeOrderId)
          : null;
    const nextOrder = toQrOrderResponse(refreshedContext, preview);

    setQrOrders((current) => {
      const next = { ...current };
      if (nextOrder) {
        next[tableCode] = nextOrder;
      } else {
        delete next[tableCode];
      }
      return next;
    });

    setTableState((current) =>
      current.map((table) => {
        if (table.id !== tableId) {
          return table;
        }

        if (!nextOrder) {
          if (backendSubmittedRounds.length > 0) {
            const submittedTotal = backendSubmittedRounds.reduce((sum, round) => sum + round.total, 0);
            const backendSource = backendSubmittedOrders.some((order) => order.sourceOrderType === "QR") ? "QR" : "POS";
            const isPaymentPending = refreshedContext.tableStatus === "PENDING_SETTLEMENT";
            return {
              ...table,
              status: "occupied",
              source: backendSource,
              settlementState: isPaymentPending ? "PENDING_SETTLEMENT" : "SUBMITTED",
              memberName: undefined,
              total: Number(submittedTotal.toFixed(2)),
              guests: table.guests || 2,
              course: isPaymentPending
                ? `${backendSubmittedRounds.reduce((sum, round) => sum + round.items.length, 0)} items payment pending`
                : `${backendSubmittedRounds.length} rounds sent to kitchen`
            };
          }

          return {
            ...table,
            status: "available",
            source: undefined,
            settlementState: undefined,
            memberName: undefined,
            total: 0,
            guests: 0,
            course: "Ready"
          };
        }

        return {
          ...table,
          status: "occupied",
          source: nextOrder.orderSource,
          settlementState: nextOrder.settlementStatus,
          memberName: nextOrder.memberName ?? undefined,
          total: Number((nextOrder.payableAmountCents / 100).toFixed(2)),
          guests: table.guests || 2,
          course:
            nextOrder.settlementStatus === "DRAFT"
              ? "Draft order in progress"
              : nextOrder.settlementStatus === "SUBMITTED"
                ? "Sent to kitchen"
                : nextOrder.settlementStatus === "PENDING_SETTLEMENT"
                  ? `${nextOrder.items.length} items payment pending`
                  : "Settled"
        };
      })
    );

    setTableOrderStages((current) => ({
      ...current,
      [tableId]:
        nextOrder?.settlementStatus === "SETTLED"
          ? "SETTLED"
          : nextOrder?.settlementStatus === "PENDING_SETTLEMENT"
            ? "PENDING_SETTLEMENT"
            : nextOrder?.settlementStatus === "SUBMITTED"
              ? "SUBMITTED"
              : refreshedContext.tableStatus === "PENDING_SETTLEMENT"
                ? "PENDING_SETTLEMENT"
              : backendSubmittedRounds.length > 0
                ? "SUBMITTED"
                : "DRAFT"
    }));

    setTableDraftOrders((current) => {
      if (!nextOrder || nextOrder.settlementStatus !== "DRAFT" || nextOrder.orderSource !== "POS") {
        if (!current[tableId]?.length) {
          return current;
        }

        const next = { ...current };
        next[tableId] = [];
        return next;
      }

      const mappedItems = nextOrder.items.map((item, index) => {
        const source = menuByName.get(item.productName);
        const unitPrice =
          (item.memberPriceCents ?? item.unitPriceCents ?? 0) / 100;

        return {
          id: source?.id ?? 9000 + index,
          skuId: source?.skuId ?? item.skuId,
          skuCode: source?.skuCode ?? item.skuCode,
          name: item.productName,
          category: source?.category ?? "all",
          price: unitPrice,
          memberPrice: item.memberPriceCents ? item.memberPriceCents / 100 : source?.memberPrice,
          image:
            source?.image ??
            "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=900&q=80",
          quantity: item.quantity,
          note: source?.category ?? "加菜"
        } satisfies OrderItem;
      });

      return {
        ...current,
        [tableId]: mappedItems
      };
    });

    setSubmittedOrdersByTable((current) => ({
      ...current,
      [tableId]: backendSubmittedOrders
    }));

    setTablePaymentPreviews((current) => ({
      ...current,
      [tableId]: preview
    }));

    return nextOrder;
  };

  const persistQrOrderItems = async (
    nextQrOrder: QrCurrentOrderResponse,
    tableCode = selectedTableCode,
    tableId = selectedTable.id
  ) => {
    const tableContext = await fetchTableContext(tableCode);
    if (!tableContext) {
      return;
    }

    pendingQrWrites.current.add(tableCode);

    try {
      const response = await fetch(
        `/api/v2/stores/${tableContext.storeId}/tables/${tableContext.tableId}/active-order/items`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            orderSource: nextQrOrder.orderSource,
            memberId: nextQrOrder.memberId,
            items: nextQrOrder.items.map((item) => ({
              skuId: item.skuId,
              skuCode: item.skuCode,
              skuName: item.productName,
              quantity: item.quantity,
              unitPriceCents: item.unitPriceCents,
              remark: ""
            }))
          })
        }
      );

      if (!response.ok) {
        throw new Error("Failed to update active order");
      }

      const payload = (await response.json()) as ApiResponse<ActiveTableOrderDto>;
      if (payload.code !== 0 || !payload.data) {
        throw new Error(payload.message || "Failed to update active order");
      }

      await syncTableFromV2(tableCode, tableId);
    } finally {
      pendingQrWrites.current.delete(tableCode);
    }
  };

  const clearCurrentTableOrder = async (activeOrderId: string) => {
    await fetch(
      `/api/v2/stores/${STORE_ID}/tables/${selectedTable.id}/active-order/${encodeURIComponent(activeOrderId)}/empty-draft`,
      { method: "DELETE" }
    );
  };

  const addItem = (item: MenuItem) => {
    const currentItems = effectiveDraftOrderItems;
    const nextItems = currentItems.find((entry) => entry.id === item.id)
      ? currentItems.map((entry) =>
          entry.id === item.id ? { ...entry, quantity: entry.quantity + 1 } : entry
        )
      : [...currentItems, { ...item, quantity: 1, note: "加菜" }];

    setTableDraftOrders((current) => {
      return {
        ...current,
        [selectedTable.id]: nextItems
      };
    });
    setTableOrderStages((current) => ({
      ...current,
      [selectedTable.id]: hasSubmittedRounds ? "SUBMITTED" : "DRAFT"
    }));
    setTableState((current) =>
      current.map((table) =>
        table.id === selectedTable.id
          ? {
              ...table,
              status: "occupied",
              source: table.source === "QR" && hasSubmittedRounds ? "QR" : "POS",
              guests: table.guests || 2,
              total: Number((submittedRoundsTotal + getDraftOrderTotal(nextItems)).toFixed(2)),
              course:
                hasSubmittedRounds
                  ? `${submittedRounds.length} rounds sent · drafting add-ons`
                  : "Draft order in progress"
            }
          : table
      )
    );
    void persistQrOrderItems(
      createQrPayloadFromDraft(
        selectedTableCode,
        nextItems,
        "DRAFT",
        "POS"
      ),
      selectedTableCode,
      selectedTable.id
    );
  };

  const updateQuantity = (id: number, delta: number) => {
    const previewNextItems = effectiveDraftOrderItems
      .map((entry) =>
        entry.id === id ? { ...entry, quantity: Math.max(0, entry.quantity + delta) } : entry
      )
      .filter((entry) => entry.quantity > 0);

    setTableDraftOrders((current) => ({
      ...current,
      [selectedTable.id]: previewNextItems
    }));
    setTableOrderStages((current) => ({
      ...current,
      [selectedTable.id]: hasSubmittedRounds ? "SUBMITTED" : "DRAFT"
    }));
    setTableState((current) =>
      current.map((table) =>
        table.id === selectedTable.id
          ? {
              ...table,
              status: previewNextItems.length > 0 ? "occupied" : "available",
              source:
                previewNextItems.length > 0
                  ? table.source === "QR" && hasSubmittedRounds ? "QR" : "POS"
                  : undefined,
              guests: previewNextItems.length > 0 ? table.guests || 2 : 0,
              total: Number((submittedRoundsTotal + getDraftOrderTotal(previewNextItems)).toFixed(2)),
              course:
                previewNextItems.length > 0
                  ? hasSubmittedRounds
                    ? `${submittedRounds.length} rounds sent · drafting add-ons`
                    : "Draft order in progress"
                  : hasSubmittedRounds
                    ? `${submittedRounds.length} rounds sent to kitchen`
                    : "Ready"
            }
          : table
      )
    );

    if (previewNextItems.length === 0) {
      if (selectedBackendOrder?.activeOrderId && selectedOrderStage === "DRAFT") {
        void clearCurrentTableOrder(selectedBackendOrder.activeOrderId).then(() =>
          syncTableFromV2(selectedTableCode, selectedTable.id)
        );
      }
      return;
    }

    void persistQrOrderItems(
      createQrPayloadFromDraft(
        selectedTableCode,
        previewNextItems,
        "DRAFT",
        "POS"
      ),
      selectedTableCode,
      selectedTable.id
    );
  };

  const chooseTable = (table: TableCard) => {
    setSelectedTableId(table.id);
    setView("ordering");
  };

  const sendToKitchen = async () => {
    const currentDraftItems = effectiveDraftOrderItems;
    if (currentDraftItems.length === 0) {
      return;
    }
    let activeOrderId = selectedPosDraftOrder?.activeOrderId;
    let storeId = selectedPosDraftOrder?.storeId ?? STORE_ID;
    let tableId = selectedPosDraftOrder?.tableId ?? selectedTable.id;

    if (!activeOrderId) {
      await persistQrOrderItems(
        createQrPayloadFromDraft(selectedTableCode, currentDraftItems, "DRAFT", "POS"),
        selectedTableCode,
        selectedTable.id
      );
      const latestDraft = await syncTableFromV2(selectedTableCode, selectedTable.id);
      activeOrderId = latestDraft?.activeOrderId;
      storeId = latestDraft?.storeId ?? storeId;
      tableId = latestDraft?.tableId ?? tableId;
    }

    if (!activeOrderId) {
      return;
    }

    await fetch(
      `/api/v2/stores/${storeId}/tables/${tableId}/active-order/${encodeURIComponent(activeOrderId)}/submit-to-kitchen`,
      { method: "POST" }
    );
    setTableDraftOrders((current) => ({
      ...current,
      [selectedTable.id]: []
    }));
    await syncTableFromV2(selectedTableCode, selectedTable.id);
    setView("ordering");
  };

  const moveToSettlement = async () => {
    if (!hasSubmittedRounds || effectiveDraftOrderItems.length > 0) {
      return;
    }

    await fetch(
      `/api/v2/stores/${STORE_ID}/tables/${selectedTable.id}/payment`,
      { method: "POST" }
    );
    const preview = await fetchTablePaymentPreview(STORE_ID, selectedTable.id);
    setTablePaymentPreviews((current) => ({
      ...current,
      [selectedTable.id]: preview
    }));
    await syncTableFromV2(selectedTableCode, selectedTable.id);
    setView("payment");
  };

  const completeSettlement = async () => {
    await fetch(`/api/v2/stores/${STORE_ID}/tables/${selectedTable.id}/payment/collect`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        cashierId: 9001,
        paymentMethod: "CASHIER_POS",
        collectedAmountCents: Math.round(paymentTotal * 100)
      })
    });
    setTablePaymentPreviews((current) => ({
      ...current,
      [selectedTable.id]: null
    }));
    await syncTableFromV2(selectedTableCode, selectedTable.id);

    setView("success");
  };

  const statusSummary = [
    { label: "Occupied", value: tableState.filter((table) => table.status === "occupied").length, tone: "sky" },
    { label: "Reserved", value: tableState.filter((table) => table.status === "reserved").length, tone: "rose" },
    { label: "Available", value: tableState.filter((table) => table.status === "available").length, tone: "mint" }
  ];

  const reservationSummary = [
    { label: "Checked in", value: reservations.filter((entry) => entry.status === "checked-in").length, tone: "mint" },
    { label: "Waiting", value: reservations.filter((entry) => entry.status === "waiting").length, tone: "peach" },
    { label: "Upcoming", value: reservations.filter((entry) => entry.status === "upcoming").length, tone: "lilac" }
  ];

  const availableTables = tableState.filter((table) => table.status === "available");

  const windowTitle =
    view === "reservations"
      ? "Reservations and waitlist"
      : view === "tables"
        ? "Table management"
        : view === "transfer"
          ? "Transfer table"
          : view === "ordering"
            ? "Ordering"
            : view === "review"
              ? "Order review"
              : view === "split"
                ? "Split bill"
                : view === "payment"
                  ? "Payment"
                  : "Payment success";

  return (
    <div className="app-shell">
      <main className="preview-stage">
        <section className="pos-window">
          <header className="window-header">
            <div className="header-spacer" />
            <strong>{windowTitle}</strong>
            <div className="header-actions">
              <button
                className="persistent-nav-button persistent-nav-button-secondary"
                onClick={() =>
                  window.open(
                    `${QR_ORDERING_BASE_URL}/?storeName=Riverside%20Branch&storeCode=1001&table=${encodeURIComponent(selectedTableCode)}`,
                    "_blank"
                  )
                }
              >
                打开 {selectedTableCode} 扫码页
              </button>
            </div>
          </header>

          {recentQrAlert ? (
            <div className="incoming-qr-alert">
              <div>
                <p className="table-tag">New QR order pending settlement</p>
                <h3>
                  {recentQrAlert.tableCode} · {recentQrAlert.orderNo}
                </h3>
                <p>
                  {recentQrAlert.items.length} items · {formatMoney(recentQrAlert.payableAmountCents / 100)} waiting for cashier
                </p>
              </div>
              <div className="incoming-qr-actions">
                <button
                  className="minor-pill"
                  onClick={() => {
                    const tableId = Number(recentQrAlert.tableCode.replace("T", ""));
                    if (!Number.isNaN(tableId)) {
                      setSelectedTableId(tableId);
                    }
                    setView("ordering");
                    setRecentQrAlert(null);
                  }}
                >
                  Open order
                </button>
                <button className="minor-pill" onClick={() => setRecentQrAlert(null)}>
                  Dismiss
                </button>
              </div>
            </div>
          ) : null}

          <div className="persistent-nav persistent-nav-top">
            {[
              ["tables", "Table Management"],
              ["ordering", "Ordering"]
            ].map(([value, label]) => (
              <button
                key={value}
                className={`persistent-nav-button ${view === value ? "persistent-nav-button-active" : ""}`}
                onClick={() => setView(value as View)}
              >
                {label}
              </button>
            ))}
          </div>

          <div className="window-content">
          {view === "reservations" && (
            <div className="tables-layout">
              <section className="table-board">
                <div className="section-heading">
                  <div>
                    <h2>Host stand</h2>
                    <div className="heading-underline" />
                  </div>
                  <div className="utility-group">
                    <button className="utility-button">+</button>
                    <button className="utility-button" onClick={() => setView("tables")}>
                      →
                    </button>
                  </div>
                </div>

                <div className="status-strip">
                  {reservationSummary.map((item) => (
                    <article key={item.label} className={`status-card tone-${item.tone}`}>
                      <span>{item.label}</span>
                      <strong>{item.value}</strong>
                    </article>
                  ))}
                </div>

                <div className="reservation-list">
                  {reservations.map((entry) => (
                    <article key={entry.id} className="reservation-card">
                      <div className="reservation-main">
                        <div>
                          <p className="table-tag">{entry.time}</p>
                          <h3>{entry.name}</h3>
                          <p className="reservation-meta">
                            {entry.partySize} guests · {entry.area}
                          </p>
                        </div>
                        <span className={`reservation-badge badge-${entry.status}`}>
                          {entry.status}
                        </span>
                      </div>
                      <div className="reservation-actions">
                        <button className="minor-pill">Seat guests</button>
                        <button className="minor-pill">Edit</button>
                      </div>
                    </article>
                  ))}
                </div>
              </section>

              <aside className="table-sidebar">
                <h2>Waitlist tools</h2>
                <div className="detail-card">
                  <div className="detail-row">
                    <span>Walk-ins</span>
                    <strong>3 parties</strong>
                  </div>
                  <div className="detail-row">
                    <span>Average wait</span>
                    <strong>18 min</strong>
                  </div>
                  <div className="detail-row">
                    <span>Next table</span>
                    <strong>Table 12</strong>
                  </div>
                </div>

                <div className="sidebar-actions">
                  <button className="sort-row">
                    <span className="sort-icon" />
                    <span>Add walk-in</span>
                  </button>
                  <button className="sort-row">
                    <span className="sort-icon" />
                    <span>Notify next guest</span>
                  </button>
                  <button className="sort-row" onClick={() => setView("tables")}>
                    <span className="sort-icon" />
                    <span>Open floor panel</span>
                  </button>
                </div>
              </aside>
            </div>
          )}

          {view === "tables" && (
            <div className="floorplan-layout">
              <section className="floorplan-shell">
                <div className="floorplan-board">
                  <div className="floorplan-meta">
                    <div className="floorplan-legend">
                      <span className="legend-chip legend-available">Available</span>
                      <span className="legend-chip legend-occupied">Occupied</span>
                      <span className="legend-chip legend-reserved">Reserved</span>
                      <span className="legend-chip legend-billing">Billing</span>
                      <span className="legend-chip legend-attention">Action needed</span>
                    </div>
                    <div className="floorplan-summary">
                      <strong>{tableState.length} tables</strong>
                      <span>{tableState.filter((table) => table.status === "occupied").length} active now</span>
                    </div>
                  </div>

                  <div className="qr-order-strip">
                    <div>
                      <p className="table-tag">QR pending settlement</p>
                      <h3>{qrPendingTables.length} tables synced from table code orders</h3>
                    </div>
                    <div className="qr-order-chips">
                      {qrPendingTables.slice(0, 4).map((table) => (
                        <button key={table.id} className="qr-order-chip" onClick={() => chooseTable(table)}>
                          T{table.id}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="floorplan-map">
                    <aside className="floorplan-service-strip">
                      <div className="service-node">
                        <span>Host</span>
                        <strong>Front</strong>
                      </div>
                      <div className="service-node">
                        <span>Pickup</span>
                        <strong>Pass</strong>
                      </div>
                      <div className="service-node">
                        <span>Cashier</span>
                        <strong>POS</strong>
                      </div>
                    </aside>

                    <div className="floorplan-islands">
                      {[tableState.slice(0, 8), tableState.slice(8, 16), tableState.slice(16, 24)].map(
                        (group, index) => (
                          <div key={index} className="floorplan-island">
                            <div className="floorplan-grid">
                              {group.map((table) => {
                                const tableStage = deriveOrderStage({
                                  table,
                                  backendOrder: qrOrders[`T${table.id}`],
                                  submittedOrders: submittedOrdersByTable[table.id] ?? [],
                                  paymentPreview: tablePaymentPreviews[table.id] ?? null,
                                  draftItems: tableDraftOrders[table.id] ?? []
                                });
                                const tone =
                                  table.status === "available"
                                    ? "available"
                                    : table.status === "reserved"
                                      ? "reserved"
                                      : tableStage === "PENDING_SETTLEMENT"
                                        ? "billing"
                                        : tableStage === "SETTLED"
                                          ? "neutral"
                                          : "occupied";

                                return (
                                  <button
                                    key={table.id}
                                    className={`floor-table floor-table-${tone} ${
                                      selectedTable.id === table.id ? "floor-selected" : ""
                                    }`}
                                    onClick={() => chooseTable(table)}
                                  >
                                    {table.source === "QR" ? <span className="floor-source-badge">QR</span> : null}
                                    <strong>T{table.id}</strong>
                                    <span>
                                      {table.status === "available"
                                        ? "Available"
                                        : table.status === "reserved"
                                          ? "Reserved"
                                          : `${getStageLabel(tableStage)} · ${table.total > 0 ? formatMoney(table.total) : "Open"}`}
                                    </span>
                                  </button>
                                );
                              })}
                            </div>
                          </div>
                        )
                      )}
                    </div>
                  </div>
                </div>
              </section>
            </div>
          )}

          {view === "transfer" && (
            <div className="review-layout">
              <section className="payment-summary">
                <div className="summary-topbar">
                  <div>
                    <p className="table-tag">Current table {selectedTable.id}</p>
                    <h2>Transfer table</h2>
                  </div>
                  <button className="minor-pill" onClick={() => setView("tables")}>
                    Back to floor
                  </button>
                </div>

                <div className="detail-card transfer-banner">
                  <div className="detail-row">
                    <span>Guests</span>
                    <strong>{selectedTable.guests}</strong>
                  </div>
                  <div className="detail-row">
                    <span>Current check</span>
                    <strong>{formatMoney(displayedTotal)}</strong>
                  </div>
                  <div className="detail-row">
                    <span>Reason</span>
                    <strong>Move to quieter zone</strong>
                  </div>
                </div>

                <div className="transfer-grid">
                  {availableTables.map((table) => (
                    <button
                      key={table.id}
                      className={`table-card table-${table.accent} ${
                        targetTable.id === table.id ? "table-selected" : ""
                      }`}
                      onClick={() => setTargetTableId(table.id)}
                    >
                      <div className="table-card-top">
                        <span>T{table.id}</span>
                        <strong className={`table-status status-${table.status}`}>{table.status}</strong>
                      </div>
                      <div className="table-card-main">
                        <h3>--</h3>
                      </div>
                      <div className="table-card-foot">
                        <span>Ready</span>
                        <strong>Free</strong>
                      </div>
                    </button>
                  ))}
                </div>
              </section>

              <aside className="review-panel">
                <div className="payment-card">
                  <p className="sidebar-title">Transfer target</p>
                  <div className="detail-row">
                    <span>Destination</span>
                    <strong>Table {targetTable.id}</strong>
                  </div>
                  <div className="detail-row">
                    <span>Area</span>
                    <strong>{targetTable.area}</strong>
                  </div>
                  <div className="detail-row">
                    <span>Status</span>
                    <strong>{targetTable.status}</strong>
                  </div>
                </div>

                <div className="payment-card accent-card">
                  <p className="sidebar-title">Confirm transfer</p>
                  <h3>Move order to Table {targetTable.id}</h3>
                  <p className="accent-copy">
                    All active items, guest count, and bill ownership will follow the party to the
                    selected table.
                  </p>
                  <button className="cta-button" onClick={() => setView("tables")}>
                    Confirm move
                  </button>
                </div>
              </aside>
            </div>
          )}

          {view === "ordering" && (
            <div className="pos-body">
              <section className="menu-section">
                <div className="section-heading">
                  <div className="section-heading-main">
                    <div className="heading-topline">
                      <div className="current-table-badge">
                        <span className="current-table-label">Current table</span>
                        <strong>T{selectedTable.id}</strong>
                      </div>
                      <button className="minor-pill order-stage-pill">{getStageLabel(selectedOrderStage)}</button>
                      <span className="ordering-inline-copy">{getStageSupportText(selectedOrderStage)}</span>
                    </div>
                  </div>
                </div>

                <div className="category-grid">
                  <button
                    className={activeCategory === "all" ? "category-card active-all" : "category-card"}
                    onClick={() => setActiveCategory("all")}
                  >
                    <span>All Items</span>
                    <strong>{menu.length}</strong>
                  </button>
                  {categories.map((category) => (
                    <button
                      key={category.id}
                      className={`category-card tile-${category.color} ${
                        activeCategory === category.id ? "active-tile" : ""
                      }`}
                      onClick={() => setActiveCategory(category.id)}
                    >
                      <span>{category.name}</span>
                      <strong>{category.items}</strong>
                    </button>
                  ))}
                </div>

                <div className="menu-search-row">
                  <input
                    aria-label="Search menu"
                    placeholder="Search item"
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                  />
                </div>

                <div className="menu-grid">
                  {visibleMenu.map((item) => (
                    <article key={item.id} className="dish-card" onClick={() => addItem(item)}>
                      <img src={item.image} alt={item.name} />
                      <div className="dish-overlay">
                        <div>
                          <h3>{item.name}</h3>
                          <p>{formatMoney(item.price)}</p>
                        </div>
                        <button className="add-chip">+</button>
                      </div>
                    </article>
                  ))}
                  {Array.from({ length: 4 }).map((_, index) => (
                    <div key={index} className="empty-slot">
                      +
                    </div>
                  ))}
                </div>
              </section>

              <aside className="sidebar-panel ordering-sidebar">
                <div className="ordering-panel-card">
                  <div className="ordering-panel-head">
                    <div>
                      <p className="sidebar-title">Current order</p>
                      <h2>Cart</h2>
                    </div>
                    <span className="cart-pill">{displayedOrderItems.length} items</span>
                  </div>

                  <div className="ordering-cart-list">
                    {displayedOrderItems.map((item) => (
                      <article key={item.id} className="ordering-cart-row">
                        <div>
                          <strong>{item.name}</strong>
                          <p>{item.note ?? item.category}</p>
                        </div>
                        <div className="qty-control">
                          <button onClick={() => updateQuantity(item.id, -1)}>-</button>
                          <span>{item.quantity}</span>
                          <button onClick={() => updateQuantity(item.id, 1)}>+</button>
                        </div>
                      </article>
                    ))}
                  </div>
                </div>

                <div className="ordering-panel-card">
                  <p className="sidebar-title">Actions</p>
                  <button className="sort-row">
                    <span className="sort-icon" />
                    <span>Add note</span>
                  </button>
                  <button className="sort-row">
                    <span className="sort-icon" />
                    <span>Apply discount</span>
                  </button>
                  {effectiveDraftOrderItems.length > 0 ? (
                    <button className="sort-row" onClick={sendToKitchen}>
                      <span className="sort-icon" />
                      <span>Send to kitchen</span>
                    </button>
                  ) : null}
                  {hasSubmittedRounds && effectiveDraftOrderItems.length === 0 && selectedOrderStage !== "PENDING_SETTLEMENT" ? (
                    <button className="sort-row" onClick={moveToSettlement}>
                      <span className="sort-icon" />
                      <span>Payment</span>
                    </button>
                  ) : null}
                  {selectedOrderStage === "PENDING_SETTLEMENT" ? (
                    <button className="sort-row" onClick={() => setView("payment")}>
                      <span className="sort-icon" />
                      <span>Collect payment</span>
                    </button>
                  ) : null}
                </div>

                {hasSubmittedRounds ? (
                  <div className="ordering-panel-card">
                    <div className="ordering-panel-head">
                      <div>
                        <p className="sidebar-title">Sent to kitchen</p>
                        <h2>Submitted rounds</h2>
                      </div>
                      <span className="cart-pill">{selectedSubmittedOrders.length} rounds</span>
                    </div>

                    <div className="ordering-cart-list">
                      {submittedRounds.map((round, index) => (
                        <article key={round.id} className="ordering-cart-row">
                          <div>
                            <strong>Round {index + 1}</strong>
                            <p>
                              {round.sentAt} · {round.items.map((item) => `${item.name} x${item.quantity}`).join(" / ")}
                            </p>
                          </div>
                          <strong>{formatMoney(round.total)}</strong>
                        </article>
                      ))}
                    </div>
                  </div>
                ) : null}

                <div className="ordering-panel-card">
                  <p className="sidebar-title">Summary</p>
                  <div className="amount-row">
                    <span>Subtotal</span>
                    <strong>{formatMoney(displayedSubtotal)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Tax</span>
                    <strong>{formatMoney(displayedTax)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Service</span>
                    <strong>{formatMoney(displayedService)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Member benefit</span>
                    <strong>-{formatMoney(displayedMemberDiscount)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Promotion hit</span>
                    <strong>-{formatMoney(displayedPromotionDiscount)}</strong>
                  </div>
                  <div className="amount-row total">
                    <span>Payable</span>
                    <strong>{formatMoney(displayedTotal)}</strong>
                  </div>
                </div>
              </aside>
            </div>
          )}

          {view === "review" && (
            <div className="review-layout">
              <section className="payment-summary">
                <div className="summary-topbar">
                  <div>
                    <div className="current-table-badge">
                      <span className="current-table-label">Current table</span>
                      <strong>T{selectedTable.id}</strong>
                    </div>
                    <h2>Order review</h2>
                    <p className="stage-support-copy">{getStageSupportText(selectedOrderStage)}</p>
                  </div>
                  <button className="minor-pill" onClick={() => setView("ordering")}>
                    Continue ordering
                  </button>
                </div>

                <div className="guest-strip">
                  <span>{selectedTable.guests || 4} guests</span>
                  <span>{selectedTable.area}</span>
                  <span>{selectedTable.source === "QR" ? "QR table order" : "Server Maya"}</span>
                  <span>{selectedQrOrder?.memberName ?? selectedTable.memberName ?? memberProfile.name}</span>
                  <span>{getStageLabel(selectedOrderStage)}</span>
                </div>

                {selectedTable.source === "QR" ? (
                  <div className="qr-checkout-banner">
                    <div>
                      <p className="table-tag">QR settlement handoff</p>
                      <h3>Customer has already placed items from the table code</h3>
                      <p className="reservation-meta">
                        Review the synced basket, confirm discounts, then collect payment from staff POS.
                      </p>
                    </div>
                    <span className="reservation-badge badge-waiting">
                      {selectedOrderStage === "PENDING_SETTLEMENT" ? "PAYMENT_PENDING" : selectedTable.settlementState ?? "SUBMITTED"}
                    </span>
                  </div>
                ) : null}

                <div className="order-card-list">
                  {paymentOrderItems.map((item) => (
                    <article key={item.id} className="order-row-card">
                      <img src={item.image} alt={item.name} />
                      <div className="order-row-main">
                        <div>
                          <strong>{item.name}</strong>
                          <p>{item.note ?? item.category}</p>
                        </div>
                        <strong>{item.quantity} items</strong>
                      </div>
                      <strong className="row-price">
                        {formatMoney(item.quantity * item.price)}
                      </strong>
                    </article>
                  ))}
                </div>
              </section>

              <aside className="review-panel">
                <div className="payment-card">
                  <p className="sidebar-title">Review summary</p>
                  <div className="amount-row">
                    <span>Subtotal</span>
                    <strong>{formatMoney(displayedSubtotal)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Tax</span>
                    <strong>{formatMoney(displayedTax)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Service</span>
                    <strong>{formatMoney(displayedService)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Member discount</span>
                    <strong>-{formatMoney(displayedMemberDiscount)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Full reduction</span>
                    <strong>-{formatMoney(displayedPromotionDiscount)}</strong>
                  </div>
                  <div className="amount-row total">
                    <span>Payable</span>
                    <strong>{formatMoney(displayedTotal)}</strong>
                  </div>
                </div>

                <div className="payment-card">
                  <p className="sidebar-title">Actions</p>
                  <button className="sort-row">
                    <span className="sort-icon" />
                    <span>Member recharge</span>
                  </button>
                  <button className="sort-row" onClick={() => setView("split")}>
                    <span className="sort-icon" />
                    <span>Split bill</span>
                  </button>
                  {hasSubmittedRounds && effectiveDraftOrderItems.length === 0 && selectedOrderStage !== "PENDING_SETTLEMENT" ? (
                    <button className="sort-row" onClick={moveToSettlement}>
                      <span className="sort-icon" />
                      <span>Payment</span>
                    </button>
                  ) : null}
                  {selectedOrderStage === "PENDING_SETTLEMENT" ? (
                    <button className="cta-button" onClick={() => setView("payment")}>
                      Collect payment
                    </button>
                  ) : null}
                </div>
              </aside>
            </div>
          )}

          {view === "split" && (
            <div className="review-layout">
              <section className="payment-summary">
                <div className="summary-topbar">
                  <div>
                    <div className="current-table-badge">
                      <span className="current-table-label">Current table</span>
                      <strong>T{selectedTable.id}</strong>
                    </div>
                    <h2>Split bill</h2>
                  </div>
                  <button className="minor-pill" onClick={() => setView("ordering")}>
                    Back to ordering
                  </button>
                </div>

                <div className="split-grid">
                  {Array.from({ length: splitGuests }).map((_, index) => (
                    <article key={index} className="split-card">
                      <div className="split-head">
                        <span>Guest {index + 1}</span>
                        <strong>{formatMoney(perGuest)}</strong>
                      </div>
                      <div className="split-tags">
                        {displayedOrderItems.slice(index % 2, index % 2 === 0 ? 2 : 3).map((item) => (
                          <span key={`${item.id}-${index}`}>{item.name}</span>
                        ))}
                      </div>
                    </article>
                  ))}
                </div>
              </section>

              <aside className="review-panel">
                <div className="payment-card">
                  <p className="sidebar-title">Split method</p>
                  <div className="method-grid single-column">
                    <button className="method-button active">Equal split</button>
                    <button className="method-button">By seat</button>
                    <button className="method-button">Custom amount</button>
                  </div>
                </div>

                <div className="payment-card accent-card">
                  <p className="sidebar-title">Per guest</p>
                  <h3>{formatMoney(perGuest)}</h3>
                  <p className="accent-copy">
                    Each guest can tap separately on the reader. Service and tax stay evenly
                    distributed unless you switch to custom split.
                  </p>
                  <button className="cta-button" onClick={() => setView("payment")}>
                    Continue to collection
                  </button>
                </div>
              </aside>
            </div>
          )}

          {view === "payment" && (
            <div className="payment-layout">
              <section className="payment-summary">
                <div className="summary-topbar">
                  <div>
                    <div className="current-table-badge">
                      <span className="current-table-label">Current table</span>
                      <strong>T{selectedTable.id}</strong>
                    </div>
                    <h2>Cashier settlement</h2>
                    <p className="stage-support-copy">{getStageSupportText(selectedOrderStage)}</p>
                  </div>
                  <button className="minor-pill" onClick={() => setView("ordering")}>
                    Back to ordering
                  </button>
                </div>

                <div className="payment-hero">
                  <div className="payment-card feature-card">
                    <p className="sidebar-title">Ready to charge</p>
                    <h3>{formatMoney(paymentTotal)}</h3>
                    <p className="accent-copy">
                      {selectedTable.source === "QR"
                        ? "A QR table order has already been placed by the guest. Confirm the basket and collect payment at cashier."
                        : "This table order is ready to settle. Confirm the order summary, then collect payment at cashier."}
                    </p>
                  </div>
                </div>

                {selectedTable.source === "QR" ? (
                  <div className="qr-checkout-banner qr-checkout-banner-light">
                    <div>
                      <p className="table-tag">Source</p>
                      <h3>Table code order · T{selectedTable.id}</h3>
                      <p className="reservation-meta">
                        {selectedTable.memberName ?? "Guest"} · {selectedTable.settlementState ?? "PENDING_SETTLEMENT"}
                      </p>
                    </div>
                    <span className="reservation-badge badge-checked-in">SYNCED</span>
                  </div>
                ) : null}

                <div className="payment-card settlement-breakdown">
                  <div className="amount-row">
                    <span>Original amount</span>
                    <strong>{formatMoney(paymentGrossTotal)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Member benefit</span>
                    <strong>-{formatMoney(paymentMemberDiscount)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Promotion benefit</span>
                    <strong>-{formatMoney(paymentPromotionDiscount)}</strong>
                  </div>
                  {(selectedTablePaymentPreview?.giftItems ?? []).map((gift) => (
                    <div className="amount-row" key={gift.skuName}>
                      <span>Gift item</span>
                      <strong>
                        {gift.skuName} x{gift.quantity}
                      </strong>
                    </div>
                  ))}
                </div>

                <div className="order-card-list compact-list">
                  {paymentOrderItems.map((item) => (
                    <article key={item.id} className="order-row-card compact-row">
                      <img src={item.image} alt={item.name} />
                      <div className="order-row-main">
                        <div>
                          <strong>{item.name}</strong>
                          <p>
                            {item.quantity} x {formatMoney(item.price)}
                          </p>
                        </div>
                      </div>
                      <strong className="row-price">
                        {formatMoney(item.quantity * item.price)}
                      </strong>
                    </article>
                  ))}
                </div>
              </section>

              <aside className="payment-panel">
                <div className="payment-card">
                  <p className="sidebar-title">Payment summary</p>
                  <div className="amount-row">
                    <span>Subtotal</span>
                    <strong>{formatMoney(paymentSubtotal)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Tax</span>
                    <strong>{formatMoney(paymentTax)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Service</span>
                    <strong>{formatMoney(paymentService)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Member benefit</span>
                    <strong>-{formatMoney(paymentMemberDiscount)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Promotion benefit</span>
                    <strong>-{formatMoney(paymentPromotionDiscount)}</strong>
                  </div>
                  <div className="amount-row total">
                    <span>Total due</span>
                    <strong>{formatMoney(paymentTotal)}</strong>
                  </div>
                </div>

                <div className="payment-card">
                  <p className="sidebar-title">Payment methods</p>
                  <div className="method-grid">
                    <button className="method-button active">Tap to Pay</button>
                    <button className="method-button">Card</button>
                    <button className="method-button">Cash</button>
                    <button className="method-button">Split Bill</button>
                  </div>
                </div>

                <div className="payment-card accent-card">
                  <p className="sidebar-title">Ready to collect</p>
                  <h3>{formatMoney(paymentTotal)}</h3>
                  <p className="accent-copy">
                    Complete cashier settlement to close the active table order and release the
                    table back into service.
                  </p>
                  <button
                    className="cta-button"
                    disabled={!canCollectPayment(selectedOrderStage)}
                    onClick={() => void completeSettlement()}
                  >
                    Collect payment
                  </button>
                  {!canCollectPayment(selectedOrderStage) ? (
                    <p className="payment-guard-copy">
                      Move this order to <strong>Payment Pending</strong> before collecting payment.
                    </p>
                  ) : null}
                </div>
              </aside>
            </div>
          )}

          {view === "success" && (
            <div className="review-layout">
              <section className="payment-summary success-layout">
                <div className="success-orb">✓</div>
                <div className="current-table-badge">
                  <span className="current-table-label">Current table</span>
                  <strong>T{selectedTable.id}</strong>
                </div>
                <h2>Payment completed</h2>
                <p className="success-copy">
                  The charge was approved, the receipt is ready, and the table can now be marked
                  for turnover or reopened for a new party.
                </p>

                <div className="success-metrics">
                  <article className="status-card tone-mint">
                    <span>Amount paid</span>
                    <strong>{formatMoney(paymentTotal)}</strong>
                  </article>
                  <article className="status-card tone-sky">
                    <span>Method</span>
                    <strong>Tap to Pay</strong>
                  </article>
                  <article className="status-card tone-rose">
                    <span>Receipt</span>
                    <strong>Sent & printed</strong>
                  </article>
                </div>
              </section>

              <aside className="review-panel">
                <div className="payment-card">
                  <p className="sidebar-title">After payment</p>
                  <button className="sort-row">
                    <span className="sort-icon" />
                    <span>Print extra receipt</span>
                  </button>
                  <button className="sort-row">
                    <span className="sort-icon" />
                    <span>Mark table as cleaning</span>
                  </button>
                  <button className="sort-row" onClick={() => setView("tables")}>
                    <span className="sort-icon" />
                    <span>Return to floor panel</span>
                  </button>
                </div>

                <div className="payment-card accent-card">
                  <p className="sidebar-title">Next action</p>
                  <h3>Ready for turnover</h3>
                  <p className="accent-copy">
                    Staff can now clear Table {selectedTable.id} and release it back into the
                    available pool for the next reservation or walk-in.
                  </p>
                  <button className="cta-button" onClick={() => setView("tables")}>
                    Payment Done
                  </button>
                </div>
              </aside>
            </div>
          )}
          </div>

          <div className="persistent-nav persistent-nav-bottom">
            {[
              ["reservations", "Reservations"],
              ["transfer", "Transfer Table"],
              ["split", "Split Bill"]
            ].map(([value, label]) => (
              <button
                key={value}
                className={`persistent-nav-button persistent-nav-button-secondary ${
                  view === value ? "persistent-nav-button-active" : ""
                }`}
                onClick={() => setView(value as View)}
              >
                {label}
              </button>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}

export default App;
