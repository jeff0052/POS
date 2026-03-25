import { Alert, Card, Col, Row, Skeleton, Table, Typography } from "antd";
import { getDashboardSummary, getRecentOrders } from "../../api/services/dashboardService";
import { useAsyncData } from "../../hooks/useAsyncData";

const columns = [
  { title: "Order No", dataIndex: "orderNo", key: "orderNo" },
  { title: "Amount", dataIndex: "amount", key: "amount" },
  { title: "Status", dataIndex: "status", key: "status" }
];

export function DashboardPage() {
  const summaryQuery = useAsyncData(getDashboardSummary);
  const ordersQuery = useAsyncData(getRecentOrders);

  const summary = summaryQuery.data
    ? [
        { title: "Today Revenue", value: summaryQuery.data.revenue },
        { title: "Orders", value: summaryQuery.data.orders },
        { title: "Refund Amount", value: summaryQuery.data.refunds },
        { title: "Active Cashiers", value: summaryQuery.data.cashiers }
      ]
    : [];

  return (
    <div className="page-shell">
      <Typography.Title className="page-title" level={2}>
        Dashboard
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        商户每天最先看的经营总览页。
      </Typography.Paragraph>

      {summaryQuery.error ? <Alert type="error" message={summaryQuery.error} /> : null}
      {summaryQuery.loading ? (
        <Skeleton active paragraph={{ rows: 4 }} />
      ) : (
        <Row gutter={[16, 16]}>
          {summary.map((item) => (
            <Col span={6} key={item.title}>
              <Card>
                <Typography.Text type="secondary">{item.title}</Typography.Text>
                <Typography.Title level={3} style={{ marginTop: 12, marginBottom: 0 }}>
                  {item.value}
                </Typography.Title>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      <div className="split-panels">
        <Card title="Sales Curve" extra="Today" bodyStyle={{ padding: 16 }}>
          <div className="mini-chart" />
        </Card>
        <Card title="Payment Mix" extra="Preview">
          <Typography.Paragraph style={{ marginBottom: 8 }}>
            Card terminal and QR gateway payments dominate today, while cash remains the secondary channel.
          </Typography.Paragraph>
          <Typography.Title level={3} style={{ color: "#0b6e4f", marginBottom: 8 }}>
            83%
          </Typography.Title>
          <Typography.Text type="secondary">Digital payment share</Typography.Text>
          <div style={{ marginTop: 20 }}>
            <Typography.Text>CASH</Typography.Text>
            <div style={{ height: 10, background: "#e5e7eb", borderRadius: 999, margin: "6px 0 12px" }}>
              <div style={{ width: "17%", height: "100%", background: "#ce6a1c", borderRadius: 999 }} />
            </div>
            <Typography.Text>CARD / QR</Typography.Text>
            <div style={{ height: 10, background: "#e5e7eb", borderRadius: 999, margin: "6px 0 0" }}>
              <div style={{ width: "83%", height: "100%", background: "#0b6e4f", borderRadius: 999 }} />
            </div>
          </div>
        </Card>
      </div>

      <Card title="Recent Orders" style={{ marginTop: 24 }}>
        <Table
          rowKey="orderNo"
          columns={columns}
          loading={ordersQuery.loading}
          dataSource={ordersQuery.data ?? []}
          pagination={false}
          locale={{ emptyText: "No orders yet" }}
        />
      </Card>
    </div>
  );
}
