import { Alert, Card, Col, List, Row, Tag, Timeline, Typography } from "antd";

const { Title, Paragraph, Text } = Typography;

const alerts = [
  { id: 1, level: "warning", message: "Store 'Riverside Branch' — POS Terminal #2 offline for 3 hours", time: "14:30" },
  { id: 2, level: "info", message: "Flyway migration V055 applied successfully on AWS", time: "12:15" },
  { id: 3, level: "success", message: "All 6 Docker containers healthy", time: "09:00" },
];

const healthChecks = [
  { service: "Backend API", status: "HEALTHY", latency: "45ms" },
  { service: "MySQL Database", status: "HEALTHY", latency: "12ms" },
  { service: "POS Frontend", status: "HEALTHY", latency: "8ms" },
  { service: "Admin Frontend", status: "HEALTHY", latency: "6ms" },
  { service: "QR Frontend", status: "HEALTHY", latency: "7ms" },
  { service: "Platform Admin", status: "HEALTHY", latency: "5ms" },
];

const recentEvents = [
  { time: "16:45", event: "Refund REF702f212760f4 processed — SGD 10.00" },
  { time: "16:30", event: "Platform Admin deployed to AWS" },
  { time: "15:00", event: "V1 code cleanup — 57 files removed" },
  { time: "12:00", event: "CI/CD pipeline configured (GitHub Actions)" },
  { time: "09:00", event: "Daily health check — all systems nominal" },
];

export function SupportMonitoringPage() {
  return (
    <div className="page-shell">
      <Title level={2} className="page-title">Support & Monitoring</Title>
      <Paragraph className="page-subtitle">System health, alerts, and operational event timeline.</Paragraph>

      <Row gutter={[24, 24]}>
        <Col xs={24} md={12}>
          <Card title="Active Alerts">
            {alerts.map((a) => (
              <Alert
                key={a.id}
                type={a.level as "warning" | "info" | "success"}
                message={a.message}
                description={a.time}
                showIcon
                style={{ marginBottom: 12 }}
              />
            ))}
          </Card>
        </Col>

        <Col xs={24} md={12}>
          <Card title="Service Health">
            <List
              dataSource={healthChecks}
              renderItem={(item) => (
                <List.Item>
                  <Text>{item.service}</Text>
                  <div>
                    <Tag color={item.status === "HEALTHY" ? "green" : "red"}>{item.status}</Tag>
                    <Text type="secondary">{item.latency}</Text>
                  </div>
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>

      <Card title="Recent Events" style={{ marginTop: 24 }}>
        <Timeline
          items={recentEvents.map((e) => ({
            children: <><Text type="secondary">{e.time}</Text> — {e.event}</>,
          }))}
        />
      </Card>
    </div>
  );
}
