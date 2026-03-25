import { Navigate, Route, Routes } from "react-router-dom";
import { PlatformAdminLayout } from "../layout/PlatformAdminLayout";
import { DashboardPage } from "../pages/DashboardPage";
import { MerchantsPage } from "../pages/MerchantsPage";
import { StoresPage } from "../pages/StoresPage";
import { DevicesPage } from "../pages/DevicesPage";
import { ConfigurationsPage } from "../pages/ConfigurationsPage";
import { PlatformUsersPage } from "../pages/PlatformUsersPage";
import { SupportMonitoringPage } from "../pages/SupportMonitoringPage";

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<PlatformAdminLayout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="merchants" element={<MerchantsPage />} />
        <Route path="stores" element={<StoresPage />} />
        <Route path="devices" element={<DevicesPage />} />
        <Route path="configurations" element={<ConfigurationsPage />} />
        <Route path="platform-users" element={<PlatformUsersPage />} />
        <Route path="support-monitoring" element={<SupportMonitoringPage />} />
      </Route>
    </Routes>
  );
}
