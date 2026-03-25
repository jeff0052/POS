import { Card, Typography } from "antd";

export function MerchantsPage() {
  return (
    <div className="page-shell">
      <Typography.Title level={2} className="page-title">
        Merchants
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        平台侧管理商户开通、套餐、状态和支持信息。
      </Typography.Paragraph>
      <Card>Merchant list, onboarding actions, package status, and account health will live here.</Card>
    </div>
  );
}
