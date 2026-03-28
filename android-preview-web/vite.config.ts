import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const proxyTarget = process.env.VITE_API_PROXY_TARGET ?? "http://localhost:8098";
const qrOrderingUrlBase = process.env.VITE_QR_ORDERING_URL_BASE ?? "http://localhost:4183";

export default defineConfig({
  base: "./",
  plugins: [react()],
  define: {
    "import.meta.env.VITE_QR_ORDERING_URL_BASE": JSON.stringify(qrOrderingUrlBase)
  },
  server: {
    proxy: {
      "/api": {
        target: proxyTarget,
        changeOrigin: true
      }
    }
  }
});
