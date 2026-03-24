import { useMemo, useState } from "react";

type View =
  | "reservations"
  | "tables"
  | "transfer"
  | "ordering"
  | "review"
  | "split"
  | "payment"
  | "success";

type Category = {
  id: string;
  name: string;
  items: number;
  color: string;
};

type MenuItem = {
  id: number;
  name: string;
  category: string;
  price: number;
  image: string;
};

type OrderItem = MenuItem & {
  quantity: number;
  note?: string;
};

type TableCard = {
  id: number;
  status: "occupied" | "reserved" | "available";
  guests: number;
  area: string;
  total: number;
  course: string;
  accent: string;
};

type Reservation = {
  id: number;
  name: string;
  time: string;
  partySize: number;
  status: "checked-in" | "waiting" | "upcoming";
  area: string;
};

const categories: Category[] = [
  { id: "starters", name: "Starters", items: 7, color: "coral" },
  { id: "mains", name: "Mains", items: 8, color: "amber" },
  { id: "beer", name: "Beer & Wine", items: 12, color: "berry" },
  { id: "discounts", name: "Discounts", items: 4, color: "green" },
  { id: "drinks", name: "Drinks", items: 5, color: "blue" },
  { id: "scan", name: "Scan", items: 1, color: "sand" },
  { id: "salads", name: "Salads", items: 6, color: "red" },
  { id: "desserts", name: "Desserts", items: 5, color: "pink" }
];

const tables: TableCard[] = [
  { id: 1, status: "occupied", guests: 2, area: "Window", total: 48, course: "Starters served", accent: "sky" },
  { id: 2, status: "occupied", guests: 4, area: "Main hall", total: 92.5, course: "Mains fired", accent: "peach" },
  { id: 3, status: "available", guests: 0, area: "Main hall", total: 0, course: "Ready", accent: "mint" },
  { id: 4, status: "reserved", guests: 2, area: "Patio", total: 0, course: "19:00 hold", accent: "lilac" },
  { id: 5, status: "occupied", guests: 6, area: "Private booth", total: 168, course: "Wine pairing", accent: "gold" },
  { id: 6, status: "available", guests: 0, area: "Window", total: 0, course: "Ready", accent: "mint" },
  { id: 7, status: "occupied", guests: 3, area: "Chef counter", total: 71, course: "Desserts", accent: "rose" },
  { id: 8, status: "reserved", guests: 5, area: "Main hall", total: 0, course: "19:30 hold", accent: "lilac" },
  { id: 9, status: "occupied", guests: 4, area: "Main hall", total: 88, course: "Review bill", accent: "sky" },
  { id: 10, status: "available", guests: 0, area: "Patio", total: 0, course: "Ready", accent: "mint" },
  { id: 11, status: "occupied", guests: 2, area: "Window", total: 46, course: "Coffee service", accent: "peach" },
  { id: 12, status: "available", guests: 0, area: "Patio", total: 0, course: "Ready", accent: "mint" },
  { id: 13, status: "occupied", guests: 5, area: "Main hall", total: 124, course: "Shared plates", accent: "gold" },
  { id: 14, status: "reserved", guests: 2, area: "Chef counter", total: 0, course: "20:00 hold", accent: "lilac" },
  { id: 15, status: "available", guests: 0, area: "Main hall", total: 0, course: "Ready", accent: "mint" },
  { id: 16, status: "occupied", guests: 4, area: "Private booth", total: 136, course: "Main course", accent: "rose" },
  { id: 17, status: "available", guests: 0, area: "Window", total: 0, course: "Ready", accent: "mint" },
  { id: 18, status: "reserved", guests: 2, area: "Window", total: 0, course: "19:30 hold", accent: "lilac" },
  { id: 19, status: "occupied", guests: 3, area: "Patio", total: 63, course: "Second round drinks", accent: "peach" },
  { id: 20, status: "available", guests: 0, area: "Main hall", total: 0, course: "Ready", accent: "mint" },
  { id: 21, status: "occupied", guests: 4, area: "Main hall", total: 104, course: "Desserts", accent: "sky" },
  { id: 22, status: "reserved", guests: 6, area: "Private booth", total: 0, course: "20:15 hold", accent: "lilac" },
  { id: 23, status: "occupied", guests: 2, area: "Chef counter", total: 58, course: "Check requested", accent: "rose" },
  { id: 24, status: "occupied", guests: 4, area: "Indoor", total: 78.5, course: "Review bill", accent: "gold" }
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
    name: "Ahi Tuna Salad",
    category: "Salads",
    price: 12.5,
    image:
      "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 2,
    name: "Citrus Salad",
    category: "Salads",
    price: 8.5,
    image:
      "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 3,
    name: "Cheesecake",
    category: "Desserts",
    price: 9.5,
    image:
      "https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 4,
    name: "Chocolate Cake",
    category: "Desserts",
    price: 10.5,
    image:
      "https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 5,
    name: "Flatbread",
    category: "Starters",
    price: 9.5,
    image:
      "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 6,
    name: "Roasted Vegetables",
    category: "Starters",
    price: 11,
    image:
      "https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 7,
    name: "Miso Sea Bass",
    category: "Mains",
    price: 24,
    image:
      "https://images.unsplash.com/photo-1559847844-5315695dadae?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 8,
    name: "Yuzu Spritz",
    category: "Drinks",
    price: 7.5,
    image:
      "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=900&q=80"
  }
];

const initialOrder: OrderItem[] = [
  { ...menu[0], quantity: 1, note: "No onion" },
  { ...menu[4], quantity: 2 },
  { ...menu[7], quantity: 2, note: "Less ice" }
];

function formatMoney(value: number) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "EUR"
  }).format(value);
}

function App() {
  const [view, setView] = useState<View>("tables");
  const [activeCategory, setActiveCategory] = useState("all");
  const [search, setSearch] = useState("");
  const [selectedTable, setSelectedTable] = useState<TableCard>(tables[5]);
  const [targetTable, setTargetTable] = useState<TableCard>(tables[0]);
  const [orderItems, setOrderItems] = useState<OrderItem[]>(initialOrder);

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

  const subtotal = orderItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
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
  const perGuest = total / splitGuests;

  const addItem = (item: MenuItem) => {
    setOrderItems((current) => {
      const existing = current.find((entry) => entry.id === item.id);
      if (existing) {
        return current.map((entry) =>
          entry.id === item.id ? { ...entry, quantity: entry.quantity + 1 } : entry
        );
      }

      return [...current, { ...item, quantity: 1 }];
    });
  };

  const updateQuantity = (id: number, delta: number) => {
    setOrderItems((current) =>
      current
        .map((entry) =>
          entry.id === id ? { ...entry, quantity: Math.max(0, entry.quantity + delta) } : entry
        )
        .filter((entry) => entry.quantity > 0)
    );
  };

  const chooseTable = (table: TableCard) => {
    setSelectedTable(table);
    setView(table.status === "occupied" ? "ordering" : "review");
  };

  const statusSummary = [
    { label: "Occupied", value: tables.filter((table) => table.status === "occupied").length, tone: "sky" },
    { label: "Reserved", value: tables.filter((table) => table.status === "reserved").length, tone: "rose" },
    { label: "Available", value: tables.filter((table) => table.status === "available").length, tone: "mint" }
  ];

  const reservationSummary = [
    { label: "Checked in", value: reservations.filter((entry) => entry.status === "checked-in").length, tone: "mint" },
    { label: "Waiting", value: reservations.filter((entry) => entry.status === "waiting").length, tone: "peach" },
    { label: "Upcoming", value: reservations.filter((entry) => entry.status === "upcoming").length, tone: "lilac" }
  ];

  const availableTables = tables.filter((table) => table.status === "available");
  const getTable = (id: number) => tables.find((table) => table.id === id)!;

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
            <strong>{windowTitle}</strong>
          </header>

          <div className="persistent-nav persistent-nav-top">
            {[
              ["tables", "Table Management"],
              ["ordering", "Ordering"],
              ["review", "Order Review"],
              ["payment", "Payment"]
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
                      <strong>{tables.length} tables</strong>
                      <span>{tables.filter((table) => table.status === "occupied").length} active now</span>
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
                      {[tables.slice(0, 8), tables.slice(8, 16), tables.slice(16, 24)].map(
                        (group, index) => (
                          <div key={index} className="floorplan-island">
                            <div className="floorplan-grid">
                              {group.map((table) => {
                                const tone =
                                  table.status === "available"
                                    ? "available"
                                    : table.status === "reserved"
                                      ? "reserved"
                                      : table.total >= 100
                                        ? "attention"
                                        : table.total >= 60
                                          ? "billing"
                                          : "occupied";

                                return (
                                  <button
                                    key={table.id}
                                    className={`floor-table floor-table-${tone} ${
                                      selectedTable.id === table.id ? "floor-selected" : ""
                                    }`}
                                    onClick={() => chooseTable(table)}
                                  >
                                    <strong>T{table.id}</strong>
                                    <span>
                                      {table.status === "available"
                                        ? "Available"
                                        : table.status === "reserved"
                                          ? "Reserved"
                                          : table.total > 0
                                            ? formatMoney(table.total)
                                            : "Open"}
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
                    <strong>{formatMoney(total)}</strong>
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
                      onClick={() => setTargetTable(table)}
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
                  <div>
                    <div className="current-table-badge">
                      <span className="current-table-label">Current table</span>
                      <strong>T{selectedTable.id}</strong>
                    </div>
                    <h2>Ordering</h2>
                    <div className="heading-underline" />
                  </div>
                  <div className="utility-group">
                    <button className="utility-button">⌕</button>
                    <button className="utility-button" onClick={() => setView("review")}>
                      →
                    </button>
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
                <div className="ordering-panel-card member-card">
                  <div className="ordering-panel-head">
                    <div>
                      <p className="sidebar-title">Member</p>
                      <h2>{memberProfile.name}</h2>
                    </div>
                    <span className="cart-pill">{memberProfile.tier}</span>
                  </div>

                  <div className="member-metrics">
                    <div className="member-metric">
                      <span>Points</span>
                      <strong>{memberProfile.points}</strong>
                    </div>
                    <div className="member-metric">
                      <span>Balance</span>
                      <strong>{formatMoney(memberProfile.balance)}</strong>
                    </div>
                  </div>
                </div>

                <div className="ordering-panel-card">
                  <div className="ordering-panel-head">
                    <div>
                      <p className="sidebar-title">Current order</p>
                      <h2>Cart</h2>
                    </div>
                    <span className="cart-pill">{orderItems.length} items</span>
                  </div>

                  <div className="ordering-cart-list">
                    {orderItems.map((item) => (
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
                  <p className="sidebar-title">Summary</p>
                  <div className="amount-row">
                    <span>Subtotal</span>
                    <strong>{formatMoney(subtotal)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Tax</span>
                    <strong>{formatMoney(tax)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Service</span>
                    <strong>{formatMoney(service)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Member benefit</span>
                    <strong>-{formatMoney(memberDiscount)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Promotion hit</span>
                    <strong>-{formatMoney(promotionDiscount)}</strong>
                  </div>
                  <div className="amount-row total">
                    <span>Payable</span>
                    <strong>{formatMoney(total)}</strong>
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
                  <button className="cta-button" onClick={() => setView("review")}>
                    Review order
                  </button>
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
                  </div>
                  <button className="minor-pill" onClick={() => setView("ordering")}>
                    Add more items
                  </button>
                </div>

                <div className="guest-strip">
                  <span>{selectedTable.guests || 4} guests</span>
                  <span>{selectedTable.area}</span>
                  <span>Server Maya</span>
                  <span>{memberProfile.name}</span>
                </div>

                <div className="order-card-list">
                  {orderItems.map((item) => (
                    <article key={item.id} className="order-row-card">
                      <img src={item.image} alt={item.name} />
                      <div className="order-row-main">
                        <div>
                          <strong>{item.name}</strong>
                          <p>{item.note ?? item.category}</p>
                        </div>
                        <div className="qty-control">
                          <button onClick={() => updateQuantity(item.id, -1)}>-</button>
                          <span>{item.quantity}</span>
                          <button onClick={() => updateQuantity(item.id, 1)}>+</button>
                        </div>
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
                    <strong>{formatMoney(subtotal)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Tax</span>
                    <strong>{formatMoney(tax)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Service</span>
                    <strong>{formatMoney(service)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Member discount</span>
                    <strong>-{formatMoney(memberDiscount)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Full reduction</span>
                    <strong>-{formatMoney(promotionDiscount)}</strong>
                  </div>
                  <div className="amount-row total">
                    <span>Payable</span>
                    <strong>{formatMoney(total)}</strong>
                  </div>
                </div>

                <div className="payment-card">
                  <p className="sidebar-title">Actions</p>
                  <button className="sort-row">
                    <span className="sort-icon" />
                    <span>Member recharge</span>
                  </button>
                  <button className="sort-row">
                    <span className="sort-icon" />
                    <span>Send to kitchen</span>
                  </button>
                  <button className="sort-row" onClick={() => setView("split")}>
                    <span className="sort-icon" />
                    <span>Split bill</span>
                  </button>
                  <button className="sort-row" onClick={() => setView("payment")}>
                    <span className="sort-icon" />
                    <span>Proceed to payment</span>
                  </button>
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
                  <button className="minor-pill" onClick={() => setView("review")}>
                    Back to review
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
                        {orderItems.slice(index % 2, index % 2 === 0 ? 2 : 3).map((item) => (
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
                    <h2>Payment collection</h2>
                  </div>
                  <button className="minor-pill" onClick={() => setView("review")}>
                    Back to review
                  </button>
                </div>

                <div className="payment-hero">
                  <div className="payment-card feature-card">
                    <p className="sidebar-title">Ready to charge</p>
                    <h3>{formatMoney(total)}</h3>
                    <p className="accent-copy">
                      The bill is confirmed and the terminal is paired. Tap to charge or hand the
                      countertop reader to the guest.
                    </p>
                  </div>
                </div>

                <div className="payment-card settlement-breakdown">
                  <div className="amount-row">
                    <span>Original amount</span>
                    <strong>{formatMoney(grossTotal)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Member benefit</span>
                    <strong>-{formatMoney(memberDiscount)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Promotion benefit</span>
                    <strong>-{formatMoney(promotionDiscount)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Gift item</span>
                    <strong>Peach Soda</strong>
                  </div>
                </div>

                <div className="order-card-list compact-list">
                  {orderItems.map((item) => (
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
                    <strong>{formatMoney(subtotal)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Tax</span>
                    <strong>{formatMoney(tax)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Service</span>
                    <strong>{formatMoney(service)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Member benefit</span>
                    <strong>-{formatMoney(memberDiscount)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Promotion benefit</span>
                    <strong>-{formatMoney(promotionDiscount)}</strong>
                  </div>
                  <div className="amount-row total">
                    <span>Total due</span>
                    <strong>{formatMoney(total)}</strong>
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
                  <h3>{formatMoney(total)}</h3>
                  <p className="accent-copy">
                    Card terminal is connected. Once collected, the table can be marked ready for
                    turnover from the floor panel.
                  </p>
                  <button className="cta-button" onClick={() => setView("success")}>
                    Collect payment
                  </button>
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
                    <strong>{formatMoney(total)}</strong>
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
                  <button className="cta-button" onClick={() => setView("reservations")}>
                    Back to host stand
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
              ["split", "Split Bill"],
              ["success", "Payment Success"]
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
