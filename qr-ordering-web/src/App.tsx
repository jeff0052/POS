import { useEffect, useMemo, useState } from "react";

type CategoryMeta = {
  id: string;
  label: string;
  icon: string;
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
};

type CartItem = MenuItem & {
  quantity: number;
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
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2
  }).format(value);
}

function buildIncrementalSubmissionItems(
  cart: CartItem[],
  existingItems: Array<{ skuId: number; quantity: number }>
): SubmitItemPayload[] {
  const existingQuantities = new Map<number, number>();

  existingItems.forEach((item) => {
    existingQuantities.set(item.skuId, (existingQuantities.get(item.skuId) ?? 0) + item.quantity);
  });

  return cart
    .map((item) => {
      const existingQuantity = existingQuantities.get(item.skuId) ?? 0;
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
        remark: ""
      } satisfies SubmitItemPayload;
    })
    .filter((item): item is SubmitItemPayload => Boolean(item));
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
          quantity: item.quantity
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
        description: "当前桌台订单",
        image: menuImagePool[index % menuImagePool.length],
        quantity: item.quantity
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
              image: menuImagePool[(categoryIndex + itemIndex) % menuImagePool.length]
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

  const addItem = (item: MenuItem) => {
    setCart((current) => {
      const existing = current.find((entry) => entry.id === item.id);
      if (existing) {
        return current.map((entry) => (entry.id === item.id ? { ...entry, quantity: entry.quantity + 1 } : entry));
      }

      return [...current, { ...item, quantity: 1 }];
    });
  };

  const changeQty = (id: number, delta: number) => {
    setCart((current) =>
      current
        .map((entry) => (entry.id === id ? { ...entry, quantity: Math.max(0, entry.quantity + delta) } : entry))
        .filter((entry) => entry.quantity > 0)
    );
  };

  const submitOrder = async () => {
    if (cart.length === 0 || submitting) {
      return;
    }

    if (incrementalSubmissionItems.length === 0) {
      setSubmitError("没有新增菜品可提交");
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
                        remark: "",
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
            {storeName} · 桌号 {submitResult.tableCode}
          </p>
          <h1>下单成功</h1>
          <p className="hero-copy">
            菜品已经发送到前台收银队列，门店员工会确认订单并在结账时完成收款。
          </p>

          <div className="success-cards">
            <article className="metric-card">
              <span>取号队列</span>
              <strong>{submitResult.orderNo}</strong>
            </article>
            <article className="metric-card">
              <span>待收金额</span>
              <strong>{money(submitResult.payableAmountCents / 100)}</strong>
            </article>
          </div>

          <button className="primary-button" onClick={() => setSubmitResult(null)}>
            返回继续点单
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
              <p className="eyebrow">首页</p>
              <h1>{storeName}</h1>
              <p className="hero-copy">桌号 {tableCode} · 扫码点餐 · 下单后前台结账</p>
            </div>
            <button className="icon-lite">⋯</button>
          </div>
        </header>

        {currentOrder || submittedOrders.length > 0 ? (
          <section className="current-order-banner">
          <div>
            <p className="eyebrow">当前桌台订单</p>
            <h2>{submittedOrders[0]?.orderNo ?? currentOrder?.orderNo ?? `${tableCode}-ACTIVE`}</h2>
            <p>
              {currentTableItems.length} 件商品 · 待前台结账 · 待收 {money(currentTablePayable / 100)}
            </p>
          </div>
          <span className="table-order-state">进行中</span>
          </section>
        ) : null}

        <section className="search-row">
          <input
            aria-label="Search menu"
            placeholder="搜索菜品"
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
                    {item.spicy ? <span className="flag">辣</span> : null}
                  </div>
                  <p>{item.description}</p>
                  <div className="menu-meta">
                    <span>月售{item.sales ?? 0}</span>
                    <span>点赞{item.likes ?? 0}</span>
                  </div>
                  <div className="price-row">
                    <strong>¥{item.price.toFixed(0)}</strong>
                  </div>
                </div>
                <button className="floating-add-button" onClick={() => addItem(item)}>
                  +
                </button>
              </article>
            ))}
            {visibleMenu.length === 0 ? (
              <article className="empty-menu-state">
                <strong>暂时没有可点菜品</strong>
                <p>请检查当前门店菜单、分类状态，或稍后刷新再试。</p>
              </article>
            ) : null}
          </section>
        </section>

        <section className="cart-panel">
          <div className="section-head">
            <div>
              <p className="eyebrow">当前已选</p>
              <h2>购物车</h2>
            </div>
            <span className="cart-count">{cart.length} 件商品</span>
          </div>

          <div className="cart-list">
            {cart.map((item) => (
              <article key={item.id} className="cart-row">
                <div className="cart-copy">
                  <strong>{item.name}</strong>
                  <p>{money(item.price)} each</p>
                </div>
                <div className="qty-group">
                  <button onClick={() => changeQty(item.id, -1)}>-</button>
                  <span>{item.quantity}</span>
                  <button onClick={() => changeQty(item.id, 1)}>+</button>
                </div>
              </article>
            ))}
          </div>

          <div className="summary-card">
            <div className="summary-row">
              <span>商品小计</span>
              <strong>{money(subtotal)}</strong>
            </div>
            <div className="summary-row discount">
              <span>会员优惠</span>
              <strong>-{money(memberDiscount)}</strong>
            </div>
            <div className="summary-row discount">
              <span>活动优惠</span>
              <strong>-{money(promotionDiscount)}</strong>
            </div>
            <div className="summary-row total">
              <span>前台待收</span>
              <strong>{money(payable)}</strong>
            </div>
            <p className="summary-note">扫码下单只提交订单，不在线支付。订单会同步到前台 POS，由 cashier 完成最终结账。</p>
          </div>

          {submitError ? <p className="submit-error">{submitError}</p> : null}

          <div className="floating-cart-bar">
            <div className="floating-cart-left">
              <span className="floating-cart-icon">🛒</span>
              <div>
                <strong>¥{payable.toFixed(1)}</strong>
                <p>{cart.reduce((sum, item) => sum + item.quantity, 0)}件商品</p>
              </div>
            </div>
            <button className="floating-cart-action" onClick={submitOrder} disabled={cart.length === 0 || submitting}>
              {submitting ? "提交中" : "选好了"}
            </button>
          </div>
        </section>
      </div>
    </div>
  );
}
