import { useEffect, useMemo, useState } from "react";

type CategoryMeta = {
  id: string;
  label: string;
  icon: string;
};

type AttributeValue = {
  code?: string;
  label: string;
  priceDeltaCents: number;
  defaultSelected: boolean;
  kitchenLabel?: string;
};

type AttributeGroup = {
  code?: string;
  name: string;
  selectionMode: "SINGLE" | "MULTIPLE";
  required: boolean;
  minSelect?: number;
  maxSelect?: number;
  values: AttributeValue[];
};

type ModifierOption = {
  code?: string;
  label: string;
  priceDeltaCents: number;
  defaultSelected: boolean;
  kitchenLabel?: string;
};

type ModifierGroup = {
  code?: string;
  name: string;
  freeQuantity?: number;
  minSelect?: number;
  maxSelect?: number;
  options: ModifierOption[];
};

type ComboSlot = {
  code?: string;
  name: string;
  minSelect?: number;
  maxSelect?: number;
  allowedSkuCodes: string[];
};

type MenuItem = {
  id: number;
  productCode: string;
  skuId: number;
  skuCode: string;
  name: string;
  category: string;
  price: number;
  memberPrice?: number;
  description: string;
  spicy?: boolean;
  sales?: number;
  likes?: number;
  image: string;
  attributeGroups: AttributeGroup[];
  modifierGroups: ModifierGroup[];
  comboSlots: ComboSlot[];
};

type CartItem = MenuItem & {
  cartLineId: string;
  selectionKey: string;
  quantity: number;
  remark: string;
};

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

type ActiveOrderDto = {
  activeOrderId: string;
  orderNo: string;
  storeId: number;
  tableId: number;
  tableCode: string;
  orderSource: string;
  status: string;
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

type QrSubmitResponse = {
  activeOrderId: string;
  orderNo: string;
  storeCode: string;
  tableCode: string;
  status: string;
  payableAmountCents: number;
  totalItemCount: number;
};

type QrContextResponse = {
  storeId: number;
  storeCode: string;
  storeName: string;
  tableId: number;
  tableCode: string;
  tableName: string;
  tableStatus: string;
  currentActiveOrder: ActiveOrderDto | null;
  submittedOrders: SubmittedOrderDto[];
};

type SubmittedOrderDto = {
  submittedOrderId: string;
  orderNo: string;
  sourceOrderType: string;
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

type SubmitItemPayload = {
  skuId: number;
  skuCode: string;
  skuName: string;
  quantity: number;
  unitPriceCents: number;
  remark: string;
};

type QrMenuResponse = {
  storeId: number;
  storeCode: string;
  storeName: string;
  categories: Array<{
    categoryId: number;
    categoryCode: string;
    categoryName: string;
    items: Array<{
      productId: number;
      productCode: string;
      productName: string;
      skuId: number;
      skuCode: string;
      skuName: string;
      unitPriceCents: number;
      attributeGroups?: AttributeGroup[];
      modifierGroups?: ModifierGroup[];
      comboSlots?: ComboSlot[];
    }>;
  }>;
};

const categoryIcons = ["🟢", "🍱", "🍟", "🥤", "🧁", "🍜", "🍛", "🍤"];
const menuImagePool = [
  "https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=600&q=80",
  "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=600&q=80",
  "https://images.unsplash.com/photo-1562967916-eb82221dfb92?auto=format&fit=crop&w=600&q=80",
  "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=600&q=80",
  "https://images.unsplash.com/photo-1558857563-b371033873b8?auto=format&fit=crop&w=600&q=80",
  "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=600&q=80",
  "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=600&q=80",
  "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=600&q=80"
];

function money(value: number) {
  return new Intl.NumberFormat("en-SG", {
    style: "currency",
    currency: "SGD",
    currencyDisplay: "code",
    minimumFractionDigits: 2
  }).format(value);
}

function buildIncrementalSubmissionItems(
  cart: CartItem[],
  existingItems: Array<{ skuId: number; remark?: string | null; quantity: number }>
): SubmitItemPayload[] {
  const existingQuantities = new Map<string, number>();

  existingItems.forEach((item) => {
    const key = `${item.skuId}::${item.remark ?? ""}`;
    existingQuantities.set(key, (existingQuantities.get(key) ?? 0) + item.quantity);
  });

  return cart
    .map((item) => {
      const existingQuantity = existingQuantities.get(`${item.skuId}::${item.remark ?? ""}`) ?? 0;
      const nextQuantity = item.quantity - existingQuantity;

      if (nextQuantity <= 0) {
        return null;
      }

      return {
        skuId: item.skuId,
        skuCode: item.skuCode,
        skuName: item.name,
        quantity: nextQuantity,
        unitPriceCents: Math.round(item.price * 100),
        remark: item.remark
      } satisfies SubmitItemPayload;
    })
    .filter((item): item is SubmitItemPayload => Boolean(item));
}

function hasCustomizations(item: MenuItem) {
  return item.attributeGroups.length > 0 || item.modifierGroups.length > 0 || item.comboSlots.length > 0;
}

function buildSelectionSummary(
  item: MenuItem,
  attributeSelections: Record<number, string[]>,
  modifierSelections: Record<number, string[]>,
  comboSelections: Record<number, string[]>
) {
  const parts: string[] = [];

  item.attributeGroups.forEach((group, index) => {
    const selected = attributeSelections[index] ?? [];
    if (selected.length > 0) {
      parts.push(`${group.name}:${selected.join("/")}`);
    }
  });
  item.modifierGroups.forEach((group, index) => {
    const selected = modifierSelections[index] ?? [];
    if (selected.length > 0) {
      parts.push(`${group.name}:${selected.join("/")}`);
    }
  });
  item.comboSlots.forEach((slot, index) => {
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

  item.attributeGroups.forEach((group, index) => {
    const selected = new Set(attributeSelections[index] ?? []);
    group.values.forEach((value) => {
      if (selected.has(value.label)) {
        deltaCents += value.priceDeltaCents;
      }
    });
  });

  item.modifierGroups.forEach((group, index) => {
    const selected = modifierSelections[index] ?? [];
    const freeQuantity = group.freeQuantity ?? 0;
    group.options.forEach((option) => {
      const occurrences = selected.filter((value) => value === option.label).length;
      deltaCents += Math.max(0, occurrences - freeQuantity) * option.priceDeltaCents;
    });
  });

  return deltaCents / 100;
}

function buildDefaultAttributeSelections(item: MenuItem) {
  return Object.fromEntries(
    item.attributeGroups.map((group, index) => [
      index,
      group.values.filter((value) => value.defaultSelected).map((value) => value.label)
    ])
  ) as Record<number, string[]>;
}

function buildDefaultModifierSelections(item: MenuItem) {
  return Object.fromEntries(
    item.modifierGroups.map((group, index) => [
      index,
      group.options.filter((option) => option.defaultSelected).map((option) => option.label)
    ])
  ) as Record<number, string[]>;
}

function createCartLineId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export default function App() {
  const params = new URLSearchParams(window.location.search);
  const initialStoreName = params.get("storeName") ?? "Riverside Branch";
  const storeCode = params.get("storeCode") ?? "1001";
  const tableCode = params.get("table") ?? "T12";
  const [storeName, setStoreName] = useState(initialStoreName);
  const [categories, setCategories] = useState<CategoryMeta[]>([]);
  const [menu, setMenu] = useState<MenuItem[]>([]);
  const [activeCategory, setActiveCategory] = useState<string>("");
  const [keyword, setKeyword] = useState("");
  const [cart, setCart] = useState<CartItem[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [submitResult, setSubmitResult] = useState<QrSubmitResponse | null>(null);
  const [currentOrder, setCurrentOrder] = useState<ActiveOrderDto | null>(null);
  const [submittedOrders, setSubmittedOrders] = useState<SubmittedOrderDto[]>([]);
  const [configuringItem, setConfiguringItem] = useState<MenuItem | null>(null);
  const [attributeSelections, setAttributeSelections] = useState<Record<number, string[]>>({});
  const [modifierSelections, setModifierSelections] = useState<Record<number, string[]>>({});
  const [comboSelections, setComboSelections] = useState<Record<number, string[]>>({});

  const hydrateCartFromActiveOrder = (
    activeOrder: ActiveOrderDto | null,
    menuSource: MenuItem[],
    categorySource: CategoryMeta[],
    activeCategoryValue: string
  ) => {
    if (!activeOrder || menuSource.length === 0) {
      return [] as CartItem[];
    }

    return activeOrder.items.map((item, index) => {
      const matchedMenuItem =
        menuSource.find((menuItem) => menuItem.skuId === item.skuId) ??
        menuSource.find((menuItem) => menuItem.skuCode === item.skuCode);

      if (matchedMenuItem) {
        return {
          ...matchedMenuItem,
          quantity: item.quantity,
          cartLineId: `${item.skuId}-${index}`,
          selectionKey: `${item.skuId}::${item.remark ?? ""}`,
          remark: item.remark ?? ""
        };
      }

      return {
        id: item.skuId || index + 1,
        productCode: item.skuCode,
        skuId: item.skuId,
        skuCode: item.skuCode,
        name: item.skuName,
        category: activeCategoryValue || categorySource[0]?.id || "",
        price: item.unitPriceCents / 100,
        description: "Current table order",
        image: menuImagePool[index % menuImagePool.length],
        quantity: item.quantity,
        cartLineId: `${item.skuId}-${index}`,
        selectionKey: `${item.skuId}::${item.remark ?? ""}`,
        remark: item.remark ?? "",
        attributeGroups: [],
        modifierGroups: [],
        comboSlots: []
      };
    });
  };

  const visibleMenu = useMemo(
    () =>
      menu.filter((item) => {
        const matchCategory = activeCategory === "" ? true : item.category === activeCategory;
        const matchKeyword =
          keyword.trim() === "" || `${item.name} ${item.description}`.toLowerCase().includes(keyword.toLowerCase());
        return matchCategory && matchKeyword;
      }),
    [menu, activeCategory, keyword]
  );

  const existingSubmittedItems = useMemo(
    () =>
      submittedOrders.flatMap((order) =>
        order.items.map((item) => ({
          skuId: item.skuId,
          remark: item.remark,
          quantity: item.quantity
        }))
      ),
    [submittedOrders]
  );

  const currentTableItems = useMemo(() => {
    if (submittedOrders.length > 0) {
      return submittedOrders.flatMap((order) => order.items);
    }

    return currentOrder?.items ?? [];
  }, [currentOrder, submittedOrders]);

  const currentTablePayable = useMemo(() => {
    if (submittedOrders.length > 0) {
      return submittedOrders.reduce((sum, order) => sum + order.pricing.payableAmountCents, 0);
    }

    return currentOrder?.pricing.payableAmountCents ?? 0;
  }, [currentOrder, submittedOrders]);

  const subtotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const memberDiscount = 0;
  const promotionDiscount = subtotal >= 60 ? 8 : 0;
  const payable = subtotal - memberDiscount - promotionDiscount;
  const incrementalSubmissionItems = useMemo(
    () => buildIncrementalSubmissionItems(cart, existingSubmittedItems),
    [cart, existingSubmittedItems]
  );
  const configuringPrice = useMemo(
    () =>
      configuringItem
        ? configuringItem.price + calculateCustomizationPrice(configuringItem, attributeSelections, modifierSelections)
        : 0,
    [attributeSelections, configuringItem, modifierSelections]
  );

  useEffect(() => {
    const controller = new AbortController();

    const loadContextAndMenu = async () => {
      try {
        const [contextResponse, menuResponse] = await Promise.all([
          fetch(
            `/api/v2/qr-ordering/context?storeCode=${encodeURIComponent(storeCode)}&tableCode=${encodeURIComponent(tableCode)}`,
            { signal: controller.signal }
          ),
          fetch(`/api/v2/qr-ordering/menu?storeCode=${encodeURIComponent(storeCode)}`, {
            signal: controller.signal
          })
        ]);

        if (!contextResponse.ok || !menuResponse.ok) {
          return;
        }

        const contextPayload = (await contextResponse.json()) as ApiResponse<QrContextResponse>;
        const menuPayload = (await menuResponse.json()) as ApiResponse<QrMenuResponse>;

        if (contextPayload.code === 0 && contextPayload.data) {
          setStoreName(contextPayload.data.storeName);
          setCurrentOrder(contextPayload.data.currentActiveOrder ?? null);
          setSubmittedOrders(contextPayload.data.submittedOrders ?? []);
        }

        if (menuPayload.code === 0 && menuPayload.data) {
          setStoreName(menuPayload.data.storeName);
          const nextCategories = menuPayload.data.categories.map((category, index) => ({
            id: category.categoryCode,
            label: category.categoryName,
            icon: categoryIcons[index % categoryIcons.length]
          }));
          const nextMenu = menuPayload.data.categories.flatMap((category, categoryIndex) =>
            category.items.map((item, itemIndex) => ({
              id: item.productId,
              productCode: item.productCode,
              skuId: item.skuId,
              skuCode: item.skuCode,
              name: item.skuName || item.productName,
              category: category.categoryCode,
              price: item.unitPriceCents / 100,
              description: category.categoryName,
              sales: 100 + itemIndex * 17,
              likes: 300 + categoryIndex * 41 + itemIndex * 13,
              image: menuImagePool[(categoryIndex + itemIndex) % menuImagePool.length],
              attributeGroups: item.attributeGroups ?? [],
              modifierGroups: item.modifierGroups ?? [],
              comboSlots: item.comboSlots ?? []
            }))
          );
          setCategories(nextCategories);
          setMenu(nextMenu);
          setActiveCategory((current) => current || nextCategories[0]?.id || "");
        }
      } catch (error) {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }
      }
    };

    void loadContextAndMenu();

    return () => controller.abort();
  }, [storeCode, tableCode]);

  useEffect(() => {
    if (menu.length === 0 || currentTableItems.length === 0) {
      return;
    }

    setCart((current) =>
      current.length > 0
        ? current
        : hydrateCartFromActiveOrder(
            {
              activeOrderId: currentOrder?.activeOrderId ?? "submitted",
              orderNo: currentOrder?.orderNo ?? (submittedOrders[0]?.orderNo ?? "submitted"),
              storeId: currentOrder?.storeId ?? 0,
              tableId: currentOrder?.tableId ?? 0,
              tableCode,
              orderSource: currentOrder?.orderSource ?? "QR",
              status: currentOrder?.status ?? "SUBMITTED",
              memberId: currentOrder?.memberId ?? null,
              items: currentTableItems.map((item) => ({
                skuId: item.skuId,
                skuCode: item.skuCode,
                skuName: item.skuName,
                quantity: item.quantity,
                unitPriceCents: item.unitPriceCents,
                remark: item.remark,
                lineTotalCents: item.lineTotalCents
              })),
              pricing: {
                originalAmountCents: 0,
                memberDiscountCents: 0,
                promotionDiscountCents: 0,
                payableAmountCents: currentTablePayable
              }
            },
            menu,
            categories,
            activeCategory
          )
    );
  }, [activeCategory, categories, currentOrder, currentTableItems, currentTablePayable, menu, submittedOrders, tableCode]);

  const commitConfiguredItem = (
    item: MenuItem,
    nextAttributeSelections: Record<number, string[]>,
    nextModifierSelections: Record<number, string[]>,
    nextComboSelections: Record<number, string[]>
  ) => {
    const summary = buildSelectionSummary(item, nextAttributeSelections, nextModifierSelections, nextComboSelections);
    const nextPrice = item.price + calculateCustomizationPrice(item, nextAttributeSelections, nextModifierSelections);
    const selectionKey = `${item.skuId}::${summary}`;

    setCart((current) => {
      const existing = current.find((entry) => entry.selectionKey === selectionKey);
      if (existing) {
        return current.map((entry) =>
          entry.selectionKey === selectionKey ? { ...entry, quantity: entry.quantity + 1 } : entry
        );
      }

      return [
        ...current,
        {
          ...item,
          price: nextPrice,
          quantity: 1,
          cartLineId: createCartLineId(),
          selectionKey,
          remark: summary
        }
      ];
    });
  };

  const addItem = (item: MenuItem) => {
    if (hasCustomizations(item)) {
      setConfiguringItem(item);
      setAttributeSelections(buildDefaultAttributeSelections(item));
      setModifierSelections(buildDefaultModifierSelections(item));
      setComboSelections(
        Object.fromEntries(item.comboSlots.map((_, index) => [index, []])) as Record<number, string[]>
      );
      return;
    }

    commitConfiguredItem(item, {}, {}, {});
  };

  const changeQty = (cartLineId: string, delta: number) => {
    setCart((current) =>
      current
        .map((entry) =>
          entry.cartLineId === cartLineId ? { ...entry, quantity: Math.max(0, entry.quantity + delta) } : entry
        )
        .filter((entry) => entry.quantity > 0)
    );
  };

  const toggleAttributeValue = (groupIndex: number, valueLabel: string, selectionMode: "SINGLE" | "MULTIPLE") => {
    setAttributeSelections((current) => {
      const selected = current[groupIndex] ?? [];
      if (selectionMode === "SINGLE") {
        return {
          ...current,
          [groupIndex]: [valueLabel]
        };
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
        [slotIndex]: selected.includes(skuCode) ? selected.filter((value) => value !== skuCode) : [...selected, skuCode]
      };
    });
  };

  const confirmConfiguredItem = () => {
    if (!configuringItem) {
      return;
    }

    for (const [index, group] of configuringItem.attributeGroups.entries()) {
      const selected = attributeSelections[index] ?? [];
      const minSelect = group.required ? Math.max(1, group.minSelect ?? 1) : group.minSelect ?? 0;
      const maxSelect = group.maxSelect ?? (group.selectionMode === "SINGLE" ? 1 : Number.MAX_SAFE_INTEGER);

      if (selected.length < minSelect) {
        setSubmitError(`Please select ${group.name} first`);
        return;
      }

      if (selected.length > maxSelect) {
        setSubmitError(`You can select up to ${maxSelect} options for ${group.name}`);
        return;
      }
    }

    for (const [index, group] of configuringItem.modifierGroups.entries()) {
      const selected = modifierSelections[index] ?? [];
      const minSelect = group.minSelect ?? 0;
      const maxSelect = group.maxSelect ?? Number.MAX_SAFE_INTEGER;
      if (selected.length < minSelect) {
        setSubmitError(`Please select at least ${minSelect} option(s) for ${group.name}`);
        return;
      }
      if (selected.length > maxSelect) {
        setSubmitError(`You can select up to ${maxSelect} options for ${group.name}`);
        return;
      }
    }

    for (const [index, slot] of configuringItem.comboSlots.entries()) {
      const selected = comboSelections[index] ?? [];
      const minSelect = slot.minSelect ?? 0;
      const maxSelect = slot.maxSelect ?? Number.MAX_SAFE_INTEGER;
      if (selected.length < minSelect) {
        setSubmitError(`Please select ${slot.name} first`);
        return;
      }
      if (selected.length > maxSelect) {
        setSubmitError(`You can select up to ${maxSelect} choices for ${slot.name}`);
        return;
      }
    }

    setSubmitError("");
    commitConfiguredItem(configuringItem, attributeSelections, modifierSelections, comboSelections);
    setConfiguringItem(null);
  };

  const submitOrder = async () => {
    if (cart.length === 0 || submitting) {
      return;
    }

    if (incrementalSubmissionItems.length === 0) {
      setSubmitError("No new items to submit");
      return;
    }

    setSubmitting(true);
    setSubmitError("");

    try {
      const response = await fetch("/api/v2/qr-ordering/submit", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          storeCode,
          tableCode,
          memberId: null,
          items: incrementalSubmissionItems
        })
      });

      if (!response.ok) {
        throw new Error(`Request failed with status ${response.status}`);
      }

      const payload = (await response.json()) as ApiResponse<QrSubmitResponse>;
      if (payload.code !== 0 || !payload.data) {
        throw new Error(payload.message || "Unable to submit order");
      }

      let latestActiveOrder: ActiveOrderDto | null = null;
      const refreshedContextResponse = await fetch(
        `/api/v2/qr-ordering/context?storeCode=${encodeURIComponent(storeCode)}&tableCode=${encodeURIComponent(tableCode)}`
      );
      if (refreshedContextResponse.ok) {
        const refreshedContextPayload = (await refreshedContextResponse.json()) as ApiResponse<QrContextResponse>;
        if (refreshedContextPayload.code === 0 && refreshedContextPayload.data) {
          latestActiveOrder = refreshedContextPayload.data.currentActiveOrder ?? null;
          setCurrentOrder(latestActiveOrder);
          setSubmittedOrders(refreshedContextPayload.data.submittedOrders ?? []);
          const latestItems =
            (refreshedContextPayload.data.submittedOrders ?? []).flatMap((order) => order.items);
          if (latestItems.length > 0 || latestActiveOrder) {
            setCart(
              hydrateCartFromActiveOrder(
                latestActiveOrder ?? {
                  activeOrderId: payload.data.activeOrderId,
                  orderNo: payload.data.orderNo,
                  storeId: refreshedContextPayload.data.storeId,
                  tableId: refreshedContextPayload.data.tableId,
                  tableCode: refreshedContextPayload.data.tableCode,
                  orderSource: "QR",
                  status: payload.data.status,
                  memberId: null,
                  items: latestItems.length > 0
                    ? latestItems.map((item) => ({
                        skuId: item.skuId,
                        skuCode: item.skuCode,
                        skuName: item.skuName,
                        quantity: item.quantity,
                        unitPriceCents: item.unitPriceCents,
                        remark: item.remark,
                        lineTotalCents: item.lineTotalCents
                      }))
                    : cart.map((item) => ({
                        skuId: item.skuId,
                        skuCode: item.skuCode,
                        skuName: item.name,
                        quantity: item.quantity,
                        unitPriceCents: Math.round(item.price * 100),
                        remark: item.remark,
                        lineTotalCents: Math.round(item.price * 100) * item.quantity
                      })),
                  pricing: {
                    originalAmountCents: 0,
                    memberDiscountCents: 0,
                    promotionDiscountCents: 0,
                    payableAmountCents: payload.data.payableAmountCents
                  }
                },
                menu,
                categories,
                activeCategory
              )
            );
          }
        }
      }

      setSubmitResult(payload.data);
      if (!latestActiveOrder) {
        setCurrentOrder({
          activeOrderId: payload.data.activeOrderId,
          orderNo: payload.data.orderNo,
          storeId: 0,
          tableId: 0,
          tableCode: payload.data.tableCode,
          orderSource: "QR",
          status: payload.data.status,
          memberId: null,
          items: cart.map((item) => ({
            skuId: item.skuId,
            skuCode: item.skuCode,
            skuName: item.name,
            quantity: item.quantity,
            unitPriceCents: Math.round(item.price * 100),
            remark: "",
            lineTotalCents: Math.round(item.price * 100) * item.quantity
          })),
          pricing: {
            originalAmountCents: Math.round(subtotal * 100),
            memberDiscountCents: Math.round(memberDiscount * 100),
            promotionDiscountCents: Math.round(promotionDiscount * 100),
            payableAmountCents: payload.data.payableAmountCents
          }
        });
      }
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : "Unable to submit order");
    } finally {
      setSubmitting(false);
    }
  };

  if (submitResult) {
    return (
      <div className="app-shell">
        <div className="mobile-shell success-shell">
          <div className="success-orb">✓</div>
          <p className="eyebrow">
            {storeName} · Table {submitResult.tableCode}
          </p>
          <h1>Order Submitted</h1>
          <p className="hero-copy">
            Your items have been sent to the cashier queue. Store staff will review the order and complete payment at checkout.
          </p>

          <div className="success-cards">
            <article className="metric-card">
              <span>Queue Number</span>
              <strong>{submitResult.orderNo}</strong>
            </article>
            <article className="metric-card">
              <span>Amount Due at POS</span>
              <strong>{money(submitResult.payableAmountCents / 100)}</strong>
            </article>
          </div>

          <button className="primary-button" onClick={() => setSubmitResult(null)}>
            Back to Menu
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <div className="mobile-shell">
        <header className="hero">
          <div className="hero-top">
            <button className="icon-lite">×</button>
            <div className="hero-title-block">
              <p className="eyebrow">Home</p>
              <h1>{storeName}</h1>
              <p className="hero-copy">Table {tableCode} · QR ordering · Pay at the cashier after ordering</p>
            </div>
            <button className="icon-lite">⋯</button>
          </div>
        </header>

        {currentOrder || submittedOrders.length > 0 ? (
          <section className="current-order-banner">
          <div>
            <p className="eyebrow">Current Table Order</p>
            <h2>{submittedOrders[0]?.orderNo ?? currentOrder?.orderNo ?? `${tableCode}-ACTIVE`}</h2>
            <p>
              {currentTableItems.length} item(s) · Awaiting cashier checkout · Due {money(currentTablePayable / 100)}
            </p>
          </div>
          <span className="table-order-state">In Progress</span>
          </section>
        ) : null}

        <section className="search-row">
          <input
            aria-label="Search menu"
            placeholder="Search menu"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
          />
        </section>

        <section className="menu-shell">
          <aside className="category-rail">
            {categories.map((category) => (
              <button
                key={category.id}
                className={`category-rail-item ${activeCategory === category.id ? "category-rail-item-active" : ""}`}
                onClick={() => setActiveCategory(category.id)}
              >
                <span className="category-rail-icon">{category.icon}</span>
                <span className="category-rail-label">{category.label}</span>
              </button>
            ))}
          </aside>

          <section className="menu-feed">
            {visibleMenu.map((item) => (
              <article key={item.id} className="menu-feed-card">
                <img className="menu-feed-image" src={item.image} alt={item.name} />
                <div className="menu-feed-copy">
                  <div className="menu-title-row">
                    <h3>{item.name}</h3>
                    {item.spicy ? <span className="flag">Spicy</span> : null}
                  </div>
                  <p>{item.description}</p>
                  <div className="menu-meta">
                    <span>Monthly {item.sales ?? 0}</span>
                    <span>Likes {item.likes ?? 0}</span>
                  </div>
                  <div className="price-row">
                    <strong>SGD {item.price.toFixed(2)}</strong>
                  </div>
                  {hasCustomizations(item) ? (
                    <div className="menu-customization-hint">
                      {item.attributeGroups.length + item.modifierGroups.length + item.comboSlots.length} customization groups
                    </div>
                  ) : null}
                </div>
                <button className="floating-add-button" onClick={() => addItem(item)}>
                  +
                </button>
              </article>
            ))}
            {visibleMenu.length === 0 ? (
              <article className="empty-menu-state">
                <strong>No menu items available right now</strong>
                <p>Please check the store menu, category status, or refresh and try again later.</p>
              </article>
            ) : null}
          </section>
        </section>

        <section className="cart-panel">
          <div className="section-head">
            <div>
              <p className="eyebrow">Current Selection</p>
              <h2>Cart</h2>
            </div>
            <span className="cart-count">{cart.length} item(s)</span>
          </div>

          <div className="cart-list">
            {cart.map((item) => (
              <article key={item.cartLineId} className="cart-row">
                <div className="cart-copy">
                  <strong>{item.name}</strong>
                  <p>{money(item.price)} each</p>
                  {item.remark ? <p className="cart-remark">{item.remark}</p> : null}
                </div>
                <div className="qty-group">
                  <button onClick={() => changeQty(item.cartLineId, -1)}>-</button>
                  <span>{item.quantity}</span>
                  <button onClick={() => changeQty(item.cartLineId, 1)}>+</button>
                </div>
              </article>
            ))}
          </div>

          <div className="summary-card">
            <div className="summary-row">
              <span>Subtotal</span>
              <strong>{money(subtotal)}</strong>
            </div>
            <div className="summary-row discount">
              <span>Member Discount</span>
              <strong>-{money(memberDiscount)}</strong>
            </div>
            <div className="summary-row discount">
              <span>Promotion Discount</span>
              <strong>-{money(promotionDiscount)}</strong>
            </div>
            <div className="summary-row total">
              <span>Amount Due at POS</span>
              <strong>{money(payable)}</strong>
            </div>
            <p className="summary-note">QR ordering only submits the order. Payment is completed at the POS by the cashier.</p>
          </div>

          {submitError ? <p className="submit-error">{submitError}</p> : null}

          <div className="floating-cart-bar">
            <div className="floating-cart-left">
              <span className="floating-cart-icon">🛒</span>
              <div>
                <strong>SGD {payable.toFixed(2)}</strong>
                <p>{cart.reduce((sum, item) => sum + item.quantity, 0)} item(s)</p>
              </div>
            </div>
            <button className="floating-cart-action" onClick={submitOrder} disabled={cart.length === 0 || submitting}>
              {submitting ? "Submitting" : "Submit Order"}
            </button>
          </div>
        </section>

        {configuringItem ? (
          <div className="configurator-backdrop">
            <section className="configurator-panel">
              <div className="section-head">
                <div>
                  <p className="eyebrow">Customize Item</p>
                  <h2>{configuringItem.name}</h2>
                </div>
                <button className="icon-lite" onClick={() => setConfiguringItem(null)}>
                  ×
                </button>
              </div>

              <div className="configurator-groups">
                {configuringItem.attributeGroups.map((group, groupIndex) => (
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

                {configuringItem.modifierGroups.map((group, groupIndex) => (
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

                {configuringItem.comboSlots.map((slot, slotIndex) => (
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
                  <p className="eyebrow">Add to Cart</p>
                  <strong>SGD {configuringPrice.toFixed(2)}</strong>
                </div>
                <button className="primary-button configurator-submit" onClick={confirmConfiguredItem}>
                  Add to Cart
                </button>
              </div>
            </section>
          </div>
        ) : null}
      </div>
    </div>
  );
}
