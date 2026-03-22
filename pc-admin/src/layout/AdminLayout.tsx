import {
  AppstoreOutlined,
  BarChartOutlined,
  DatabaseOutlined,
  LogoutOutlined,
  OrderedListOutlined,
  RollbackOutlined,
  ShopOutlined
} from "@ant-design/icons";
import { Button, Layout, Menu, Typography } from "antd";
import type { MenuProps } from "antd";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const { Header, Sider, Content } = Layout;

const items: MenuProps["items"] = [
  { key: "/dashboard", icon: <ShopOutlined />, label: "Dashboard" },
  { key: "/products", icon: <AppstoreOutlined />, label: "Products" },
  { key: "/categories", icon: <DatabaseOutlined />, label: "Categories" },
  { key: "/orders", icon: <OrderedListOutlined />, label: "Orders" },
  { key: "/refunds", icon: <RollbackOutlined />, label: "Refunds" },
  { key: "/reports", icon: <BarChartOutlined />, label: "Reports" }
];

export function AdminLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, signOut } = useAuth();

  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sider width={240} theme="light" style={{ borderRight: "1px solid #e5e7eb" }}>
        <div style={{ padding: 24 }}>
          <Typography.Title level={3} style={{ margin: 0 }}>
            Developer POS
          </Typography.Title>
          <Typography.Text type="secondary">Merchant Admin</Typography.Text>
        </div>
        <Menu
          mode="inline"
          selectedKeys={[location.pathname]}
          items={items}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: "rgba(255,255,255,0.7)",
            borderBottom: "1px solid #e5e7eb",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: "0 24px",
            backdropFilter: "blur(8px)"
          }}
        >
          <Typography.Text strong>Single-store POS control panel</Typography.Text>
          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
            <Typography.Text type="secondary">{user?.displayName ?? "Admin"}</Typography.Text>
            <Button
              icon={<LogoutOutlined />}
              onClick={() => {
                signOut();
                navigate("/login");
              }}
            >
              Logout
            </Button>
          </div>
        </Header>
        <Content>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
