import { useState } from "react";
import { apiPostV2, saveToken } from "../api/client";

interface LoginResponse {
  token: string;
  user: {
    id: number;
    username: string;
    displayName: string;
    role: string;
    storeId: number | null;
  };
}

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const response = await apiPostV2<LoginResponse>("/auth/login", {
        username,
        password,
        clientType: "PLATFORM"
      });

      if (response.user.role !== "PLATFORM_ADMIN") {
        setError("Access denied. Platform admin role required.");
        setLoading(false);
        return;
      }

      saveToken(response.token);
      localStorage.setItem("platform-admin-user", JSON.stringify(response.user));
      window.location.href = "/platform/";
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: "100vh", background: "#f0f2f5" }}>
      <form onSubmit={handleLogin} style={{ background: "white", padding: 40, borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)", width: 360 }}>
        <h2 style={{ margin: "0 0 8px", fontSize: 24 }}>Platform Admin</h2>
        <p style={{ margin: "0 0 24px", color: "#666" }}>Sign in to manage the platform</p>

        {error && <div style={{ background: "#fff2f0", border: "1px solid #ffccc7", borderRadius: 6, padding: "8px 12px", marginBottom: 16, color: "#cf1322", fontSize: 14 }}>{error}</div>}

        <div style={{ marginBottom: 16 }}>
          <label style={{ display: "block", marginBottom: 4, fontWeight: 500 }}>Username</label>
          <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} required
            style={{ width: "100%", padding: "8px 12px", border: "1px solid #d9d9d9", borderRadius: 6, fontSize: 14, boxSizing: "border-box" }} />
        </div>

        <div style={{ marginBottom: 24 }}>
          <label style={{ display: "block", marginBottom: 4, fontWeight: 500 }}>Password</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required
            style={{ width: "100%", padding: "8px 12px", border: "1px solid #d9d9d9", borderRadius: 6, fontSize: 14, boxSizing: "border-box" }} />
        </div>

        <button type="submit" disabled={loading}
          style={{ width: "100%", padding: "10px 0", background: loading ? "#999" : "#1890ff", color: "white", border: "none", borderRadius: 6, fontSize: 16, cursor: loading ? "default" : "pointer" }}>
          {loading ? "Signing in..." : "Sign In"}
        </button>
      </form>
    </div>
  );
}
