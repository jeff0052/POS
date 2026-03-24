import { useEffect, useMemo, useState } from "react";

type Category = "Popular" | "Meals" | "Snacks" | "Drinks" | "Desserts";

type CategoryMeta = {
  id: Category;
  label: string;
  icon: string;
};

type MenuItem = {
  id: number;
  name: string;
  category: Category;
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
  {
    id: 1,
    name: "招牌炒饭",
    category: "Meals",
    price: 18,
    memberPrice: 16,
    description: "现炒蛋香粒粒分明",
    sales: 385,
    likes: 721,
    image: "https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: 2,
    name: "黑椒牛肉饭",
    category: "Meals",
    price: 34,
    memberPrice: 31,
    description: "现煎牛肉配黑椒酱汁",
    spicy: true,
    sales: 265,
    likes: 713,
    image: "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: 3,
    name: "酥脆鸡块",
    category: "Snacks",
    price: 16,
    description: "外酥里嫩搭配蘸酱",
    sales: 186,
    likes: 488,
    image: "https://images.unsplash.com/photo-1562967916-eb82221dfb92?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: 4,
    name: "白桃气泡饮",
    category: "Drinks",
    price: 12,
    memberPrice: 10,
    description: "轻甜爽口店内热卖",
    sales: 482,
    likes: 999,
    image: "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: 5,
    name: "黑糖奶茶",
    category: "Drinks",
    price: 14,
    memberPrice: 12,
    description: "经典奶香与黑糖风味",
    sales: 356,
    likes: 875,
    image: "https://images.unsplash.com/photo-1558857563-b371033873b8?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: 6,
    name: "芒果布丁",
    category: "Desserts",
    price: 15,
    description: "清爽收尾甜品",
    sales: 144,
    likes: 402,
    image: "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: 7,
    name: "主厨套餐",
    category: "Popular",
    price: 46,
    memberPrice: 42,
    description: "主食+小食+饮品组合",
    sales: 517,
    likes: 1104,
    image: "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=600&q=80"
  },
  {
    id: 8,
    name: "松露薯条",
    category: "Snacks",
    price: 19,
    description: "薯条香脆带松露风味",
    sales: 199,
    likes: 530,
    image: "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?auto=format&fit=crop&w=600&q=80"
  }
];

const categories: CategoryMeta[] = [
  { id: "Popular", label: "新品", icon: "🟢" },
  { id: "Meals", label: "热销饭类", icon: "🍱" },
  { id: "Snacks", label: "现炸小食", icon: "🍟" },
  { id: "Drinks", label: "饮品甜水", icon: "🥤" },
  { id: "Desserts", label: "甜点轻食", icon: "🧁" }
];

function money(value: number) {
  return new Intl.NumberFormat("zh-CN", {
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
          <p className="eyebrow">
            {submitResult.storeName} · 桌号 {submitResult.tableCode}
          </p>
          <h1>下单成功</h1>
          <p className="hero-copy">
            菜品已经发送到前台收银队列，门店员工会确认订单并在结账时完成收款。
          </p>

          <div className="success-cards">
            <article className="metric-card">
              <span>取号队列</span>
              <strong>{submitResult.queueNo}</strong>
            </article>
            <article className="metric-card">
              <span>待收金额</span>
              <strong>{money(submitResult.payableAmountCents / 100)}</strong>
            </article>
            <article className="metric-card">
              <span>会员身份</span>
              <strong>{submitResult.memberTier ? `${submitResult.memberTier} 会员` : "散客"}</strong>
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

        <section className="member-banner">
          <div>
            <p className="eyebrow">会员权益</p>
            <h2>{memberBound ? "Lina Chen · Gold" : "绑定会员"}</h2>
            <p>{memberBound ? "积分 2860 · 储值 ¥320.00 · 已享会员价" : "绑定手机号后可享会员价、积分累计和储值余额。"}</p>
          </div>
          <button className="soft-button" onClick={() => setMemberBound((current) => !current)}>
            {memberBound ? "切换散客" : "绑定会员"}
          </button>
        </section>

        {currentOrder ? (
          <section className="current-order-banner">
            <div>
              <p className="eyebrow">当前桌台订单</p>
              <h2>{currentOrder.queueNo}</h2>
              <p>
                {currentOrder.items.length} 件商品 · 待前台结账 · 待收 {money(currentOrder.payableAmountCents / 100)}
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
                    <strong>¥{(memberBound && item.memberPrice ? item.memberPrice : item.price).toFixed(0)}</strong>
                    {memberBound && item.memberPrice ? <span>¥{item.price.toFixed(0)}</span> : null}
                  </div>
                </div>
                <button className="floating-add-button" onClick={() => addItem(item)}>
                  +
                </button>
              </article>
            ))}
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
                  <p>
                    {money(memberBound && item.memberPrice ? item.memberPrice : item.price)} each
                    {memberBound && item.memberPrice ? " · 会员价" : ""}
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
