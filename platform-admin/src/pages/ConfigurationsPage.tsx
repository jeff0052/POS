import { Card, Typography } from "antd";

export function ConfigurationsPage() {
  return (
    <div className="page-shell">
      <Typography.Title level={2} className="page-title">
        Configurations
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        平台模板、支付参数、打印参数、GTO 对接参数和默认策略。
      </Typography.Paragraph>
      <Card>Global templates and rollout-safe configuration management will live here.</Card>
    </div>
  );
}
