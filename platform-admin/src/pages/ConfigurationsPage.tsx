import { Card, Col, Descriptions, Row, Switch, Typography } from "antd";

const { Title, Paragraph } = Typography;

const configs = [
  { category: "Payment", items: [
    { key: "DCS Card Terminal", value: "Enabled", editable: true },
    { key: "VibeCash QR Gateway", value: "Enabled", editable: true },
    { key: "Cash Payment", value: "Enabled", editable: true },
    { key: "Default Currency", value: "SGD", editable: false },
  ]},
  { category: "Tax & Compliance", items: [
    { key: "GST Rate", value: "9%", editable: false },
    { key: "GTO Export", value: "Enabled", editable: true },
    { key: "Tax Calculation", value: "Inclusive (9/109)", editable: false },
  ]},
  { category: "Printing", items: [
    { key: "Receipt Printer", value: "Not Configured", editable: false },
    { key: "Kitchen Printer", value: "Not Configured", editable: false },
    { key: "Auto-print on Settlement", value: "Disabled", editable: true },
  ]},
  { category: "AI Settings", items: [
    { key: "AI Operator", value: "Enabled", editable: true },
    { key: "Auto-approve LOW risk", value: "Enabled", editable: true },
    { key: "Daily Report Generation", value: "09:00 SGT", editable: false },
  ]},
];

export function ConfigurationsPage() {
  return (
    <div className="page-shell">
      <Title level={2} className="page-title">Configurations</Title>
      <Paragraph className="page-subtitle">Platform templates, payment parameters, tax settings, and AI configuration.</Paragraph>
      <Row gutter={[24, 24]}>
        {configs.map((group) => (
          <Col xs={24} md={12} key={group.category}>
            <Card title={group.category}>
              <Descriptions column={1} size="small">
                {group.items.map((item) => (
                  <Descriptions.Item key={item.key} label={item.key}>
                    {item.editable ? <Switch defaultChecked={item.value === "Enabled"} size="small" /> : item.value}
                  </Descriptions.Item>
                ))}
              </Descriptions>
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  );
}
