import { useEffect, useMemo, useState } from "react";

type Category = "Popular" | "Meals" | "Snacks" | "Drinks" | "Desserts";

type MenuItem = {
  id: number;
  name: string;
  category: Category;
  price: number;
  memberPrice?: number;
  description: string;
  spicy?: boolean;
};

type CartItem = MenuItem & {
  quantity: number;
};

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

type QrSubmitResponse = {
  orderNo: string;
  queueNo: string;
  storeCode: string;
  storeName: string;
  tableCode: string;
  orderType: string;
  settlementStatus: string;
  memberName: string | null;
  memberTier: string | null;
  originalAmountCents: number;
  memberDiscountCents: number;
  promotionDiscountCents: number;
  payableAmountCents: number;
};

type QrCurrentOrderResponse = {
  orderNo: string;
  queueNo: string;
  storeCode: string;
  storeName: string;
  tableCode: string;
  settlementStatus: string;
  memberName: string | null;
  memberTier: string | null;
  originalAmountCents: number;
  memberDiscountCents: number;
  promotionDiscountCents: number;
  payableAmountCents: number;
  items: Array<{
    productId: number | null;
    productName: string;
    quantity: number;
    unitPriceCents: number;
    memberPriceCents: number | null;
  }>;
};

const menu: MenuItem[] = [
  { id: 1, name: "Signature Fried Rice", category: "Meals", price: 18, memberPrice: 16, description: "Wok fried rice with egg and scallion" },
  { id: 2, name: "Black Pepper Beef", category: "Meals", price: 34, memberPrice: 31, description: "Tender beef with pepper glaze", spicy: true },
  { id: 3, name: "Crispy Chicken Bites", category: "Snacks", price: 16, description: "Golden fried chicken with dip" },
  { id: 4, name: "Peach Soda", category: "Drinks", price: 12, memberPrice: 10, description: "Sparkling house soda" },
  { id: 5, name: "Milk Tea", category: "Drinks", price: 14, memberPrice: 12, description: "Classic brown sugar milk tea" },
  { id: 6, name: "Mango Pudding", category: "Desserts", price: 15, description: "Fresh mango with cream" },
  { id: 7, name: "Chef Combo", category: "Popular", price: 46, memberPrice: 42, description: "Meal set with drink and side" },
  { id: 8, name: "Truffle Fries", category: "Snacks", price: 19, description: "Crispy fries with truffle seasoning" }
];

const categories: Category[] = ["Popular", "Meals", "Snacks", "Drinks", "Desserts"];

function money(value: number) {
  return new Intl.NumberFormat("en-SG", {
    style: "currency",
    currency: "CNY",
    minimumFractionDigits: 2
  }).format(value);
}

export default function App() {
  const params = new URLSearchParams(window.location.search);
  const storeName = params.get("storeName") ?? "Riverside Branch";
  const storeCode = params.get("storeCode") ?? "1001";
  const tableCode = params.get("table") ?? "T12";
  const [activeCategory, setActiveCategory] = useState<Category>("Popular");
  const [keyword, setKeyword] = useState("");
  const [cart, setCart] = useState<CartItem[]>([
    { ...menu[0], quantity: 1 },
    { ...menu[3], quantity: 2 }
  ]);
  const [memberBound, setMemberBound] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [submitResult, setSubmitResult] = useState<QrSubmitResponse | null>(null);
  const [currentOrder, setCurrentOrder] = useState<QrCurrentOrderResponse | null>(null);

  const visibleMenu = useMemo(
    () =>
      menu.filter((item) => {
        const matchCategory = activeCategory === "Popular" ? true : item.category === activeCategory;
        const matchKeyword =
          keyword.trim() === "" || `${item.name} ${item.description}`.toLowerCase().includes(keyword.toLowerCase());
        return matchCategory && matchKeyword;
      }),
    [activeCategory, keyword]
  );

  const subtotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const memberDiscount = memberBound
    ? cart.reduce((sum, item) => sum + Math.max(0, item.price - (item.memberPrice ?? item.price)) * item.quantity, 0)
    : 0;
  const promotionDiscount = subtotal >= 60 ? 8 : 0;
  const payable = subtotal - memberDiscount - promotionDiscount;

  useEffect(() => {
    const controller = new AbortController();

    const loadCurrentOrder = async () => {
      try {
        const response = await fetch(
          `/api/v1/orders/qr-current?storeCode=${encodeURIComponent(storeCode)}&tableCode=${encodeURIComponent(tableCode)}`,
          { signal: controller.signal }
        );

        if (!response.ok) {
          return;
        }

        const payload = (await response.json()) as ApiResponse<QrCurrentOrderResponse | null>;
        if (payload.code === 0) {
          setCurrentOrder(payload.data ?? null);
        }
      } catch (error) {
        if (error instanceof DOMException && error.name === "AbortError") {
          return;
        }
      }
    };

    void loadCurrentOrder();

    return () => controller.abort();
  }, [storeCode, tableCode]);

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

    setSubmitting(true);
    setSubmitError("");

    try {
      const response = await fetch("/api/v1/orders/qr-submit", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          storeCode,
          storeName,
          tableCode,
          memberBound,
          memberName: memberBound ? "Lina Chen" : null,
          memberTier: memberBound ? "Gold" : null,
          memberPhone: memberBound ? "13800000001" : null,
          items: cart.map((item) => ({
            productId: item.id,
            productName: item.name,
            quantity: item.quantity,
            unitPriceCents: Math.round(item.price * 100),
            memberPriceCents: item.memberPrice ? Math.round(item.memberPrice * 100) : null
          }))
        })
      });

      if (!response.ok) {
        throw new Error(`Request failed with status ${response.status}`);
      }

      const payload = (await response.json()) as ApiResponse<QrSubmitResponse>;
      if (payload.code !== 0 || !payload.data) {
        throw new Error(payload.message || "Unable to submit order");
      }

      setSubmitResult(payload.data);
      setCurrentOrder({
        orderNo: payload.data.orderNo,
        queueNo: payload.data.queueNo,
        storeCode: payload.data.storeCode,
        storeName: payload.data.storeName,
        tableCode: payload.data.tableCode,
        settlementStatus: payload.data.settlementStatus,
        memberName: payload.data.memberName,
        memberTier: payload.data.memberTier,
        originalAmountCents: payload.data.originalAmountCents,
        memberDiscountCents: payload.data.memberDiscountCents,
        promotionDiscountCents: payload.data.promotionDiscountCents,
        payableAmountCents: payload.data.payableAmountCents,
        items: cart.map((item) => ({
          productId: item.id,
          productName: item.name,
          quantity: item.quantity,
          unitPriceCents: Math.round(item.price * 100),
          memberPriceCents: item.memberPrice ? Math.round(item.memberPrice * 100) : null
        }))
      });
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
          <p className="eyebrow">{submitResult.storeName} · Table {submitResult.tableCode}</p>
          <h1>Order submitted</h1>
          <p className="hero-copy">
            Your order has been sent to the cashier queue. Store staff will confirm items and settle at the table or counter.
          </p>

          <div className="success-cards">
            <article className="metric-card">
              <span>Queue No</span>
              <strong>{submitResult.queueNo}</strong>
            </article>
            <article className="metric-card">
              <span>Payable</span>
              <strong>{money(submitResult.payableAmountCents / 100)}</strong>
            </article>
            <article className="metric-card">
              <span>Member</span>
              <strong>{submitResult.memberTier ? `${submitResult.memberTier} Member` : "Guest"}</strong>
            </article>
          </div>

          <button className="primary-button" onClick={() => setSubmitResult(null)}>
            Back to menu
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
            <div>
              <p className="eyebrow">Scanned table code</p>
              <h1>{storeName}</h1>
            </div>
            <span className="table-chip">{tableCode}</span>
          </div>
          <p className="hero-copy">
            Dine-in QR ordering. Your order will be synced to the restaurant POS and settled at checkout.
          </p>

          <div className="hero-pills">
            <span>Store {storeCode}</span>
            <span>Dining · QR Order</span>
            <span>{memberBound ? "Member linked" : "Guest order"}</span>
          </div>
        </header>

        <section className="member-banner">
          <div>
            <p className="eyebrow">CRM Member</p>
            <h2>{memberBound ? "Lina Chen · Gold" : "Bind membership"}</h2>
            <p>{memberBound ? "Points 2,860 · Balance CNY 320.00 · Member pricing enabled" : "Link a phone number to unlock member price, points and recharge balance."}</p>
          </div>
          <button className="soft-button" onClick={() => setMemberBound((current) => !current)}>
            {memberBound ? "Switch to Guest" : "Bind Member"}
          </button>
        </section>

        {currentOrder ? (
          <section className="current-order-banner">
            <div>
              <p className="eyebrow">Current table order</p>
              <h2>{currentOrder.queueNo}</h2>
              <p>
                {currentOrder.items.length} items · Pending settlement · Payable {money(currentOrder.payableAmountCents / 100)}
              </p>
            </div>
            <span className="table-order-state">Open</span>
          </section>
        ) : null}

        <section className="category-row">
          {categories.map((category) => (
            <button
              key={category}
              className={`category-pill ${activeCategory === category ? "category-pill-active" : ""}`}
              onClick={() => setActiveCategory(category)}
            >
              {category}
            </button>
          ))}
        </section>

        <section className="search-row">
          <input
            aria-label="Search menu"
            placeholder="Search dishes"
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
          />
        </section>

        <section className="menu-list">
          {visibleMenu.map((item) => (
            <article key={item.id} className="menu-card">
              <div className="menu-copy">
                <div className="menu-title-row">
                  <h3>{item.name}</h3>
                  {item.spicy ? <span className="flag">Spicy</span> : null}
                </div>
                <p>{item.description}</p>
                <div className="price-row">
                  <strong>{money(memberBound && item.memberPrice ? item.memberPrice : item.price)}</strong>
                  {memberBound && item.memberPrice ? <span>{money(item.price)}</span> : null}
                </div>
              </div>
              <button className="add-button" onClick={() => addItem(item)}>
                Add
              </button>
            </article>
          ))}
        </section>

        <section className="cart-panel">
          <div className="section-head">
            <div>
              <p className="eyebrow">Current table order</p>
              <h2>Cart</h2>
            </div>
            <span className="cart-count">{cart.length} items</span>
          </div>

          <div className="cart-list">
            {cart.map((item) => (
              <article key={item.id} className="cart-row">
                <div className="cart-copy">
                  <strong>{item.name}</strong>
                  <p>
                    {money(memberBound && item.memberPrice ? item.memberPrice : item.price)} each
                    {memberBound && item.memberPrice ? " · member price" : ""}
                  </p>
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
              <span>Original subtotal</span>
              <strong>{money(subtotal)}</strong>
            </div>
            <div className="summary-row discount">
              <span>Member benefit</span>
              <strong>-{money(memberDiscount)}</strong>
            </div>
            <div className="summary-row discount">
              <span>Promotion hit</span>
              <strong>-{money(promotionDiscount)}</strong>
            </div>
            <div className="summary-row total">
              <span>Payable at cashier</span>
              <strong>{money(payable)}</strong>
            </div>
            <p className="summary-note">This QR order will sync to the POS as a dine-in table order. Payment is completed at checkout.</p>
          </div>

          {submitError ? <p className="submit-error">{submitError}</p> : null}

          <button className="primary-button" onClick={submitOrder} disabled={cart.length === 0 || submitting}>
            {submitting ? "Submitting..." : "Submit to cashier"}
          </button>
        </section>
      </div>
    </div>
  );
}
