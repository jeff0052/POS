import { Card, Typography } from "antd";

export function DevicesPage() {
  return (
    <div className="page-shell">
      <Typography.Title level={2} className="page-title">
        Devices
      </Typography.Title>
      <Typography.Paragraph className="page-subtitle">
        管理 POS 终端、打印机、扫码设备和版本状态。
      </Typography.Paragraph>
      <Card>Device fleet visibility, assignments, and firmware/app version tracking will live here.</Card>
    </div>
  );
}
