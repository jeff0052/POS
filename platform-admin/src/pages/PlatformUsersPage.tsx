import { Card, Typography } from "antd";

export function PlatformUsersPage() {
  return (
    <div className="page-shell">
      <Typography.Title level={2} className="page-title">
        Platform Users
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        平台管理员、运营、财务、实施和技术支持账号管理。
      </Typography.Paragraph>
      <Card>Role matrix, platform accounts, and audit visibility will live here.</Card>
    </div>
  );
}
