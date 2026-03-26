import React from "react";
import ReactDOM from "react-dom/client";
import { ConfigProvider } from "antd";
import App from "./App";
import { AuthProvider } from "./auth/AuthContext";
import "./styles.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: "#3f4c63",
          colorInfo: "#3f4c63",
          colorSuccess: "#3f4c63",
          colorBgLayout: "#f4f7fb",
          colorBgContainer: "#ffffff",
          colorBorder: "#d9e3f0",
          colorBorderSecondary: "#e7edf5",
          colorText: "#223046",
          colorTextSecondary: "#6d7c93",
          borderRadius: 18,
          boxShadowSecondary: "0 14px 28px rgba(72, 111, 164, 0.08)"
        }
      }}
    >
      <AuthProvider>
        <App />
      </AuthProvider>
    </ConfigProvider>
  </React.StrictMode>
);
