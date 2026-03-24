import { Alert, Card, Col, Row, Skeleton, Typography } from "antd";
import { getDashboardSummary, getSalesReportSummary } from "../../api/services/dashboardService";
import { useAsyncData } from "../../hooks/useAsyncData";

export function ReportsPage() {
  const query = useAsyncData(getDashboardSummary);
  const salesQuery = useAsyncData(getSalesReportSummary);
  const metrics = query.data
    ? [
        { label: "Revenue", value: query.data.revenue },
        { label: "Cash", value: "CNY 2,100.00" },
        { label: "SDK Pay", value: "CNY 10,580.00" },
        { label: "Refunds", value: query.data.refunds }
      ]
    : [];

  return (
    <div className="page-shell">
      <Typography.Title className="page-title" level={2}>
        Reports
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        第一版同时覆盖销售总览、会员消费、充值、促销让利和 GTO 批量同步。
      </Typography.Paragraph>

      {query.error ? <Alert type="error" message={query.error} /> : null}
      {salesQuery.error ? <Alert type="error" message={salesQuery.error} /> : null}
      {query.loading ? (
        <Skeleton active paragraph={{ rows: 4 }} />
      ) : (
        <Row gutter={[16, 16]}>
          {metrics.map((metric) => (
            <Col span={6} key={metric.label}>
              <Card>
                <Typography.Text type="secondary">{metric.label}</Typography.Text>
                <Typography.Title level={3} style={{ marginTop: 12, marginBottom: 0 }}>
                  {metric.value}
                </Typography.Title>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      <Row gutter={[16, 16]} style={{ marginTop: 8 }}>
        <Col span={6}>
          <Card title="Member Sales">{salesQuery.data?.memberSales ?? "-"}</Card>
        </Col>
        <Col span={6}>
          <Card title="Discounts">{salesQuery.data?.discounts ?? "-"}</Card>
        </Col>
        <Col span={6}>
          <Card title="Recharge">{salesQuery.data?.rechargeSales ?? "-"}</Card>
        </Col>
        <Col span={6}>
          <Card title="Pending GTO">{salesQuery.data?.pendingGtoBatches ?? "-"}</Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 8 }}>
        <Col span={12}>
          <Card title="Payment Summary">Chart placeholder</Card>
        </Col>
        <Col span={12}>
          <Card title="Promotion / Table Turnover">
            <Typography.Paragraph style={{ marginBottom: 8 }}>
              Table turnover rate: {salesQuery.data?.tableTurnover ?? "-"}
            </Typography.Paragraph>
            <Typography.Text type="secondary">
              这里后续补促销命中统计、满减让利、满赠成本和单桌销售。
            </Typography.Text>
          </Card>
        </Col>
      </Row>
    </div>
  );
}
