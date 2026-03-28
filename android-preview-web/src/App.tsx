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

type AttributeValue = { code?: string; label: string; priceDeltaCents: number; defaultSelected: boolean; kitchenLabel?: string };
type AttributeGroup = {
  code?: string;
  name: string;
  selectionMode: "SINGLE" | "MULTIPLE";
  required: boolean;
  minSelect?: number;
  maxSelect?: number;
  values: AttributeValue[];
};
type ModifierOption = { code?: string; label: string; priceDeltaCents: number; defaultSelected: boolean; kitchenLabel?: string };
type ModifierGroup = {
  code?: string;
  name: string;
  freeQuantity?: number;
  minSelect?: number;
  maxSelect?: number;
  options: ModifierOption[];
};
type ComboSlot = { code?: string; name: string; minSelect?: number; maxSelect?: number; allowedSkuCodes: string[] };

type MenuItem = {
  id: number;
  skuId: number;
  skuCode: string;
  name: string;
  category: string;
  price: number;
  image: string;
  status?: "ACTIVE" | "INACTIVE";
  memberPrice?: number;
  attributeGroups?: AttributeGroup[];
  modifierGroups?: ModifierGroup[];
  comboSlots?: ComboSlot[];
};

type OrderItem = MenuItem & {
  cartLineId?: string;
  selectionKey?: string;
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
  status: "checked-in" | "waiting" | "upcoming" | "no-show" | "cancelled";
  area: string;
  tableId?: number | null;
};

type ReservationDraft = {
  id: number | null;
  name: string;
  time: string;
  partySize: number;
  status: Reservation["status"];
  area: string;
};

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

type ReservationResponse = {
  reservationId: number;
  reservationNo: string;
  storeId: number;
  tableId: number | null;
  guestName: string;
  reservationTime: string;
  partySize: number;
  reservationStatus: string;
  area: string;
};

type TableTransferResultResponse = {
  sourceTableId: number;
  destinationTableId: number;
  sessionId: string | null;
  tableStatus: string;
};

type CompletedSettlementSnapshot = {
  tableId: number;
  amount: number;
  methodLabel: string;
};

type PromotionRule = {
  id: string;
  label: string;
  threshold: number;
  discount: number;
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
  submittedOrders: SubmittedOrderResponse[];
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
    note?: string;
  }>;
};

const categories: Category[] = [
  { id: "meals", name: "Rice & Meals", items: 3, color: "amber" },
  { id: "snacks", name: "Hot Snacks", items: 2, color: "coral" },
  { id: "drinks", name: "Drinks", items: 2, color: "blue" },
  { id: "desserts", name: "Desserts", items: 1, color: "pink" },
  { id: "popular", name: "Chef Specials", items: 1, color: "green" }
];

const tableAreas = [
  "Window",
  "Main hall",
  "Main hall",
  "Patio",
  "Private booth",
  "Window",
  "Chef counter",
  "Main hall",
  "Main hall",
  "Patio",
  "Window",
  "Patio",
  "Main hall",
  "Chef counter",
  "Main hall",
  "Private booth",
  "Window",
  "Window",
  "Patio",
  "Main hall",
  "Main hall",
  "Private booth",
  "Chef counter",
  "Indoor"
];

const tableAccents = [
  "sky",
  "peach",
  "mint",
  "lilac",
  "gold",
  "mint",
  "rose",
  "lilac",
  "sky",
  "mint",
  "peach",
  "mint",
  "gold",
  "lilac",
  "mint",
  "rose",
  "mint",
  "lilac",
  "peach",
  "mint",
  "sky",
  "lilac",
  "rose",
  "gold"
];

const tableSeatCapacities = [4, 2, 6, 4, 2, 6, 2, 4, 6, 2, 4, 2, 6, 4, 2, 6, 4, 6, 2, 4, 2, 6, 4, 2];

function getTableCapacity(tableId: number) {
  return tableSeatCapacities[tableId - 1] ?? 4;
}

const tables: TableCard[] = Array.from({ length: 24 }, (_, index) => ({
  id: index + 1,
  status: "available",
  guests: 0,
  area: tableAreas[index] ?? "Main hall",
  total: 0,
  course: "Ready",
  accent: tableAccents[index] ?? "mint"
}));

const initialReservations: Reservation[] = [
  { id: 1, name: "Olivia Chen", time: "18:45", partySize: 2, status: "checked-in", area: "Window", tableId: 11 },
  { id: 2, name: "Luca Martin", time: "19:00", partySize: 4, status: "waiting", area: "Main hall", tableId: null },
  { id: 3, name: "Mia Rodriguez", time: "19:15", partySize: 6, status: "upcoming", area: "Private booth", tableId: null },
  { id: 4, name: "Noah Patel", time: "19:30", partySize: 3, status: "waiting", area: "Patio", tableId: null }
];

const menu: MenuItem[] = [
  {
    id: 1,
    skuId: 401,
    skuCode: "fried-rice-default",
    name: "Signature Fried Rice",
    category: "meals",
    price: 18,
    memberPrice: 16,
    image:
      "https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=900&q=80",
    attributeGroups: [
      {
        name: "Portion",
        selectionMode: "SINGLE",
        required: true,
        values: [
          { label: "Regular", priceDeltaCents: 0, defaultSelected: true },
          { label: "Large", priceDeltaCents: 400, defaultSelected: false }
        ]
      },
      {
        name: "Spice Level",
        selectionMode: "SINGLE",
        required: false,
        values: [
          { label: "No Spice", priceDeltaCents: 0, defaultSelected: true },
          { label: "Mild", priceDeltaCents: 0, defaultSelected: false },
          { label: "Medium", priceDeltaCents: 0, defaultSelected: false }
        ]
      }
    ],
    modifierGroups: [
      {
        name: "Add-ons",
        freeQuantity: 0,
        options: [
          { label: "Add Egg", priceDeltaCents: 150, defaultSelected: false },
          { label: "Add Chicken Cutlet", priceDeltaCents: 500, defaultSelected: false }
        ]
      }
    ],
    comboSlots: []
  },
  {
    id: 2,
    skuId: 402,
    skuCode: "black-pepper-beef-rice-default",
    name: "Black Pepper Beef Rice",
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
    name: "Crispy Chicken Bites",
    category: "snacks",
    price: 16,
    image:
      "https://images.unsplash.com/photo-1562967916-eb82221dfb92?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 4,
    skuId: 404,
    skuCode: "white-peach-soda-default",
    name: "White Peach Soda",
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
    name: "Brown Sugar Milk Tea",
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
    name: "Mango Pudding",
    category: "desserts",
    price: 15,
    image:
      "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 7,
    skuId: 407,
    skuCode: "chef-combo-default",
    name: "Chef Combo",
    category: "popular",
    price: 46,
    memberPrice: 42,
    image:
      "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=900&q=80",
    attributeGroups: [],
    modifierGroups: [],
    comboSlots: [
      {
        name: "Drink",
        minSelect: 1,
        maxSelect: 1,
        allowedSkuCodes: ["white-peach-soda-default", "brown-sugar-milk-tea-default"]
      }
    ]
  },
  {
    id: 8,
    skuId: 408,
    skuCode: "truffle-fries-default",
    name: "Truffle Fries",
    category: "snacks",
    price: 19,
    image:
      "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=900&q=80"
  }
];

const initialDraftOrders: Record<number, OrderItem[]> = {};

const DRAFT_ORDERS_STORAGE_KEY = "pos-preview-table-draft-orders-v2";
const ORDER_STAGES_STORAGE_KEY = "pos-preview-table-order-stages-v2";
const STORE_ID = 101;
const QR_ORDERING_BASE_URL = import.meta.env.VITE_QR_ORDERING_URL_BASE ?? "http://localhost:4183";

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
  return new Intl.NumberFormat("en-SG", {
    style: "currency",
    currency: "SGD",
    currencyDisplay: "code"
  }).format(value);
}

function hasCustomizations(item: MenuItem | OrderItem) {
  return (item.attributeGroups?.length ?? 0) > 0 || (item.modifierGroups?.length ?? 0) > 0 || (item.comboSlots?.length ?? 0) > 0;
}

function buildDefaultAttributeSelections(item: MenuItem) {
  return Object.fromEntries(
    (item.attributeGroups ?? []).map((group, index) => [
      index,
      group.values.filter((value) => value.defaultSelected).map((value) => value.label)
    ])
  ) as Record<number, string[]>;
}

function buildDefaultModifierSelections(item: MenuItem) {
  return Object.fromEntries(
    (item.modifierGroups ?? []).map((group, index) => [
      index,
      group.options.filter((option) => option.defaultSelected).map((option) => option.label)
    ])
  ) as Record<number, string[]>;
}

function buildSelectionSummary(
  item: MenuItem,
  attributeSelections: Record<number, string[]>,
  modifierSelections: Record<number, string[]>,
  comboSelections: Record<number, string[]>
) {
  const parts: string[] = [];
  (item.attributeGroups ?? []).forEach((group, index) => {
    const selected = attributeSelections[index] ?? [];
    if (selected.length > 0) {
      parts.push(`${group.name}:${selected.join("/")}`);
    }
  });
  (item.modifierGroups ?? []).forEach((group, index) => {
    const selected = modifierSelections[index] ?? [];
    if (selected.length > 0) {
      parts.push(`${group.name}:${selected.join("/")}`);
    }
  });
  (item.comboSlots ?? []).forEach((slot, index) => {
    const selected = comboSelections[index] ?? [];
    if (selected.length > 0) {
      parts.push(`${slot.name}:${selected.join("/")}`);
    }
  });
  return parts.join("; ");
}

function calculateCustomizationPrice(
  item: MenuItem,
  attributeSelections: Record<number, string[]>,
  modifierSelections: Record<number, string[]>
) {
  let deltaCents = 0;
  (item.attributeGroups ?? []).forEach((group, index) => {
    const selected = new Set(attributeSelections[index] ?? []);
    group.values.forEach((value) => {
      if (selected.has(value.label)) {
        deltaCents += value.priceDeltaCents;
      }
    });
  });
  (item.modifierGroups ?? []).forEach((group, index) => {
    const selected = modifierSelections[index] ?? [];
    const freeQuantity = group.freeQuantity ?? 0;
    group.options.forEach((option) => {
      const occurrences = selected.filter((value) => value === option.label).length;
      deltaCents += Math.max(0, occurrences - freeQuantity) * option.priceDeltaCents;
    });
  });
  return deltaCents / 100;
}

function createCartLineId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

function getDraftOrderTotal(items: OrderItem[]) {
  return Number(items.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2));
}

function createReservationDraft(entry?: Reservation | null): ReservationDraft {
  return {
    id: entry?.id ?? null,
    name: entry?.name ?? "",
    time: entry?.time ?? "19:00",
    partySize: entry?.partySize ?? 2,
    status: entry?.status ?? "upcoming",
    area: entry?.area ?? "Main hall"
  };
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
    memberPriceCents: entry.memberPrice ? Math.round(entry.memberPrice * 100) : null,
    note: entry.note ?? ""
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
        note: item.remark ?? "Sent to kitchen"
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

function toReservationStatus(status: string): Reservation["status"] {
  switch (status.toUpperCase()) {
    case "CHECKED_IN":
      return "checked-in";
    case "WAITING":
      return "waiting";
    case "NO_SHOW":
      return "no-show";
    case "CANCELLED":
      return "cancelled";
    case "UPCOMING":
    default:
      return "upcoming";
  }
}

function toReservationApiStatus(status: Reservation["status"]) {
  switch (status) {
    case "checked-in":
      return "CHECKED_IN";
    case "waiting":
      return "WAITING";
    case "no-show":
      return "NO_SHOW";
    case "cancelled":
      return "CANCELLED";
    case "upcoming":
    default:
      return "UPCOMING";
  }
}

function formatReservationStatus(status: Reservation["status"]) {
  switch (status) {
    case "checked-in":
      return "Checked in";
    case "waiting":
      return "Waiting";
    case "upcoming":
      return "Upcoming";
    case "no-show":
      return "No-show";
    case "cancelled":
      return "Cancelled";
    default:
      return status;
  }
}

function mapReservationFromApi(
  reservation: ReservationResponse,
  previewTableIdsByBackendTableId: Record<number, number>
): Reservation {
  return {
    id: reservation.reservationId,
    name: reservation.guestName,
    time: reservation.reservationTime,
    partySize: reservation.partySize,
    status: toReservationStatus(reservation.reservationStatus),
    area: reservation.area,
    tableId:
      reservation.tableId !== null
        ? (previewTableIdsByBackendTableId[reservation.tableId] ?? null)
        : null
  };
}

async function fetchReservations(
  storeId: number,
  previewTableIdsByBackendTableId: Record<number, number>
) {
  const response = await fetch(`/api/v2/stores/${storeId}/reservations`);

  if (!response.ok) {
    return null;
  }

  const payload = (await response.json()) as ApiResponse<ReservationResponse[]>;
  return payload.code === 0
    ? payload.data.map((entry) => mapReservationFromApi(entry, previewTableIdsByBackendTableId))
    : null;
}

async function createReservationRequest(
  storeId: number,
  draft: ReservationDraft,
  previewTableIdsByBackendTableId: Record<number, number>
) {
  const response = await fetch(`/api/v2/stores/${storeId}/reservations`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      guestName: draft.name.trim(),
      reservationTime: draft.time,
      partySize: draft.partySize,
      reservationStatus: toReservationApiStatus(draft.status),
      area: draft.area
    })
  });

  if (!response.ok) {
    throw new Error("Failed to create reservation");
  }

  const payload = (await response.json()) as ApiResponse<ReservationResponse>;
  if (payload.code !== 0 || !payload.data) {
    throw new Error(payload.message || "Failed to create reservation");
  }

  return mapReservationFromApi(payload.data, previewTableIdsByBackendTableId);
}

async function updateReservationRequest(
  storeId: number,
  draft: ReservationDraft,
  previewTableIdsByBackendTableId: Record<number, number>
) {
  const response = await fetch(`/api/v2/stores/${storeId}/reservations/${draft.id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      guestName: draft.name.trim(),
      reservationTime: draft.time,
      partySize: draft.partySize,
      reservationStatus: toReservationApiStatus(draft.status),
      area: draft.area
    })
  });

  if (!response.ok) {
    throw new Error("Failed to update reservation");
  }

  const payload = (await response.json()) as ApiResponse<ReservationResponse>;
  if (payload.code !== 0 || !payload.data) {
    throw new Error(payload.message || "Failed to update reservation");
  }

  return mapReservationFromApi(payload.data, previewTableIdsByBackendTableId);
}

async function seatReservationRequest(
  storeId: number,
  reservationId: number,
  tableId: number | null,
  previewTableIdsByBackendTableId: Record<number, number>
) {
  const response = await fetch(`/api/v2/stores/${storeId}/reservations/${reservationId}/seat`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(tableId ? { tableId } : {})
  });

  if (!response.ok) {
    throw new Error("Failed to seat reservation");
  }

  const payload = (await response.json()) as ApiResponse<ReservationResponse>;
  if (payload.code !== 0 || !payload.data) {
    throw new Error(payload.message || "Failed to seat reservation");
  }

  return mapReservationFromApi(payload.data, previewTableIdsByBackendTableId);
}

async function transferTableRequest(storeId: number, sourceTableId: number, destinationTableId: number) {
  const response = await fetch(`/api/v2/stores/${storeId}/tables/${sourceTableId}/transfer`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      destinationTableId
    })
  });

  if (!response.ok) {
    throw new Error("Failed to transfer table");
  }

  const payload = (await response.json()) as ApiResponse<TableTransferResultResponse>;
  if (payload.code !== 0 || !payload.data) {
    throw new Error(payload.message || "Failed to transfer table");
  }

  return payload.data;
}

function App() {
  type PaymentCollectionMethod = "credit-card" | "alipay" | "wechat" | "paynow";
  type PaymentAssistTool = "member" | "promotion" | "discount" | null;
  type CashierDiscountMode = "preset" | "percent" | "amount";

  const [view, setView] = useState<View>("tables");
  const [activeCategory, setActiveCategory] = useState("all");
  const [search, setSearch] = useState("");
  const [tableState, setTableState] = useState<TableCard[]>(tables);
  const [reservationState, setReservationState] = useState<Reservation[]>(initialReservations);
  const [reservationDraft, setReservationDraft] = useState<ReservationDraft>(createReservationDraft());
  const [activeReservationId, setActiveReservationId] = useState<number | null>(initialReservations[0]?.id ?? null);
  const [reservationSearch, setReservationSearch] = useState("");
  const [reservationStatusFilter, setReservationStatusFilter] = useState<"all" | Reservation["status"]>("all");
  const [reservationSeatTableId, setReservationSeatTableId] = useState<number | null>(null);
  const [selectedTableId, setSelectedTableId] = useState<number>(6);
  const [targetTableId, setTargetTableId] = useState<number>(1);
  const [tableDraftOrders, setTableDraftOrders] = useState<Record<number, OrderItem[]>>(loadStoredDraftOrders);
  const [submittedOrdersByTable, setSubmittedOrdersByTable] = useState<Record<number, SubmittedOrderResponse[]>>({});
  const [tablePaymentPreviews, setTablePaymentPreviews] = useState<Record<number, SettlementPreviewResponse | null>>({});
  const [tableOrderStages, setTableOrderStages] = useState<Record<number, OrderStage>>(loadStoredOrderStages);
  const [tableBackendIds, setTableBackendIds] = useState<Record<number, number>>({});
  const [qrOrders, setQrOrders] = useState<Record<string, QrCurrentOrderResponse>>({});
  const [recentQrAlert, setRecentQrAlert] = useState<QrCurrentOrderResponse | null>(null);
  const [isPreparingPayment, setIsPreparingPayment] = useState(false);
  const [selectedCollectionMethod, setSelectedCollectionMethod] = useState<PaymentCollectionMethod>("credit-card");
  const [activePaymentAssistTool, setActivePaymentAssistTool] = useState<PaymentAssistTool>(null);
  const [selectedPromotionRuleId, setSelectedPromotionRuleId] = useState("none");
  const [cashierDiscountMode, setCashierDiscountMode] = useState<CashierDiscountMode>("preset");
  const [cashierDiscountRate, setCashierDiscountRate] = useState<0 | 0.05 | 0.1 | 0.2>(0);
  const [cashierDiscountPercentInput, setCashierDiscountPercentInput] = useState("");
  const [cashierDiscountAmountInput, setCashierDiscountAmountInput] = useState("");
  const [completedSettlementSnapshot, setCompletedSettlementSnapshot] = useState<CompletedSettlementSnapshot | null>(null);
  const [configuringItem, setConfiguringItem] = useState<MenuItem | null>(null);
  const [attributeSelections, setAttributeSelections] = useState<Record<number, string[]>>({});
  const [modifierSelections, setModifierSelections] = useState<Record<number, string[]>>({});
  const [comboSelections, setComboSelections] = useState<Record<number, string[]>>({});
  const seenQrOrderNos = useRef<Set<string>>(new Set());
  const pendingQrWrites = useRef<Set<string>>(new Set());
  const draftOrdersRef = useRef<Record<number, OrderItem[]>>(initialDraftOrders);
  const reservationStateRef = useRef<Reservation[]>(initialReservations);
  const hasLoadedReservationsRef = useRef(false);

  const selectedTable = tableState.find((table) => table.id === selectedTableId) ?? tableState[0];
  const targetTable = tableState.find((table) => table.id === targetTableId) ?? tableState[0];
  const activeReservation = reservationState.find((entry) => entry.id === activeReservationId) ?? null;
  const selectedTableCode = `T${selectedTable.id}`;
  const selectedBackendOrder = qrOrders[selectedTableCode];
  const selectedBackendTableId = tableBackendIds[selectedTable.id] ?? selectedBackendOrder?.tableId ?? selectedTable.id;
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
  const publishedMenu = useMemo(
    () => menu.filter((item) => item.status !== "INACTIVE"),
    []
  );
  const categoryCards = useMemo(
    () =>
      categories
        .map((category) => ({
          ...category,
          items: publishedMenu.filter((item) => item.category === category.id).length
        }))
        .filter((category) => category.items > 0),
    [publishedMenu]
  );
  const filteredReservations = useMemo(() => {
    const keyword = reservationSearch.trim().toLowerCase();
    return reservationState.filter((entry) => {
      const matchesStatus = reservationStatusFilter === "all" || entry.status === reservationStatusFilter;
      const matchesKeyword =
        keyword === "" ||
        `${entry.name} ${entry.area} ${entry.time} T${entry.tableId ?? ""}`.toLowerCase().includes(keyword);
      return matchesStatus && matchesKeyword;
    });
  }, [reservationSearch, reservationState, reservationStatusFilter]);
  const previewTableIdsByBackendTableId = useMemo(
    () =>
      Object.fromEntries(
        Object.entries(tableBackendIds).map(([previewTableId, backendTableId]) => [
          backendTableId,
          Number(previewTableId)
        ])
      ) as Record<number, number>,
    [tableBackendIds]
  );

  const visibleMenu = useMemo(() => {
    return publishedMenu.filter((item) => {
      const matchCategory =
        activeCategory === "all" ||
        item.category.toLowerCase() === activeCategory ||
        item.category.toLowerCase().includes(activeCategory);
      const matchSearch =
        search.trim() === "" ||
        `${item.name} ${item.category}`.toLowerCase().includes(search.toLowerCase());

      return matchCategory && matchSearch;
    });
  }, [activeCategory, publishedMenu, search]);
  const configuringPrice = useMemo(
    () =>
      configuringItem
        ? configuringItem.price + calculateCustomizationPrice(configuringItem, attributeSelections, modifierSelections)
        : 0,
    [attributeSelections, configuringItem, modifierSelections]
  );

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
        note: item.remark ?? "Sent to kitchen"
      } satisfies OrderItem;
    })
  );
  const hasSubmittedRounds = selectedSubmittedOrders.length > 0;
  const subtotal = draftOrderItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const submittedSubtotal = submittedOrderItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const tax = subtotal * 0.09;
  const service = subtotal * 0.1;
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
        const contextByTableCode = Object.fromEntries(
          contexts
            .filter((entry): entry is TableContextResponse => Boolean(entry))
            .map((entry) => [entry.tableCode, entry])
        );
        setTableBackendIds(
          Object.fromEntries(
            contexts
              .filter((entry): entry is TableContextResponse => Boolean(entry))
              .map((entry) => [Number(entry.tableCode.replace("T", "")), entry.tableId])
          )
        );
        setSubmittedOrdersByTable((current) => ({
          ...current,
          ...Object.fromEntries(
            contexts
              .filter((entry): entry is TableContextResponse => Boolean(entry))
              .map((entry) => [Number(entry.tableCode.replace("T", "")), entry.submittedOrders ?? []])
          )
        }));

        const newIncomingOrder = Object.values(nextQrOrders).find(
          (entry) => entry.orderSource === "QR" && !seenQrOrderNos.current.has(entry.orderNo)
        );
        if (newIncomingOrder) {
          setRecentQrAlert(newIncomingOrder);
        }

        Object.values(nextQrOrders).forEach((entry) => {
          seenQrOrderNos.current.add(entry.orderNo);
        });

        setTableState((current) =>
          current.map((table) => {
            const context = contextByTableCode[`T${table.id}`];
            const match = nextQrOrders[`T${table.id}`];
            const backendSubmittedOrders = context?.submittedOrders ?? [];
            const localDraftItems = draftOrdersRef.current[table.id] ?? [];
            const linkedReservation = reservationStateRef.current.find(
              (entry) => entry.status === "checked-in" && entry.tableId === table.id
            );
            if (!match) {
              if (backendSubmittedOrders.length > 0) {
                const submittedTotal = backendSubmittedOrders.reduce(
                  (sum: number, order: SubmittedOrderResponse) => sum + order.pricing.payableAmountCents / 100,
                  0
                );
                const backendSource = backendSubmittedOrders.some((order) => order.sourceOrderType === "QR") ? "QR" : "POS";
                const isPaymentPending = context?.tableStatus === "PENDING_SETTLEMENT";
                return {
                  ...table,
                  status: "occupied",
                  source: backendSource,
                  settlementState: isPaymentPending ? "PENDING_SETTLEMENT" : "SUBMITTED",
                  memberName: undefined,
                  total: Number(submittedTotal.toFixed(2)),
                  course: isPaymentPending
                    ? `${backendSubmittedOrders.reduce((sum: number, order: SubmittedOrderResponse) => sum + order.items.length, 0)} items payment pending`
                    : `${backendSubmittedOrders.length} rounds sent to kitchen`
                };
              }

              if (context?.tableStatus === "RESERVED") {
                return {
                  ...table,
                  status: "reserved",
                  source: undefined,
                  settlementState: undefined,
                  memberName: linkedReservation?.name,
                  total: 0,
                  guests: linkedReservation?.partySize ?? table.guests,
                  course: linkedReservation ? `Reserved for ${linkedReservation.name}` : "Reserved"
                };
              }

              if (localDraftItems.length > 0) {
                const draftTotal = localDraftItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
                return {
                  ...table,
                  status: "occupied",
                  source: "POS",
                  settlementState: "DRAFT",
                  memberName: undefined,
                  total: Number(draftTotal.toFixed(2)),
                  course: "Draft order in progress"
                };
              }

              return {
                ...tables.find((baseTable) => baseTable.id === table.id)!,
                source: undefined,
                settlementState: undefined,
                memberName: undefined
              };
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
            contexts
              .filter((entry): entry is TableContextResponse => Boolean(entry))
              .map((entry) => {
                const tableId = Number(entry.tableCode.replace("T", ""));
                const localDraftItems = draftOrdersRef.current[tableId] ?? [];
                const currentOrder = nextQrOrders[entry.tableCode];

                const stage: OrderStage =
                  currentOrder?.settlementStatus === "PENDING_SETTLEMENT" || entry.tableStatus === "PENDING_SETTLEMENT"
                    ? "PENDING_SETTLEMENT"
                    : currentOrder?.settlementStatus === "SETTLED"
                      ? "SETTLED"
                      : (entry.submittedOrders?.length ?? 0) > 0
                        ? "SUBMITTED"
                        : localDraftItems.length > 0 || currentOrder?.settlementStatus === "DRAFT"
                          ? "DRAFT"
                          : "DRAFT";

                return [tableId, stage];
              })
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
          note: item.note ?? "Draft",
          selectionKey: `${item.skuId}::${item.note ?? ""}`,
          cartLineId: `${item.skuId}-${index}`
        } satisfies OrderItem;
      })
    : [];
  const shouldUseBackendDraftItems =
    Boolean(selectedPosDraftOrder) &&
    selectedSubmittedOrders.length === 0 &&
    selectedOrderStage === "DRAFT";
  const displayedOrderItems = shouldUseBackendDraftItems ? backendDraftItems : draftOrderItems;
  const effectiveDraftOrderItems = displayedOrderItems;
  const canMoveToSettlement =
    effectiveDraftOrderItems.length === 0 &&
    (hasSubmittedRounds || selectedOrderStage === "SUBMITTED") &&
    selectedOrderStage !== "PENDING_SETTLEMENT" &&
    selectedOrderStage !== "SETTLED";
  const promotionRules: PromotionRule[] = [
    { id: "none", label: "No Promotion", threshold: 0, discount: 0 },
    { id: "promo-50-5", label: "Spend SGD 50 Save SGD 5", threshold: 50, discount: 5 },
    { id: "promo-100-12", label: "Spend SGD 100 Save SGD 12", threshold: 100, discount: 12 },
    { id: "promo-180-25", label: "Spend SGD 180 Save SGD 25", threshold: 180, discount: 25 }
  ];
  const selectedPromotionRule =
    promotionRules.find((rule) => rule.id === selectedPromotionRuleId) ?? promotionRules[0];

  const paymentOrderItems = submittedOrderItems;

  const displayedSubtotal = selectedPosDraftOrder
    ? (selectedPosDraftOrder.originalAmountCents ?? selectedPosDraftOrder.payableAmountCents) / 100
    : subtotal;
  const displayedMemberDiscount = selectedPosDraftOrder
    ? (selectedPosDraftOrder.memberDiscountCents ?? 0) / 100
    : memberDiscount;
  const cashierDiscountPercentValue = Math.max(0, Number.parseFloat(cashierDiscountPercentInput) || 0);
  const cashierDiscountAmountValue = Math.max(0, Number.parseFloat(cashierDiscountAmountInput) || 0);
  const displayedCashierDiscount = Math.min(
    cashierDiscountMode === "amount"
      ? cashierDiscountAmountValue
      : displayedSubtotal * (cashierDiscountMode === "percent" ? cashierDiscountPercentValue / 100 : cashierDiscountRate),
    displayedSubtotal
  );
  const displayedPromotionDiscount = Math.min(
    displayedSubtotal >= selectedPromotionRule.threshold ? selectedPromotionRule.discount : 0,
    displayedSubtotal
  );
  const displayedDiscountedSubtotal = Math.max(
    displayedSubtotal - displayedMemberDiscount - displayedPromotionDiscount - displayedCashierDiscount,
    0
  );
  const displayedTax = displayedDiscountedSubtotal * 0.09;
  const displayedService = displayedSubtotal * 0.1;
  const displayedTotal = displayedDiscountedSubtotal + displayedTax + displayedService;
  const paymentSubtotal = selectedTablePaymentPreview
    ? selectedTablePaymentPreview.pricing.originalAmountCents / 100
    : selectedSubmittedOrders.reduce((sum, order) => sum + order.pricing.originalAmountCents / 100, 0);
  const paymentMemberDiscount = selectedTablePaymentPreview
    ? selectedTablePaymentPreview.pricing.memberDiscountCents / 100
    : selectedSubmittedOrders.reduce((sum, order) => sum + order.pricing.memberDiscountCents / 100, 0);
  const paymentCashierDiscount = Math.min(
    cashierDiscountMode === "amount"
      ? cashierDiscountAmountValue
      : paymentSubtotal * (cashierDiscountMode === "percent" ? cashierDiscountPercentValue / 100 : cashierDiscountRate),
    paymentSubtotal
  );
  const paymentPromotionDiscount = Math.min(
    paymentSubtotal >= selectedPromotionRule.threshold ? selectedPromotionRule.discount : 0,
    paymentSubtotal
  );
  const paymentDiscountedSubtotal = Math.max(
    paymentSubtotal - paymentMemberDiscount - paymentPromotionDiscount - paymentCashierDiscount,
    0
  );
  const paymentTax = paymentDiscountedSubtotal * 0.09;
  const paymentService = paymentSubtotal * 0.1;
  const paymentTotal = paymentDiscountedSubtotal + paymentTax + paymentService;
  const perGuest = paymentTotal / splitGuests;
  const submittedRoundsTotal = selectedSubmittedOrders.reduce(
    (sum, order) => sum + order.pricing.payableAmountCents / 100,
    0
  );
  const selectedCollectionMethodLabel =
    {
      "credit-card": "Credit Card",
      alipay: "Alipay",
      wechat: "WeChat Pay",
      paynow: "PayNow"
    }[selectedCollectionMethod];
  const paymentMethods: Array<{ id: PaymentCollectionMethod; label: string; accent: string }> = [
    { id: "credit-card", label: "Credit Card", accent: "Card reader" },
    { id: "alipay", label: "Alipay", accent: "Wallet" },
    { id: "wechat", label: "WeChat Pay", accent: "Wallet" },
    { id: "paynow", label: "PayNow", accent: "Bank transfer" }
  ];
  const cashierDiscountOptions: Array<{ label: string; rate: 0 | 0.05 | 0.1 | 0.2 }> = [
    { label: "No Discount", rate: 0 },
    { label: "5% Off", rate: 0.05 },
    { label: "10% Off", rate: 0.1 },
    { label: "20% Off", rate: 0.2 }
  ];
  const promotionRuleLabel =
    selectedPromotionRule.id === "none"
      ? "Apply spend-threshold promotion"
      : displayedSubtotal >= selectedPromotionRule.threshold
        ? `${formatMoney(displayedPromotionDiscount)} applied`
        : `Need ${formatMoney(Math.max(selectedPromotionRule.threshold - displayedSubtotal, 0))} more`;
  const cashierDiscountLabel =
    cashierDiscountMode === "amount"
      ? `${formatMoney(paymentCashierDiscount)} applied`
      : cashierDiscountMode === "percent"
        ? `${cashierDiscountPercentValue || 0}% applied`
        : cashierDiscountRate > 0
          ? `${Math.round(cashierDiscountRate * 100)}% applied`
          : "Apply preset, custom %, or fixed amount";
  const successAmount = completedSettlementSnapshot?.amount ?? paymentTotal;
  const successMethodLabel = completedSettlementSnapshot?.methodLabel ?? selectedCollectionMethodLabel;
  const successTableId = completedSettlementSnapshot?.tableId ?? selectedTable.id;

  useEffect(() => {
    draftOrdersRef.current = tableDraftOrders;
  }, [tableDraftOrders]);

  useEffect(() => {
    reservationStateRef.current = reservationState;
  }, [reservationState]);

  useEffect(() => {
    window.localStorage.setItem(DRAFT_ORDERS_STORAGE_KEY, JSON.stringify(tableDraftOrders));
  }, [tableDraftOrders]);

  useEffect(() => {
    window.localStorage.setItem(ORDER_STAGES_STORAGE_KEY, JSON.stringify(tableOrderStages));
  }, [tableOrderStages]);

  useEffect(() => {
    if (view !== "payment" || isPreparingPayment) {
      return;
    }

    if (canMoveToSettlement) {
      void moveToSettlement();
    }
  }, [
    view,
    isPreparingPayment,
    canMoveToSettlement,
    selectedTable.id,
    selectedTableCode
  ]);

  useEffect(() => {
    if (view !== "payment") {
      return;
    }

    if (
      !(hasSubmittedRounds || selectedOrderStage === "SUBMITTED") ||
      effectiveDraftOrderItems.length > 0 ||
      selectedTablePaymentPreview
    ) {
      return;
    }

    let active = true;

    void (async () => {
      const preview = await fetchTablePaymentPreview(STORE_ID, selectedBackendTableId);
      if (!active || !preview) {
        return;
      }

      setTablePaymentPreviews((current) => ({
        ...current,
        [selectedTable.id]: preview
      }));
    })();

    return () => {
      active = false;
    };
  }, [
    view,
    hasSubmittedRounds,
    effectiveDraftOrderItems.length,
    selectedOrderStage,
    selectedTable.id,
    selectedTablePaymentPreview
  ]);

  useEffect(() => {
    if (view !== "reservations" || activeReservationId === null) {
      return;
    }

    const selectedReservation = reservationState.find((entry) => entry.id === activeReservationId);
    if (selectedReservation) {
      setReservationDraft(createReservationDraft(selectedReservation));
      setReservationSeatTableId(selectedReservation.tableId ?? null);
    }
  }, [activeReservationId, reservationState, view]);

  const refreshReservations = async () => {
    const reservations = await fetchReservations(STORE_ID, previewTableIdsByBackendTableId);
    if (!reservations) {
      return;
    }

    setReservationState(reservations);
    setActiveReservationId((current) =>
      reservations.some((entry) => entry.id === current) ? current : (reservations[0]?.id ?? null)
    );
  };

  useEffect(() => {
    if (hasLoadedReservationsRef.current || Object.keys(previewTableIdsByBackendTableId).length === 0) {
      return;
    }

    hasLoadedReservationsRef.current = true;
    void refreshReservations();
  }, [previewTableIdsByBackendTableId]);

  const syncTableFromV2 = async (tableCode: string, tableId: number) => {
    const refreshedContext = await fetchTableContext(tableCode);
    if (!refreshedContext) {
      return null;
    }
    setTableBackendIds((current) => ({
      ...current,
      [tableId]: refreshedContext.tableId
    }));
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

        const linkedReservation = reservationStateRef.current.find(
          (entry) => entry.status === "checked-in" && entry.tableId === tableId
        );

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

          if (refreshedContext.tableStatus === "RESERVED") {
            return {
              ...table,
              status: "reserved",
              source: undefined,
              settlementState: undefined,
              memberName: linkedReservation?.name,
              total: 0,
              guests: linkedReservation?.partySize ?? table.guests,
              course: linkedReservation ? `Reserved for ${linkedReservation.name}` : "Reserved"
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
          note: item.note ?? source?.category ?? "Add-on",
          selectionKey: `${item.skuId}::${item.note ?? ""}`,
          cartLineId: `${item.skuId}-${index}`
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
              remark: item.note ?? ""
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

  const commitConfiguredItem = (
    item: MenuItem,
    nextAttributeSelections: Record<number, string[]>,
    nextModifierSelections: Record<number, string[]>,
    nextComboSelections: Record<number, string[]>
  ) => {
    const currentItems: OrderItem[] = effectiveDraftOrderItems;
    const summary = buildSelectionSummary(item, nextAttributeSelections, nextModifierSelections, nextComboSelections);
    const nextPrice = item.price + calculateCustomizationPrice(item, nextAttributeSelections, nextModifierSelections);
    const selectionKey = `${item.skuId}::${summary}`;
    const nextItems = currentItems.find((entry) => entry.selectionKey === selectionKey)
      ? currentItems.map((entry) =>
          entry.selectionKey === selectionKey ? { ...entry, quantity: entry.quantity + 1 } : entry
        )
      : [
          ...currentItems,
          {
            ...item,
            price: nextPrice,
            quantity: 1,
            note: summary || "Add-on",
            selectionKey,
            cartLineId: createCartLineId()
          } satisfies OrderItem
        ];

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

  const addItem = (item: MenuItem) => {
    if (hasCustomizations(item)) {
      setConfiguringItem(item);
      setAttributeSelections(buildDefaultAttributeSelections(item));
      setModifierSelections(buildDefaultModifierSelections(item));
      setComboSelections(
        Object.fromEntries((item.comboSlots ?? []).map((_, index) => [index, []])) as Record<number, string[]>
      );
      return;
    }

    commitConfiguredItem(item, {}, {}, {});
  };

  const updateQuantity = (cartLineId: string, delta: number) => {
    const previewNextItems: OrderItem[] = effectiveDraftOrderItems
      .map((entry) =>
        entry.cartLineId === cartLineId ? { ...entry, quantity: Math.max(0, entry.quantity + delta) } : entry
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

  const confirmConfiguredItem = () => {
    if (!configuringItem) {
      return;
    }

    for (const [index, group] of (configuringItem.attributeGroups ?? []).entries()) {
      const selected = attributeSelections[index] ?? [];
      const minSelect = group.required ? Math.max(1, group.minSelect ?? 1) : group.minSelect ?? 0;
      const maxSelect = group.maxSelect ?? (group.selectionMode === "SINGLE" ? 1 : Number.MAX_SAFE_INTEGER);
      if (selected.length < minSelect || selected.length > maxSelect) {
        return;
      }
    }

    for (const [index, group] of (configuringItem.modifierGroups ?? []).entries()) {
      const selected = modifierSelections[index] ?? [];
      const minSelect = group.minSelect ?? 0;
      const maxSelect = group.maxSelect ?? Number.MAX_SAFE_INTEGER;
      if (selected.length < minSelect || selected.length > maxSelect) {
        return;
      }
    }

    for (const [index, slot] of (configuringItem.comboSlots ?? []).entries()) {
      const selected = comboSelections[index] ?? [];
      const minSelect = slot.minSelect ?? 0;
      const maxSelect = slot.maxSelect ?? Number.MAX_SAFE_INTEGER;
      if (selected.length < minSelect || selected.length > maxSelect) {
        return;
      }
    }

    commitConfiguredItem(configuringItem, attributeSelections, modifierSelections, comboSelections);
    setConfiguringItem(null);
  };

  const toggleAttributeValue = (groupIndex: number, valueLabel: string, selectionMode: "SINGLE" | "MULTIPLE") => {
    setAttributeSelections((current) => {
      const selected = current[groupIndex] ?? [];
      if (selectionMode === "SINGLE") {
        return { ...current, [groupIndex]: [valueLabel] };
      }
      return {
        ...current,
        [groupIndex]: selected.includes(valueLabel)
          ? selected.filter((value) => value !== valueLabel)
          : [...selected, valueLabel]
      };
    });
  };

  const toggleModifierOption = (groupIndex: number, optionLabel: string) => {
    setModifierSelections((current) => {
      const selected = current[groupIndex] ?? [];
      return {
        ...current,
        [groupIndex]: selected.includes(optionLabel)
          ? selected.filter((value) => value !== optionLabel)
          : [...selected, optionLabel]
      };
    });
  };

  const toggleComboSelection = (slotIndex: number, skuCode: string) => {
    setComboSelections((current) => {
      const selected = current[slotIndex] ?? [];
      return {
        ...current,
        [slotIndex]: selected.includes(skuCode)
          ? selected.filter((value) => value !== skuCode)
          : [...selected, skuCode]
      };
    });
  };

  const chooseTable = (table: TableCard) => {
    setSelectedTableId(table.id);
    setView("ordering");
  };

  const openReservationDraft = (entry?: Reservation | null) => {
    const nextDraft = createReservationDraft(entry);
    setReservationDraft(nextDraft);
    setActiveReservationId(entry?.id ?? null);
    setReservationSeatTableId(entry?.tableId ?? null);
  };

  const saveReservationDraft = async () => {
    const normalizedName = reservationDraft.name.trim();
    if (!normalizedName) return;

    try {
      const nextReservation =
        reservationDraft.id === null
          ? await createReservationRequest(
              STORE_ID,
              { ...reservationDraft, name: normalizedName },
              previewTableIdsByBackendTableId
            )
          : await updateReservationRequest(
              STORE_ID,
              { ...reservationDraft, name: normalizedName },
              previewTableIdsByBackendTableId
            );

      await refreshReservations();
      setActiveReservationId(nextReservation.id);
      setReservationDraft(createReservationDraft(nextReservation));
    } catch (error) {
      console.error("Failed to save reservation", error);
    }
  };

  const updateReservationStatus = async (
    reservationId: number,
    nextStatus: Reservation["status"]
  ) => {
    const reservation = reservationState.find((entry) => entry.id === reservationId);
    if (!reservation) return;

    try {
      await updateReservationRequest(
        STORE_ID,
        {
          id: reservation.id,
          name: reservation.name,
          time: reservation.time,
          partySize: reservation.partySize,
          status: nextStatus,
          area: reservation.area
        },
        previewTableIdsByBackendTableId
      );
      await refreshReservations();
      if (reservation.tableId) {
        await syncTableFromV2(`T${reservation.tableId}`, reservation.tableId);
      }
      setActiveReservationId(reservationId);
      setReservationDraft(
        createReservationDraft({
          ...reservation,
          status: nextStatus,
          tableId: nextStatus === "checked-in" ? reservation.tableId : null
        })
      );
    } catch (error) {
      console.error("Failed to update reservation status", error);
    }
  };

  const seatReservation = async (reservationId: number, preferredTableId?: number | null) => {
    const reservation = reservationState.find((entry) => entry.id === reservationId);
    if (!reservation) return;

    const matchedTable =
      (preferredTableId
        ? tableState.find((table) => table.id === preferredTableId && table.status === "available")
        : null) ??
      tableState.find((table) => table.status === "available" && table.area === reservation.area) ??
      tableState.find((table) => table.status === "available");

    if (!matchedTable) return;

    try {
      const matchedBackendTableId = tableBackendIds[matchedTable.id] ?? null;
      const seatedReservation = await seatReservationRequest(
        STORE_ID,
        reservationId,
        matchedBackendTableId,
        previewTableIdsByBackendTableId
      );

      await refreshReservations();
      await syncTableFromV2(`T${matchedTable.id}`, matchedTable.id);
      setSelectedTableId(matchedTable.id);
      setActiveReservationId(reservationId);
      setReservationDraft(createReservationDraft(seatedReservation));
      setView("tables");
    } catch (error) {
      console.error("Failed to seat reservation", error);
    }
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

    const submitResponse = await fetch(
      `/api/v2/stores/${storeId}/tables/${tableId}/active-order/${encodeURIComponent(activeOrderId)}/submit-to-kitchen`,
      { method: "POST" }
    );

    if (!submitResponse.ok) {
      return;
    }

    const optimisticSubmittedOrder: SubmittedOrderResponse = {
      submittedOrderId: `optimistic-${activeOrderId}`,
      orderNo: selectedPosDraftOrder?.orderNo ?? `POS-${Date.now()}`,
      sourceOrderType: "POS",
      fulfillmentStatus: "PREPARING",
      settlementStatus: "SUBMITTED",
      memberId: selectedPosDraftOrder?.memberId ?? null,
      pricing: {
        originalAmountCents: Math.round(
          currentDraftItems.reduce((sum, item) => sum + item.price * item.quantity, 0) * 100
        ),
        memberDiscountCents: 0,
        promotionDiscountCents: 0,
        payableAmountCents: Math.round(
          currentDraftItems.reduce((sum, item) => sum + item.price * item.quantity, 0) * 100
        )
      },
      items: currentDraftItems.map((item) => ({
        skuId: item.skuId,
        skuCode: item.skuCode,
        skuName: item.name,
        quantity: item.quantity,
        unitPriceCents: Math.round(item.price * 100),
        remark: item.note ?? null,
        lineTotalCents: Math.round(item.price * item.quantity * 100)
      }))
    };

    setSubmittedOrdersByTable((current) => ({
      ...current,
      [selectedTable.id]: [...(current[selectedTable.id] ?? []), optimisticSubmittedOrder]
    }));
    setTableOrderStages((current) => ({
      ...current,
      [selectedTable.id]: "SUBMITTED"
    }));
    setQrOrders((current) => {
      const existing = current[selectedTableCode];
      if (!existing) {
        return current;
      }

      return {
        ...current,
        [selectedTableCode]: {
          ...existing,
          settlementStatus: "SUBMITTED",
          originalAmountCents: optimisticSubmittedOrder.pricing.originalAmountCents,
          memberDiscountCents: optimisticSubmittedOrder.pricing.memberDiscountCents,
          promotionDiscountCents: optimisticSubmittedOrder.pricing.promotionDiscountCents,
          payableAmountCents: optimisticSubmittedOrder.pricing.payableAmountCents,
          items: []
        }
      };
    });
    setTableState((current) =>
      current.map((table) =>
        table.id === selectedTable.id
          ? {
              ...table,
              status: "occupied",
              source: "POS",
              settlementState: "SUBMITTED",
              total: Number(
                currentDraftItems.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2)
              ),
              course: "Sent to kitchen"
            }
          : table
      )
    );
    setTableDraftOrders((current) => ({
      ...current,
      [selectedTable.id]: []
    }));

    window.setTimeout(() => {
      void syncTableFromV2(selectedTableCode, selectedTable.id);
    }, 900);

    setView("ordering");
  };

  const moveToSettlement = async () => {
    if (!canMoveToSettlement) {
      return;
    }

    setIsPreparingPayment(true);
    try {
      await fetch(
        `/api/v2/stores/${STORE_ID}/tables/${selectedBackendTableId}/payment`,
        { method: "POST" }
      );
      const preview = await fetchTablePaymentPreview(STORE_ID, selectedBackendTableId);
      setTablePaymentPreviews((current) => ({
        ...current,
        [selectedTable.id]: preview
      }));
      await syncTableFromV2(selectedTableCode, selectedTable.id);
      setView("payment");
    } finally {
      setIsPreparingPayment(false);
    }
  };

  const completeSettlement = async () => {
    const settlementSnapshot: CompletedSettlementSnapshot = {
      tableId: selectedTable.id,
      amount: paymentTotal,
      methodLabel: selectedCollectionMethodLabel
    };
    const collectRes = await fetch(`/api/v2/stores/${STORE_ID}/tables/${selectedBackendTableId}/payment/collect`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        cashierId: 9001,
        paymentMethod: "CASH",
        collectedAmountCents: Math.round(paymentTotal * 100)
      })
    });
    if (!collectRes.ok) {
      const errText = await collectRes.text().catch(() => "Unknown error");
      console.error("Settlement failed:", collectRes.status, errText);
      alert(`Settlement failed (${collectRes.status}): ${errText}`);
      return;
    }
    setCompletedSettlementSnapshot(settlementSnapshot);
    setTablePaymentPreviews((current) => ({
      ...current,
      [selectedTable.id]: null
    }));
    await syncTableFromV2(selectedTableCode, selectedTable.id);

    setView("success");
  };

  const applyLocalTransferState = (sourceTableId: number, destinationTableId: number) => {
    const sourceTableCode = `T${sourceTableId}`;
    const destinationTableCode = `T${destinationTableId}`;
    const sourceDraft = tableDraftOrders[sourceTableId] ?? [];
    const sourceSubmitted = submittedOrdersByTable[sourceTableId] ?? [];
    const sourcePreview = tablePaymentPreviews[sourceTableId] ?? null;
    const sourceBackendId = tableBackendIds[sourceTableId];
    const sourceQrOrder = qrOrders[sourceTableCode];
    const nextStage = tableOrderStages[sourceTableId] ?? "DRAFT";
    const carriedTotal = selectedTable.total;

    setTableDraftOrders((current) => ({ ...current, [destinationTableId]: sourceDraft, [sourceTableId]: [] }));
    setSubmittedOrdersByTable((current) => ({ ...current, [destinationTableId]: sourceSubmitted, [sourceTableId]: [] }));
    setTablePaymentPreviews((current) => ({ ...current, [destinationTableId]: sourcePreview, [sourceTableId]: null }));
    setTableOrderStages((current) => ({ ...current, [destinationTableId]: nextStage, [sourceTableId]: "DRAFT" }));
    setTableBackendIds((current) => {
      const next = { ...current };
      if (sourceBackendId) next[destinationTableId] = sourceBackendId;
      delete next[sourceTableId];
      return next;
    });
    setQrOrders((current) => {
      const next = { ...current };
      if (sourceQrOrder) {
        next[destinationTableCode] = { ...sourceQrOrder, tableCode: destinationTableCode, tableId: sourceBackendId ?? destinationTableId };
      } else {
        delete next[destinationTableCode];
      }
      delete next[sourceTableCode];
      return next;
    });
    setTableState((current) =>
      current.map((table) => {
        if (table.id === sourceTableId) {
          return { ...tables.find((baseTable) => baseTable.id === sourceTableId)!, source: undefined, settlementState: undefined, memberName: undefined };
        }
        if (table.id === destinationTableId) {
          return {
            ...table,
            status: "occupied",
            guests: selectedTable.guests,
            source: selectedTable.source,
            settlementState: nextStage,
            memberName: selectedTable.memberName,
            total: carriedTotal,
            course: nextStage === "PENDING_SETTLEMENT" ? "Payment pending" : sourceSubmitted.length > 0 ? `${sourceSubmitted.length} rounds sent to kitchen` : sourceDraft.length > 0 ? "Draft order in progress" : selectedTable.course
          };
        }
        return table;
      })
    );
    setSelectedTableId(destinationTableId);
    setTargetTableId(sourceTableId);
    setView("ordering");
  };

  const confirmTransferTable = async () => {
    if (selectedTable.id === targetTable.id || targetTable.status !== "available") return;
    const sourceTableId = selectedTable.id;
    const destinationTableId = targetTable.id;
    const sourceBackendTableId = tableBackendIds[sourceTableId] ?? selectedBackendTableId;
    const destinationBackendTableId = tableBackendIds[destinationTableId];

    if (!sourceBackendTableId || !destinationBackendTableId) {
      applyLocalTransferState(sourceTableId, destinationTableId);
      return;
    }

    try {
      await transferTableRequest(STORE_ID, sourceBackendTableId, destinationBackendTableId);
      await Promise.all([
        syncTableFromV2(`T${sourceTableId}`, sourceTableId),
        syncTableFromV2(`T${destinationTableId}`, destinationTableId)
      ]);
      await refreshReservations();
      setSelectedTableId(destinationTableId);
      setTargetTableId(sourceTableId);
      setView("ordering");
    } catch (error) {
      console.error("Failed to transfer table", error);
      applyLocalTransferState(sourceTableId, destinationTableId);
    }
  };

  const statusSummary = [
    { label: "Occupied", value: tableState.filter((table) => table.status === "occupied").length, tone: "sky" },
    { label: "Reserved", value: tableState.filter((table) => table.status === "reserved").length, tone: "rose" },
    { label: "Available", value: tableState.filter((table) => table.status === "available").length, tone: "mint" }
  ];

  const reservationSummary = [
    { label: "Checked in", value: reservationState.filter((entry) => entry.status === "checked-in").length, tone: "mint" },
    { label: "Waiting", value: reservationState.filter((entry) => entry.status === "waiting").length, tone: "peach" },
    { label: "Upcoming", value: reservationState.filter((entry) => entry.status === "upcoming").length, tone: "lilac" }
  ];

  const availableTables = tableState.filter((table) => table.status === "available");
  const availableReservationTables = tableState.filter(
    (table) => table.status === "available" || table.id === activeReservation?.tableId
  );

  return (
    <div className="app-shell">
      <main className="preview-stage">
        <section className="pos-window">
          <div className="persistent-nav persistent-nav-top">
            <div className="persistent-nav-group">
              {[
                ["tables", "Table Management"],
                ["reservations", "Reservations"],
                ["transfer", "Transfer Table"],
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
            <button
              className="persistent-nav-button persistent-nav-button-secondary"
              onClick={() =>
                window.open(
                  `${QR_ORDERING_BASE_URL}/?storeName=Riverside%20Branch&storeCode=1001&table=${encodeURIComponent(selectedTableCode)}`,
                  "_blank"
                )
              }
            >
              Open {selectedTableCode} QR page
            </button>
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
                    <button className="utility-button" onClick={() => openReservationDraft(null)}>+</button>
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

                <div className="reservation-toolbar">
                  <input
                    className="reservation-search"
                    value={reservationSearch}
                    onChange={(event) => setReservationSearch(event.target.value)}
                    placeholder="Search guest, area, time, or table"
                  />
                  <select
                    className="reservation-filter"
                    value={reservationStatusFilter}
                    onChange={(event) => setReservationStatusFilter(event.target.value as "all" | Reservation["status"])}
                  >
                    <option value="all">All statuses</option>
                    <option value="upcoming">Upcoming</option>
                    <option value="waiting">Waiting</option>
                    <option value="checked-in">Checked in</option>
                    <option value="no-show">No-show</option>
                    <option value="cancelled">Cancelled</option>
                  </select>
                </div>

                <div className="reservation-list">
                  {filteredReservations.map((entry) => (
                    <article key={entry.id} className="reservation-card">
                      <div className="reservation-main">
                        <div>
                          <p className="table-tag">{entry.time}</p>
                          <h3>{entry.name}</h3>
                          <p className="reservation-meta">
                            {entry.partySize} guests · {entry.area}
                            {entry.tableId ? ` · T${entry.tableId}` : ""}
                          </p>
                        </div>
                        <span className={`reservation-badge badge-${entry.status}`}>
                          {formatReservationStatus(entry.status)}
                        </span>
                      </div>
                      <div className="reservation-actions">
                        {entry.status !== "checked-in" && entry.status !== "cancelled" && entry.status !== "no-show" ? (
                          <button className="minor-pill" onClick={() => seatReservation(entry.id, entry.tableId ?? null)}>
                            Seat guests
                          </button>
                        ) : null}
                        {entry.status === "upcoming" ? (
                          <button className="minor-pill" onClick={() => updateReservationStatus(entry.id, "waiting")}>
                            Mark waiting
                          </button>
                        ) : null}
                        <button className="minor-pill" onClick={() => openReservationDraft(entry)}>Edit</button>
                      </div>
                    </article>
                  ))}
                </div>
              </section>

              <aside className="table-sidebar">
                <h2>Reservation details</h2>
                <div className="detail-card">
                  <div className="detail-row">
                    <span>Selected</span>
                    <strong>{activeReservation?.name ?? "New reservation"}</strong>
                  </div>
                  <div className="detail-row">
                    <span>Status</span>
                    <strong>{formatReservationStatus(activeReservation?.status ?? reservationDraft.status)}</strong>
                  </div>
                  <div className="detail-row">
                    <span>Assigned table</span>
                    <strong>{activeReservation?.tableId ? `T${activeReservation.tableId}` : "Not seated"}</strong>
                  </div>
                </div>

                <div className="detail-card reservation-editor">
                  <label className="reservation-field">
                    <span>Name</span>
                    <input
                      value={reservationDraft.name}
                      onChange={(event) => setReservationDraft((current) => ({ ...current, name: event.target.value }))}
                      placeholder="Guest name"
                    />
                  </label>
                  <label className="reservation-field">
                    <span>Time</span>
                    <input
                      type="time"
                      value={reservationDraft.time}
                      onChange={(event) => setReservationDraft((current) => ({ ...current, time: event.target.value }))}
                    />
                  </label>
                  <label className="reservation-field">
                    <span>Party size</span>
                    <input
                      type="number"
                      min={1}
                      max={20}
                      value={reservationDraft.partySize}
                      onChange={(event) =>
                        setReservationDraft((current) => ({
                          ...current,
                          partySize: Math.max(1, Number(event.target.value) || 1)
                        }))
                      }
                    />
                  </label>
                  <label className="reservation-field">
                    <span>Area</span>
                    <select
                      value={reservationDraft.area}
                      onChange={(event) => setReservationDraft((current) => ({ ...current, area: event.target.value }))}
                    >
                      {[...new Set(tableAreas)].map((area) => (
                        <option key={area} value={area}>
                          {area}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="reservation-field">
                    <span>Status</span>
                    <select
                      value={reservationDraft.status}
                      onChange={(event) =>
                        setReservationDraft((current) => ({
                          ...current,
                          status: event.target.value as Reservation["status"]
                        }))
                      }
                    >
                      <option value="upcoming">upcoming</option>
                      <option value="waiting">waiting</option>
                      <option value="checked-in">checked-in</option>
                      <option value="no-show">no-show</option>
                      <option value="cancelled">cancelled</option>
                    </select>
                  </label>
                  <label className="reservation-field">
                    <span>Seat to table</span>
                    <select
                      value={reservationSeatTableId ?? ""}
                      onChange={(event) =>
                        setReservationSeatTableId(event.target.value ? Number(event.target.value) : null)
                      }
                    >
                      <option value="">Auto assign best table</option>
                      {availableReservationTables.map((table) => (
                        <option key={table.id} value={table.id}>
                          Table {table.id} · {table.area}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>

                <div className="sidebar-actions">
                  <button className="sort-row" onClick={() => openReservationDraft(null)}>
                    <span>Add Walk-In</span>
                  </button>
                  <button className="sort-row" onClick={saveReservationDraft}>
                    <span>Save Reservation</span>
                  </button>
                  <button
                    className="sort-row"
                    onClick={() => {
                      if (activeReservation) {
                        seatReservation(activeReservation.id, reservationSeatTableId);
                      }
                    }}
                  >
                    <span>Seat Selected Guest</span>
                  </button>
                  <button
                    className="sort-row"
                    onClick={() => {
                      if (activeReservation) {
                        updateReservationStatus(activeReservation.id, "no-show");
                      }
                    }}
                  >
                    <span>Mark No-Show</span>
                  </button>
                  <button
                    className="sort-row"
                    onClick={() => {
                      if (activeReservation) {
                        updateReservationStatus(activeReservation.id, "cancelled");
                      }
                    }}
                  >
                    <span>Cancel Reservation</span>
                  </button>
                  <button className="sort-row" onClick={() => setView("tables")}>
                    <span>Open Floor Panel</span>
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
                    <div className="floorplan-grid">
                      {tableState.map((table) => {
                        const tableStage = deriveOrderStage({
                          table,
                          backendOrder: qrOrders[`T${table.id}`],
                          submittedOrders: submittedOrdersByTable[table.id] ?? [],
                          paymentPreview: tablePaymentPreviews[table.id] ?? null,
                          draftItems: tableDraftOrders[table.id] ?? []
                        });
                        const tableCapacity = getTableCapacity(table.id);
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
                              {tableCapacity} pax ·{" "}
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

                  <div className="floorplan-legend floorplan-legend-bottom">
                    <span className="legend-chip legend-available">Available</span>
                    <span className="legend-chip legend-occupied">Occupied</span>
                    <span className="legend-chip legend-reserved">Reserved</span>
                    <span className="legend-chip legend-billing">Billing</span>
                    <span className="legend-chip legend-attention">Action needed</span>
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
                    <div className="current-table-badge">
                      <span className="current-table-label">Current Table</span>
                      <strong>T{selectedTable.id}</strong>
                    </div>
                    <h2>Transfer table</h2>
                  </div>
                  <button className="minor-pill" onClick={() => setView("tables")}>
                    Back To Floor
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
                  <button className="cta-button" onClick={confirmTransferTable}>
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
                    <div className="heading-topline ordering-status-strip">
                      <div className="current-table-badge">
                        <span className="current-table-label">Current table</span>
                        <strong>T{selectedTable.id}</strong>
                      </div>
                      <button className="minor-pill order-stage-pill">{getStageLabel(selectedOrderStage)}</button>
                    </div>
                    <p className="stage-support-copy ordering-stage-copy">{getStageSupportText(selectedOrderStage)}</p>
                  </div>
                </div>

                <div className="category-grid">
                  <button
                    className={activeCategory === "all" ? "category-card active-all" : "category-card"}
                    onClick={() => setActiveCategory("all")}
                  >
                    <span>All Items</span>
                    <strong>{publishedMenu.length}</strong>
                  </button>
                  {categoryCards.map((category) => (
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
                          {hasCustomizations(item) ? <span className="dish-customization-tag">Customizable</span> : null}
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
                      <article key={item.cartLineId ?? item.id} className="ordering-cart-row">
                        <div>
                          <strong>{item.name}</strong>
                          <p>{item.note ?? item.category}</p>
                        </div>
                        <div className="qty-control">
                          <button onClick={() => updateQuantity(item.cartLineId ?? `${item.id}`, -1)}>-</button>
                          <span>{item.quantity}</span>
                          <button onClick={() => updateQuantity(item.cartLineId ?? `${item.id}`, 1)}>+</button>
                        </div>
                      </article>
                    ))}
                  </div>
                </div>

                <div className="ordering-panel-card">
                  <p className="sidebar-title">Actions</p>
                  <button className="sort-row">
                    <span>Add Note</span>
                  </button>
                  {effectiveDraftOrderItems.length > 0 ? (
                    <button className="sort-row" onClick={sendToKitchen}>
                      <span>Send To Kitchen</span>
                    </button>
                  ) : null}
                  {canMoveToSettlement ? (
                    <button className="sort-row" onClick={moveToSettlement}>
                      <span>Payment</span>
                    </button>
                  ) : null}
                  {selectedOrderStage === "PENDING_SETTLEMENT" ? (
                    <button className="sort-row" onClick={() => setView("payment")}>
                      <span>Collect Payment</span>
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
                    <span>GST (9%)</span>
                    <strong>{formatMoney(displayedTax)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Service Charge (10%)</span>
                    <strong>{formatMoney(displayedService)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Member benefit</span>
                    <strong>-{formatMoney(displayedMemberDiscount)}</strong>
                  </div>
                  {displayedCashierDiscount > 0 ? (
                    <div className="amount-row discount-row">
                      <span>Cashier discount</span>
                      <strong>-{formatMoney(displayedCashierDiscount)}</strong>
                    </div>
                  ) : null}
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

          {configuringItem ? (
            <div className="configurator-backdrop">
              <section className="configurator-panel">
                <div className="ordering-panel-head">
                  <div>
                    <p className="sidebar-title">Customize Item</p>
                    <h2>{configuringItem.name}</h2>
                  </div>
                  <button className="minor-pill" onClick={() => setConfiguringItem(null)}>
                    Close
                  </button>
                </div>

                <div className="configurator-groups">
                  {(configuringItem.attributeGroups ?? []).map((group, groupIndex) => (
                    <article key={`${group.name}-${groupIndex}`} className="configurator-group">
                      <div className="configurator-group-head">
                        <strong>{group.name}</strong>
                        <span>{group.required ? "Required" : "Optional"}</span>
                      </div>
                      <div className="configurator-options">
                        {group.values.map((value) => {
                          const selected = (attributeSelections[groupIndex] ?? []).includes(value.label);
                          return (
                            <button
                              key={value.label}
                              className={`config-option ${selected ? "config-option-active" : ""}`}
                              onClick={() => toggleAttributeValue(groupIndex, value.label, group.selectionMode)}
                            >
                              <span>{value.label}</span>
                              {value.priceDeltaCents > 0 ? <em>+SGD {(value.priceDeltaCents / 100).toFixed(1)}</em> : null}
                            </button>
                          );
                        })}
                      </div>
                    </article>
                  ))}

                  {(configuringItem.modifierGroups ?? []).map((group, groupIndex) => (
                    <article key={`${group.name}-${groupIndex}`} className="configurator-group">
                      <div className="configurator-group-head">
                        <strong>{group.name}</strong>
                        <span>{group.freeQuantity ? `First ${group.freeQuantity} free` : "Optional"}</span>
                      </div>
                      <div className="configurator-options">
                        {group.options.map((option) => {
                          const selected = (modifierSelections[groupIndex] ?? []).includes(option.label);
                          return (
                            <button
                              key={option.label}
                              className={`config-option ${selected ? "config-option-active" : ""}`}
                              onClick={() => toggleModifierOption(groupIndex, option.label)}
                            >
                              <span>{option.label}</span>
                              {option.priceDeltaCents > 0 ? <em>+SGD {(option.priceDeltaCents / 100).toFixed(1)}</em> : null}
                            </button>
                          );
                        })}
                      </div>
                    </article>
                  ))}

                  {(configuringItem.comboSlots ?? []).map((slot, slotIndex) => (
                    <article key={`${slot.name}-${slotIndex}`} className="configurator-group">
                      <div className="configurator-group-head">
                        <strong>{slot.name}</strong>
                        <span>{slot.minSelect ? `Pick at least ${slot.minSelect}` : "Optional"}</span>
                      </div>
                      <div className="configurator-options">
                        {slot.allowedSkuCodes.map((skuCode) => {
                          const selected = (comboSelections[slotIndex] ?? []).includes(skuCode);
                          return (
                            <button
                              key={skuCode}
                              className={`config-option ${selected ? "config-option-active" : ""}`}
                              onClick={() => toggleComboSelection(slotIndex, skuCode)}
                            >
                              <span>{skuCode}</span>
                            </button>
                          );
                        })}
                      </div>
                    </article>
                  ))}
                </div>

                <div className="configurator-footer">
                  <div>
                    <p className="sidebar-title">Add to Cart</p>
                    <strong>{formatMoney(configuringPrice)}</strong>
                  </div>
                  <button className="minor-pill configurator-submit" onClick={confirmConfiguredItem}>
                    Confirm Add
                  </button>
                </div>
              </section>
            </div>
          ) : null}

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
                    Continue Ordering
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
                    <span>GST (9%)</span>
                    <strong>{formatMoney(displayedTax)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Service Charge (10%)</span>
                    <strong>{formatMoney(displayedService)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Member discount</span>
                    <strong>-{formatMoney(displayedMemberDiscount)}</strong>
                  </div>
                  {displayedCashierDiscount > 0 ? (
                    <div className="amount-row discount-row">
                      <span>Cashier discount</span>
                      <strong>-{formatMoney(displayedCashierDiscount)}</strong>
                    </div>
                  ) : null}
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
                    <span>Member Recharge</span>
                  </button>
                  <button className="sort-row" onClick={() => setView("split")}>
                    <span>Split bill</span>
                  </button>
                  {canMoveToSettlement ? (
                    <button className="sort-row" onClick={moveToSettlement}>
                      <span>Payment</span>
                    </button>
                  ) : null}
                  {selectedOrderStage === "PENDING_SETTLEMENT" ? (
                    <button className="cta-button" onClick={() => setView("payment")}>
                      Collect Payment
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
                    Back To Ordering
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
                    <button className="method-button active">Equal Split</button>
                    <button className="method-button">By Seat</button>
                    <button className="method-button">Custom Amount</button>
                  </div>
                </div>

                <div className="payment-card accent-card">
                  <p className="sidebar-title">Per guest</p>
                  <h3>{formatMoney(perGuest)}</h3>
                  <p className="accent-copy">
                    Each guest can tap separately on the reader. Service charge and GST stay evenly
                    distributed unless you switch to custom split.
                  </p>
                  <button className="cta-button" onClick={() => setView("payment")}>
                    Continue To Collection
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
                    Back To Ordering
                  </button>
                </div>

                <div className="payment-hero">
                  <div className="payment-card feature-card">
                    <p className="sidebar-title">Ready to collect</p>
                    <h3>{formatMoney(paymentTotal)}</h3>
                    <p className="accent-copy">
                      {selectedTable.source === "QR"
                        ? "A QR table order has already been placed by the guest. Confirm the basket and collect payment at cashier."
                        : "This table order is ready to settle. Confirm the order summary, then collect payment at cashier."}
                    </p>
                  </div>
                </div>

                <div className="payment-card payment-order-focus">
                  <div className="payment-order-focus-head">
                    <div>
                      <p className="sidebar-title">Order to collect</p>
                      <h3>
                        {selectedTable.source === "QR" ? "Table code order" : "Cashier order"} · T
                        {selectedTable.id}
                      </h3>
                      <p className="accent-copy">
                        {selectedTable.memberName ?? "Guest"} · {selectedTable.settlementState ?? "PENDING_SETTLEMENT"}
                      </p>
                    </div>
                    {selectedTable.source === "QR" ? (
                      <span className="reservation-badge badge-checked-in">SYNCED</span>
                    ) : (
                      <span className="reservation-badge badge-upcoming">CASHIER</span>
                    )}
                  </div>

                  <div className="payment-order-kpis">
                    <article className="payment-kpi-card">
                      <span>Items</span>
                      <strong>{paymentOrderItems.reduce((sum, item) => sum + item.quantity, 0)}</strong>
                    </article>
                    <article className="payment-kpi-card">
                      <span>Rounds</span>
                      <strong>{Math.max(selectedSubmittedOrders.length, 1)}</strong>
                    </article>
                    <article className="payment-kpi-card payment-kpi-card-highlight">
                      <span>Total due</span>
                      <strong>{formatMoney(paymentTotal)}</strong>
                    </article>
                  </div>
                </div>

                <div className="payment-card payment-order-list-card">
                  <div className="payment-section-head">
                    <div>
                      <p className="sidebar-title">Order items</p>
                      <h3>{paymentOrderItems.length} lines ready for settlement</h3>
                    </div>
                    <div className="payment-section-meta">
                      <span>{selectedTable.source === "QR" ? "QR synced order" : "Cashier order"}</span>
                      <strong>{formatMoney(paymentTotal)}</strong>
                    </div>
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
                </div>

              </section>

              <aside className="payment-panel payment-panel-collection">
                <div className="payment-card accent-card payment-collect-card">
                  <p className="sidebar-title">Collect Payment</p>
                  <h3>{formatMoney(paymentTotal)}</h3>
                  <p className="accent-copy">
                    The cashier confirms the order and picks the payment method before closing this
                    table.
                  </p>
                  <button
                    className="cta-button"
                    disabled={!canCollectPayment(selectedOrderStage)}
                    onClick={() => void completeSettlement()}
                  >
                    Collect with {selectedCollectionMethodLabel}
                  </button>
                  {!canCollectPayment(selectedOrderStage) ? (
                    <p className="payment-guard-copy">
                      Move this order to <strong>Payment Pending</strong> before collecting payment.
                    </p>
                  ) : null}
                </div>

                <div className="payment-card">
                  <div className="payment-section-head">
                    <div>
                      <p className="sidebar-title">Payment methods</p>
                      <h3>{selectedCollectionMethodLabel}</h3>
                    </div>
                  </div>

                  <div className="payment-method-grid payment-method-grid-primary">
                    {paymentMethods.map((method) => (
                      <button
                        key={method.id}
                        className={`method-button payment-method-card ${
                          selectedCollectionMethod === method.id ? "active" : ""
                        }`}
                        onClick={() => setSelectedCollectionMethod(method.id)}
                      >
                        <strong>{method.label}</strong>
                        <span>{method.accent}</span>
                      </button>
                    ))}
                  </div>
                </div>

                <div className="payment-card">
                  <div className="payment-section-head">
                    <div>
                      <p className="sidebar-title">Cashier tools</p>
                      <h3>Member, promotion, and discount</h3>
                    </div>
                  </div>

                  <div className="payment-method-grid payment-method-grid-secondary">
                    <button
                      className={`method-button payment-method-card payment-tool-card ${
                        activePaymentAssistTool === "member" ? "active" : ""
                      }`}
                      onClick={() =>
                        setActivePaymentAssistTool((current) => (current === "member" ? null : "member"))
                      }
                    >
                      <strong>Member</strong>
                      <span>
                        {paymentMemberDiscount > 0
                          ? `${formatMoney(paymentMemberDiscount)} applied`
                          : "Lookup or apply benefits"}
                      </span>
                    </button>
                    <button
                      className={`method-button payment-method-card payment-tool-card ${
                        activePaymentAssistTool === "promotion" ? "active" : ""
                      }`}
                      onClick={() =>
                        setActivePaymentAssistTool((current) => (current === "promotion" ? null : "promotion"))
                      }
                    >
                      <strong>Promotion code</strong>
                      <span>{promotionRuleLabel}</span>
                    </button>
                    <button
                      className={`method-button payment-method-card payment-tool-card ${
                        activePaymentAssistTool === "discount" ? "active" : ""
                      }`}
                      onClick={() =>
                        setActivePaymentAssistTool((current) => (current === "discount" ? null : "discount"))
                      }
                    >
                      <strong>Discount</strong>
                      <span>{cashierDiscountLabel}</span>
                    </button>
                  </div>
                  {activePaymentAssistTool === "discount" ? (
                    <div className="payment-discount-panel">
                      <div className="payment-discount-options">
                        {cashierDiscountOptions.map((option) => (
                          <button
                            key={option.label}
                            className={`method-button payment-discount-option ${
                              cashierDiscountMode === "preset" && cashierDiscountRate === option.rate ? "active" : ""
                            }`}
                            onClick={() => {
                              setCashierDiscountMode("preset");
                              setCashierDiscountRate(option.rate);
                            }}
                          >
                            <strong>{option.label}</strong>
                          </button>
                        ))}
                      </div>
                      <div className="payment-discount-custom-grid">
                        <button
                          className={`method-button payment-discount-option ${
                            cashierDiscountMode === "percent" ? "active" : ""
                          }`}
                          onClick={() => setCashierDiscountMode("percent")}
                        >
                          <strong>Open Discount</strong>
                        </button>
                        <button
                          className={`method-button payment-discount-option ${
                            cashierDiscountMode === "amount" ? "active" : ""
                          }`}
                          onClick={() => setCashierDiscountMode("amount")}
                        >
                          <strong>Open Amount</strong>
                        </button>
                      </div>
                      {cashierDiscountMode === "percent" ? (
                        <div className="payment-discount-input-row">
                          <label>
                            <span>Custom percent</span>
                            <input
                              type="number"
                              min="0"
                              max="100"
                              step="0.1"
                              value={cashierDiscountPercentInput}
                              onChange={(event) => setCashierDiscountPercentInput(event.target.value)}
                              placeholder="Enter %"
                            />
                          </label>
                        </div>
                      ) : null}
                      {cashierDiscountMode === "amount" ? (
                        <div className="payment-discount-input-row">
                          <label>
                            <span>Custom amount</span>
                            <input
                              type="number"
                              min="0"
                              step="0.01"
                              value={cashierDiscountAmountInput}
                              onChange={(event) => setCashierDiscountAmountInput(event.target.value)}
                              placeholder="Enter SGD"
                            />
                          </label>
                        </div>
                      ) : null}
                    </div>
                  ) : null}
                  {activePaymentAssistTool === "promotion" ? (
                    <div className="payment-discount-panel">
                      <div className="payment-discount-options promotion-rule-options">
                        {promotionRules.map((rule) => (
                          <button
                            key={rule.id}
                            className={`method-button payment-discount-option ${
                              selectedPromotionRuleId === rule.id ? "active" : ""
                            }`}
                            onClick={() => setSelectedPromotionRuleId(rule.id)}
                          >
                            <strong>{rule.label}</strong>
                          </button>
                        ))}
                      </div>
                    </div>
                  ) : null}
                </div>

                <div className="payment-card payment-breakdown-compact">
                  <div className="payment-section-head">
                    <div>
                      <p className="sidebar-title">Settlement summary</p>
                      <h3>Final due today</h3>
                    </div>
                  </div>

                  <div className="amount-row">
                    <span>Subtotal</span>
                    <strong>{formatMoney(paymentSubtotal)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>GST (9%)</span>
                    <strong>{formatMoney(paymentTax)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Service Charge (10%)</span>
                    <strong>{formatMoney(paymentService)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Member benefit</span>
                    <strong>-{formatMoney(paymentMemberDiscount)}</strong>
                  </div>
                  {paymentCashierDiscount > 0 ? (
                    <div className="amount-row discount-row">
                      <span>Cashier discount</span>
                      <strong>-{formatMoney(paymentCashierDiscount)}</strong>
                    </div>
                  ) : null}
                  <div className="amount-row discount-row">
                    <span>Promotion benefit</span>
                    <strong>-{formatMoney(paymentPromotionDiscount)}</strong>
                  </div>
                  <div className="amount-row total">
                    <span>Total due</span>
                    <strong>{formatMoney(paymentTotal)}</strong>
                  </div>
                </div>
              </aside>
            </div>
          )}

          {view === "success" && (
            <div className="review-layout review-layout-single">
              <section className="payment-summary success-layout success-layout-simple">
                <div className="success-orb">✓</div>
                <div className="current-table-badge">
                  <span className="current-table-label">Current table</span>
                  <strong>T{successTableId}</strong>
                </div>
                <h2>Confirm payment</h2>
                <p className="success-copy">
                  Confirm the final amount received, then close this cashier settlement.
                </p>

                <div className="success-amount-card">
                  <span>Final amount received</span>
                  <strong>{formatMoney(successAmount)}</strong>
                  <p>
                    {successMethodLabel} · Table {successTableId}
                  </p>
                </div>

                <button className="cta-button success-done-button" onClick={() => {
                  // Clear all local state for this table after settlement
                  const tid = selectedTable?.id;
                  if (tid) {
                    setTableDraftOrders((prev) => { const next = {...prev}; delete next[tid]; return next; });
                    setSubmittedOrdersByTable((prev) => { const next = {...prev}; delete next[tid]; return next; });
                    setTableOrderStages((prev) => { const next = {...prev}; delete next[tid]; return next; });
                    setTablePaymentPreviews((prev) => { const next = {...prev}; delete next[tid]; return next; });
                    setTableBackendIds((prev) => { const next = {...prev}; delete next[tid]; return next; });
                    const tCode = `T${tid}`;
                    setQrOrders((prev) => { const next = {...prev}; delete next[tCode]; return next; });
                    // Update table card to available
                    setTableState((prev: TableCard[]) => prev.map((t: TableCard) =>
                      t.id === tid ? {...t, status: "available" as const, total: 0, itemCount: 0, source: undefined, settlementState: undefined} : t
                    ));
                  }
                  setCompletedSettlementSnapshot(null);
                  setView("tables");
                }}>
                  Payment Done
                </button>
              </section>
            </div>
          )}
          </div>
        </section>
      </main>
    </div>
  );
}

export default App;
