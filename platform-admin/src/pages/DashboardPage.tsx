import { Card, Col, Row, Spin, Typography } from "antd";
import { useEffect, useState } from "react";
import { apiGetV2 } from "../api/client";

const { Title, Text, Paragraph } = Typography;

interface DashboardData {
  totalMerchants: number;
  totalStores: number;
  activeStores: number;
  totalDevices: number;
  systemStatus: string;
}

export function DashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiGetV2<DashboardData>("/platform/dashboard")
      .then(setData)
      .catch(() => setData(null))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Spin size="large" style={{ display: "block", margin: "100px auto" }} />;

  const metrics = [
    { label: "Total Merchants", value: data?.totalMerchants ?? 0, color: "#1890ff" },
    { label: "Total Stores", value: data?.totalStores ?? 0, color: "#52c41a" },
    { label: "Active Stores", value: data?.activeStores ?? 0, color: "#faad14" },
    { label: "System Status", value: data?.systemStatus ?? "UNKNOWN", color: "#13c2c2" },
  ];

  return (
    <div className="page-shell">
      <Title level={2} className="page-title">Platform Dashboard</Title>
      <Paragraph className="page-subtitle">Overview of all merchants, stores, and system health.</Paragraph>
      <Row gutter={[16, 16]}>
        {metrics.map((m) => (
          <Col span={6} key={m.label}>
            <Card>
              <Text type="secondary">{m.label}</Text>
              <Title level={3} style={{ color: m.color, marginTop: 12, marginBottom: 0 }}>
                {m.value}
              </Title>
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  );
}
