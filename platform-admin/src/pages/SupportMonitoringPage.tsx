import { Card, Typography } from "antd";

export function SupportMonitoringPage() {
  return (
    <div className="page-shell">
      <Typography.Title level={2} className="page-title">
        Support & Monitoring
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        问题排查、平台告警、商户健康度和升级支持入口。
      </Typography.Paragraph>
      <Card>Alert queues, health signals, and support workbench will live here.</Card>
    </div>
  );
}
