import { Alert, Card, DatePicker, Input, Select, Space, Table, Tag, Typography } from "antd";
import { useMemo, useState } from "react";
import { getOrders } from "../../api/services/orderService";
import { useAsyncData } from "../../hooks/useAsyncData";
import type { Order } from "../../types";
import { OrderDetailDrawer } from "./components/OrderDetailDrawer";

function getOrderStatusMeta(status: Order["status"]) {
  switch (status) {
    case "DRAFT":
      return { label: "Draft", color: "default" };
    case "SUBMITTED":
      return { label: "Sent to Kitchen", color: "processing" };
    case "PENDING_SETTLEMENT":
      return { label: "Pending Settlement", color: "gold" };
    case "PAID":
      return { label: "Paid", color: "green" };
    case "REFUNDED":
      return { label: "Refunded", color: "red" };
    default:
      return { label: status, color: "default" };
  }
}

const columns = [
  { title: "Order No", dataIndex: "orderNo", key: "orderNo" },
  { title: "Table", dataIndex: "tableCode", key: "tableCode" },
  {
    title: "Source",
    dataIndex: "orderType",
    key: "orderType",
    render: (value: string) => <Tag color={value === "QR" ? "geekblue" : "default"}>{value ?? "POS"}</Tag>
  },
  { title: "Amount", dataIndex: "amount", key: "amount" },
  {
    title: "Status",
    dataIndex: "status",
    key: "status",
    render: (value: string) => {
      const meta = getOrderStatusMeta(value as Order["status"]);
      return <Tag color={meta.color}>{meta.label}</Tag>;
    }
  },
  { title: "Member", dataIndex: "memberName", key: "memberName" },
  { title: "Payment", dataIndex: "payment", key: "payment" },
  { title: "Time", dataIndex: "time", key: "time" }
];

export function OrdersPage() {
  const [keyword, setKeyword] = useState("");
  const [selectedStatus, setSelectedStatus] = useState<string | undefined>();
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const query = useAsyncData(getOrders);

  const data = useMemo(() => {
    let list = query.data ?? [];
    if (keyword.trim()) {
      list = list.filter((item) => item.orderNo.toLowerCase().includes(keyword.toLowerCase()));
    }
    if (selectedStatus) {
      list = list.filter((item) => item.status === selectedStatus);
    }
    return list;
  }, [query.data, keyword, selectedStatus]);

  return (
    <div className="page-shell">
      <Typography.Title className="page-title" level={2}>
        Orders
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        订单查询、支付状态、打印状态和退款排查的核心页面。
      </Typography.Paragraph>

      <Card>
        <Space wrap>
          <Input
            placeholder="Order No"
            style={{ width: 220 }}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
          />
          <Select
            placeholder="Status"
            style={{ width: 180 }}
            allowClear
            onChange={(value) => setSelectedStatus(value)}
            options={[
              { label: "Draft", value: "DRAFT" },
              { label: "Sent to Kitchen", value: "SUBMITTED" },
              { label: "Pending Settlement", value: "PENDING_SETTLEMENT" },
              { label: "Paid", value: "PAID" },
              { label: "Refunded", value: "REFUNDED" }
            ]}
          />
          <DatePicker.RangePicker />
        </Space>
        {query.error ? <Alert type="error" message={query.error} style={{ marginTop: 16 }} /> : null}

        <Table
          style={{ marginTop: 16 }}
          rowKey="orderNo"
          columns={columns}
          loading={query.loading}
          dataSource={data}
          locale={{ emptyText: "No orders yet" }}
          onRow={(record) => ({
            onClick: () => setSelectedOrder(record)
          })}
        />
      </Card>
      <OrderDetailDrawer
        order={selectedOrder}
        open={Boolean(selectedOrder)}
        onClose={() => setSelectedOrder(null)}
      />
    </div>
  );
}
