import { Alert, Button, Card, Form, Input, Typography } from "antd";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";

export function LoginPage() {
  const navigate = useNavigate();
  const { signIn } = useAuth();
  const [error, setError] = useState<string | null>(null);

  return (
    <div className="login-wrap">
      <Card className="login-card">
        <Typography.Title level={2}>Developer POS Admin</Typography.Title>
        <Typography.Paragraph type="secondary">
          Merchant后台骨架，先完成登录和核心管理页面结构。
        </Typography.Paragraph>
        {error ? <Alert type="error" message={error} style={{ marginBottom: 16 }} /> : null}
        <Form
          layout="vertical"
          onFinish={async (values: { username: string; password: string }) => {
            try {
              await signIn(values.username, values.password);
              navigate("/dashboard");
            } catch (err) {
              setError(err instanceof Error ? err.message : "Login failed");
            }
          }}
        >
          <Form.Item label="Username" name="username" rules={[{ required: true }]}>
            <Input placeholder="admin" />
          </Form.Item>
          <Form.Item label="Password" name="password" rules={[{ required: true }]}>
            <Input.Password placeholder="******" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block>
            Sign in
          </Button>
        </Form>
      </Card>
    </div>
  );
}
