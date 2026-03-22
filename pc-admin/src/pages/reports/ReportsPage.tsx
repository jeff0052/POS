import { Alert, Card, Col, Row, Skeleton, Typography } from "antd";
import { getDashboardSummary } from "../../api/services/dashboardService";
import { useAsyncData } from "../../hooks/useAsyncData";

export function ReportsPage() {
  const query = useAsyncData(getDashboardSummary);
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
        第一版先做经营总览、支付方式统计和热销商品占位。
      </Typography.Paragraph>

      {query.error ? <Alert type="error" message={query.error} /> : null}
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
        <Col span={12}>
          <Card title="Payment Summary">Chart placeholder</Card>
        </Col>
        <Col span={12}>
          <Card title="Hot Products">Ranking placeholder</Card>
        </Col>
      </Row>
    </div>
  );
}
