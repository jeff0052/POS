import {
  AlertOutlined,
  ApartmentOutlined,
  BuildOutlined,
  DashboardOutlined,
  SettingOutlined,
  ShopOutlined,
  TeamOutlined
} from "@ant-design/icons";
import { Layout, Menu, Typography } from "antd";
import type { MenuProps } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router-dom";

const { Header, Sider, Content } = Layout;

const items: MenuProps["items"] = [
  { key: "/dashboard", icon: <DashboardOutlined />, label: "Dashboard" },
  { key: "/merchants", icon: <ShopOutlined />, label: "Merchants" },
  { key: "/stores", icon: <ApartmentOutlined />, label: "Stores" },
  { key: "/devices", icon: <BuildOutlined />, label: "Devices" },
  { key: "/configurations", icon: <SettingOutlined />, label: "Configurations" },
  { key: "/platform-users", icon: <TeamOutlined />, label: "Platform Users" },
  { key: "/support-monitoring", icon: <AlertOutlined />, label: "Support & Monitoring" }
];

export function PlatformAdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sider width={260} theme="light" style={{ borderRight: "1px solid #e5e7eb" }}>
        <div style={{ padding: 24 }}>
          <Typography.Title level={3} style={{ margin: 0 }}>
            Developer POS
          </Typography.Title>
          <Typography.Text type="secondary">Platform Admin</Typography.Text>
        </div>
        <Menu mode="inline" selectedKeys={[location.pathname]} items={items} onClick={({ key }) => navigate(key)} />
      </Sider>
      <Layout>
        <Header className="platform-header">
          <div>
            <Typography.Text strong>Platform Control Center</Typography.Text>
            <Typography.Paragraph style={{ margin: "4px 0 0", color: "#667085" }}>
              Merchant provisioning, store setup, fleet visibility, and support operations.
            </Typography.Paragraph>
          </div>
        </Header>
        <Content>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
