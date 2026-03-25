import { Card, Col, Row, Typography } from "antd";

export function DashboardPage() {
  const metrics = [
    { label: "Active Merchants", value: "18" },
    { label: "Live Stores", value: "42" },
    { label: "Online Devices", value: "126" },
    { label: "Open Alerts", value: "3" }
  ];

  return (
    <div className="page-shell">
      <Typography.Title level={2} className="page-title">
        Platform Dashboard
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        全平台商户、门店、终端与告警的总览面板。
      </Typography.Paragraph>
      <Row gutter={[16, 16]}>
        {metrics.map((item) => (
          <Col span={6} key={item.label}>
            <Card>
              <Typography.Text type="secondary">{item.label}</Typography.Text>
              <Typography.Title level={3} style={{ marginTop: 12, marginBottom: 0 }}>
                {item.value}
              </Typography.Title>
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  );
}
