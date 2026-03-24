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
  memberPrice?: number;
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
  source?: "POS" | "QR";
  settlementState?: "PENDING_SETTLEMENT" | "IN_SERVICE" | "READY" | "SETTLED";
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

type QrCurrentOrderResponse = {
  orderNo: string;
  queueNo: string;
  tableCode: string;
  settlementStatus: "PENDING_SETTLEMENT" | "IN_SERVICE" | "READY" | "SETTLED";
  memberName: string | null;
  memberTier?: string | null;
  originalAmountCents?: number;
  memberDiscountCents?: number;
  promotionDiscountCents?: number;
  payableAmountCents: number;
  items: Array<{
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
  { id: 1, status: "occupied", guests: 2, area: "Window", total: 48, course: "Starters served", accent: "sky" },
  { id: 2, status: "occupied", guests: 4, area: "Main hall", total: 92.5, course: "QR order waiting for cashier", accent: "peach", source: "QR", settlementState: "PENDING_SETTLEMENT", memberName: "Lina Chen" },
  { id: 3, status: "available", guests: 0, area: "Main hall", total: 0, course: "Ready", accent: "mint" },
  { id: 4, status: "reserved", guests: 2, area: "Patio", total: 0, course: "19:00 hold", accent: "lilac" },
  { id: 5, status: "occupied", guests: 6, area: "Private booth", total: 168, course: "Wine pairing", accent: "gold" },
  { id: 6, status: "available", guests: 0, area: "Window", total: 0, course: "Ready", accent: "mint" },
  { id: 7, status: "occupied", guests: 3, area: "Chef counter", total: 71, course: "QR guest sent add-on items", accent: "rose", source: "QR", settlementState: "PENDING_SETTLEMENT" },
  { id: 8, status: "reserved", guests: 5, area: "Main hall", total: 0, course: "19:30 hold", accent: "lilac" },
  { id: 9, status: "occupied", guests: 4, area: "Main hall", total: 88, course: "Review bill", accent: "sky" },
  { id: 10, status: "available", guests: 0, area: "Patio", total: 0, course: "Ready", accent: "mint" },
  { id: 11, status: "occupied", guests: 2, area: "Window", total: 46, course: "Coffee service", accent: "peach" },
  { id: 12, status: "available", guests: 0, area: "Patio", total: 0, course: "Ready", accent: "mint" },
  { id: 13, status: "occupied", guests: 5, area: "Main hall", total: 124, course: "Shared plates", accent: "gold", source: "QR", settlementState: "READY", memberName: "Gold Member" },
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
    name: "招牌炒饭",
    category: "meals",
    price: 18,
    memberPrice: 16,
    image:
      "https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 2,
    name: "黑椒牛肉饭",
    category: "meals",
    price: 34,
    memberPrice: 31,
    image:
      "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 3,
    name: "酥脆鸡块",
    category: "snacks",
    price: 16,
    image:
      "https://images.unsplash.com/photo-1562967916-eb82221dfb92?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 4,
    name: "白桃气泡饮",
    category: "drinks",
    price: 12,
    memberPrice: 10,
    image:
      "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 5,
    name: "黑糖奶茶",
    category: "drinks",
    price: 14,
    memberPrice: 12,
    image:
      "https://images.unsplash.com/photo-1558857563-b371033873b8?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 6,
    name: "芒果布丁",
    category: "desserts",
    price: 15,
    image:
      "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 7,
    name: "主厨套餐",
    category: "popular",
    price: 46,
    memberPrice: 42,
    image:
      "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=900&q=80"
  },
  {
    id: 8,
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

function formatMoney(value: number) {
  return new Intl.NumberFormat("zh-CN", {
    style: "currency",
    currency: "CNY"
  }).format(value);
}

function App() {
  const [view, setView] = useState<View>("tables");
  const [activeCategory, setActiveCategory] = useState("all");
  const [search, setSearch] = useState("");
  const [tableState, setTableState] = useState<TableCard[]>(tables);
  const [selectedTableId, setSelectedTableId] = useState<number>(6);
  const [targetTableId, setTargetTableId] = useState<number>(1);
  const [orderItems, setOrderItems] = useState<OrderItem[]>(initialOrder);
  const [qrOrders, setQrOrders] = useState<Record<string, QrCurrentOrderResponse>>({});
  const [recentQrAlert, setRecentQrAlert] = useState<QrCurrentOrderResponse | null>(null);
  const seenQrOrderNos = useRef<Set<string>>(new Set());

  const selectedTable = tableState.find((table) => table.id === selectedTableId) ?? tableState[0];
  const targetTable = tableState.find((table) => table.id === targetTableId) ?? tableState[0];
  const selectedTableCode = `T${selectedTable.id}`;
  const selectedQrOrder = qrOrders[selectedTableCode];
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
  const qrPendingTables = tableState.filter((table) => table.source === "QR" && table.status === "occupied");

  useEffect(() => {
    const qrTableCodes = tables.map((table) => `T${table.id}`);
    let active = true;

    const syncQrTables = async () => {
      try {
        const results = await Promise.all(
          qrTableCodes.map(async (tableCode) => {
            const response = await fetch(
              `/api/v1/orders/qr-current?storeCode=1001&tableCode=${encodeURIComponent(tableCode)}`
            );

            if (!response.ok) {
              return null;
            }

            const payload = (await response.json()) as ApiResponse<QrCurrentOrderResponse | null>;
            return payload.code === 0 ? payload.data : null;
          })
        );

        if (!active) {
          return;
        }

        const nextQrOrders = Object.fromEntries(
          results.filter((entry): entry is QrCurrentOrderResponse => Boolean(entry)).map((entry) => [entry.tableCode, entry])
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
              source: "QR",
              settlementState: match.settlementStatus,
              memberName: match.memberName ?? undefined,
              total: Number((match.payableAmountCents / 100).toFixed(2)),
              course: `${match.items.length} items waiting for cashier`
            };
          })
        );
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

  const displayedOrderItems = selectedQrOrder
    ? selectedQrOrder.items.map((item, index) => {
        const source = menuByName.get(item.productName);
        const unitPrice =
          (item.memberPriceCents ?? item.unitPriceCents ?? Math.round((selectedQrOrder.payableAmountCents / Math.max(selectedQrOrder.items.length, 1)))) / 100;

        return {
          id: source?.id ?? 9000 + index,
          name: item.productName,
          category: source?.category ?? "qr",
          price: unitPrice,
          memberPrice: item.memberPriceCents ? item.memberPriceCents / 100 : source?.memberPrice,
          image: source?.image ?? "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=900&q=80",
          quantity: item.quantity,
          note: "桌码点单"
        } satisfies OrderItem;
      })
    : orderItems;

  const displayedSubtotal = selectedQrOrder
    ? (selectedQrOrder.originalAmountCents ?? selectedQrOrder.payableAmountCents) / 100
    : subtotal;
  const displayedMemberDiscount = selectedQrOrder
    ? (selectedQrOrder.memberDiscountCents ?? 0) / 100
    : memberDiscount;
  const displayedPromotionDiscount = selectedQrOrder
    ? (selectedQrOrder.promotionDiscountCents ?? 0) / 100
    : promotionDiscount;
  const displayedTax = selectedQrOrder ? 0 : tax;
  const displayedService = selectedQrOrder ? 0 : service;
  const displayedGrossTotal = selectedQrOrder ? displayedSubtotal : grossTotal;
  const displayedTotal = selectedQrOrder ? selectedQrOrder.payableAmountCents / 100 : total;
  const perGuest = displayedTotal / splitGuests;

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
    if (selectedQrOrder) {
      const nextItems = displayedOrderItems
        .map((entry) =>
          entry.id === id ? { ...entry, quantity: Math.max(0, entry.quantity + delta) } : entry
        )
        .filter((entry) => entry.quantity > 0);

      const nextQrOrder: QrCurrentOrderResponse = {
        ...selectedQrOrder,
        items: nextItems.map((item) => ({
          productName: item.name,
          quantity: item.quantity,
          unitPriceCents: Math.round(item.price * 100),
          memberPriceCents: item.memberPrice ? Math.round(item.memberPrice * 100) : null
        }))
      };

      const originalAmountCents = nextQrOrder.items.reduce(
        (sum, item) => sum + (item.unitPriceCents ?? 0) * item.quantity,
        0
      );
      const memberDiscountCents = nextQrOrder.items.reduce((sum, item) => {
        const unitPrice = item.unitPriceCents ?? 0;
        const memberPrice = item.memberPriceCents ?? unitPrice;
        return sum + Math.max(0, unitPrice - memberPrice) * item.quantity;
      }, 0);
      const promotionDiscountCents = originalAmountCents >= 6000 ? 800 : 0;

      nextQrOrder.originalAmountCents = originalAmountCents;
      nextQrOrder.memberDiscountCents = memberDiscountCents;
      nextQrOrder.promotionDiscountCents = promotionDiscountCents;
      nextQrOrder.payableAmountCents = Math.max(0, originalAmountCents - memberDiscountCents - promotionDiscountCents);

      setQrOrders((current) => ({
        ...current,
        [selectedTableCode]: nextQrOrder
      }));
      setTableState((current) =>
        current.map((table) =>
          table.id === selectedTable.id
            ? {
                ...table,
                total: Number((nextQrOrder.payableAmountCents / 100).toFixed(2)),
                course: `${nextQrOrder.items.length} items waiting for cashier`
              }
            : table
        )
      );

      void fetch("/api/v1/orders/qr-current", {
        method: "PUT",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          storeCode: "1001",
          tableCode: selectedTableCode,
          items: nextQrOrder.items
        })
      });

      return;
    }

    setOrderItems((current) =>
      current
        .map((entry) =>
          entry.id === id ? { ...entry, quantity: Math.max(0, entry.quantity + delta) } : entry
        )
        .filter((entry) => entry.quantity > 0)
    );
  };

  const chooseTable = (table: TableCard) => {
    setSelectedTableId(table.id);
    setView(table.status === "occupied" && table.source === "QR" ? "review" : table.status === "occupied" ? "ordering" : "review");
  };

  const completeSettlement = async () => {
    if (selectedQrOrder) {
      await fetch("/api/v1/orders/qr-settle", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          storeCode: "1001",
          tableCode: selectedTableCode
        })
      });

      setQrOrders((current) => {
        const next = { ...current };
        delete next[selectedTableCode];
        return next;
      });
      setTableState((current) =>
        current.map((table) =>
          table.id === selectedTable.id
            ? {
                ...tables.find((baseTable) => baseTable.id === table.id)!,
                source: undefined,
                settlementState: undefined,
                memberName: undefined
              }
            : table
        )
      );
    }

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
                    `http://localhost:4177/?storeName=Riverside%20Branch&storeCode=1001&table=${encodeURIComponent(selectedTableCode)}`,
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
                  {recentQrAlert.tableCode} · {recentQrAlert.queueNo}
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
                    setView("review");
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
                                    {table.source === "QR" ? <span className="floor-source-badge">QR</span> : null}
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
                  <span>{selectedTable.source === "QR" ? "QR table order" : "Server Maya"}</span>
                  <span>{selectedQrOrder?.memberName ?? selectedTable.memberName ?? memberProfile.name}</span>
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
                      {selectedTable.settlementState ?? "PENDING_SETTLEMENT"}
                    </span>
                  </div>
                ) : null}

                <div className="order-card-list">
                  {displayedOrderItems.map((item) => (
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
                    <h2>Payment collection</h2>
                  </div>
                  <button className="minor-pill" onClick={() => setView("review")}>
                    Back to review
                  </button>
                </div>

                <div className="payment-hero">
                  <div className="payment-card feature-card">
                    <p className="sidebar-title">Ready to charge</p>
                    <h3>{formatMoney(displayedTotal)}</h3>
                    <p className="accent-copy">
                      {selectedTable.source === "QR"
                        ? "A QR table order is ready for cashier settlement. Confirm the synced discounts and collect at the counter or table."
                        : "The bill is confirmed and the terminal is paired. Tap to charge or hand the countertop reader to the guest."}
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
                    <strong>{formatMoney(displayedGrossTotal)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Member benefit</span>
                    <strong>-{formatMoney(displayedMemberDiscount)}</strong>
                  </div>
                  <div className="amount-row discount-row">
                    <span>Promotion benefit</span>
                    <strong>-{formatMoney(displayedPromotionDiscount)}</strong>
                  </div>
                  <div className="amount-row">
                    <span>Gift item</span>
                    <strong>Peach Soda</strong>
                  </div>
                </div>

                <div className="order-card-list compact-list">
                  {displayedOrderItems.map((item) => (
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
                    <span>Promotion benefit</span>
                    <strong>-{formatMoney(displayedPromotionDiscount)}</strong>
                  </div>
                  <div className="amount-row total">
                    <span>Total due</span>
                    <strong>{formatMoney(displayedTotal)}</strong>
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
                  <h3>{formatMoney(displayedTotal)}</h3>
                  <p className="accent-copy">
                    Card terminal is connected. Once collected, the table can be marked ready for
                    turnover from the floor panel.
                  </p>
                  <button className="cta-button" onClick={() => void completeSettlement()}>
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
