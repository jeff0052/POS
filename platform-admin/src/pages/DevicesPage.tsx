import { Badge, Card, Table, Tag, Typography } from "antd";

const { Title, Paragraph } = Typography;

const devices = [
  { id: 1, deviceName: "POS Terminal #1", deviceType: "Sunmi T2", store: "Riverside Branch", serialNo: "SN-T2-001", status: "ONLINE", appVersion: "2.0.1", lastSeen: "2 min ago" },
  { id: 2, deviceName: "POS Terminal #2", deviceType: "Sunmi T2", store: "Riverside Branch", serialNo: "SN-T2-002", status: "OFFLINE", appVersion: "2.0.0", lastSeen: "3 hours ago" },
  { id: 3, deviceName: "Kitchen Display", deviceType: "Sunmi D2", store: "Riverside Branch", serialNo: "SN-D2-001", status: "ONLINE", appVersion: "1.0.0", lastSeen: "1 min ago" },
];

const statusColor: Record<string, string> = { ONLINE: "green", OFFLINE: "red", MAINTENANCE: "orange" };

export function DevicesPage() {
  return (
    <div className="page-shell">
      <Title level={2} className="page-title">Devices</Title>
      <Paragraph className="page-subtitle">POS terminals, printers, scanners, and firmware version tracking.</Paragraph>
      <Card>
        <Table dataSource={devices} rowKey="id" pagination={false} columns={[
          { title: "Device", dataIndex: "deviceName" },
          { title: "Type", dataIndex: "deviceType" },
          { title: "Store", dataIndex: "store" },
          { title: "Serial No", dataIndex: "serialNo", render: (v: string) => <code>{v}</code> },
          { title: "Status", dataIndex: "status", render: (v: string) => <Badge status={v === "ONLINE" ? "success" : "error"} text={v} /> },
          { title: "App Version", dataIndex: "appVersion", render: (v: string) => <Tag>{v}</Tag> },
          { title: "Last Seen", dataIndex: "lastSeen" },
        ]} />
      </Card>
    </div>
  );
}
