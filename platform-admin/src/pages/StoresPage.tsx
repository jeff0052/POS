import { Card, Typography } from "antd";

export function StoresPage() {
  return (
    <div className="page-shell">
      <Typography.Title level={2} className="page-title">
        Stores
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        平台侧查看门店开通状态、桌台规模、终端接入和配置覆盖率。
      </Typography.Paragraph>
      <Card>Store provisioning, readiness checklist, and rollout status will live here.</Card>
    </div>
  );
}
