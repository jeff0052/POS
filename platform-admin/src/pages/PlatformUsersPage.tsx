import { Card, Table, Tag, Typography } from "antd";

const { Title, Paragraph } = Typography;

const users = [
  { id: 1, username: "jeff", displayName: "Jeff", role: "SUPER_ADMIN", email: "jeff@ontanetwork.com", status: "ACTIVE", lastLogin: "2026-03-27 09:00" },
  { id: 2, username: "ops01", displayName: "Operations Lead", role: "OPERATIONS", email: "ops@demo.com", status: "ACTIVE", lastLogin: "2026-03-26 18:30" },
  { id: 3, username: "finance01", displayName: "Finance Manager", role: "FINANCE", email: "finance@demo.com", status: "ACTIVE", lastLogin: "2026-03-25 14:00" },
  { id: 4, username: "support01", displayName: "Tech Support", role: "SUPPORT", email: "support@demo.com", status: "INACTIVE", lastLogin: "2026-03-20 10:00" },
];

const roleColors: Record<string, string> = { SUPER_ADMIN: "red", OPERATIONS: "blue", FINANCE: "green", SUPPORT: "orange" };

export function PlatformUsersPage() {
  return (
    <div className="page-shell">
      <Title level={2} className="page-title">Platform Users</Title>
      <Paragraph className="page-subtitle">Platform admins, operations, finance, and tech support accounts.</Paragraph>
      <Card>
        <Table dataSource={users} rowKey="id" pagination={false} columns={[
          { title: "Username", dataIndex: "username" },
          { title: "Name", dataIndex: "displayName" },
          { title: "Role", dataIndex: "role", render: (v: string) => <Tag color={roleColors[v] || "default"}>{v}</Tag> },
          { title: "Email", dataIndex: "email" },
          { title: "Status", dataIndex: "status", render: (v: string) => <Tag color={v === "ACTIVE" ? "green" : "default"}>{v}</Tag> },
          { title: "Last Login", dataIndex: "lastLogin" },
        ]} />
      </Card>
    </div>
  );
}
