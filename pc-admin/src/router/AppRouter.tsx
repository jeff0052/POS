import { Navigate, Route, Routes } from "react-router-dom";
import { AdminLayout } from "../layout/AdminLayout";
import { ProtectedRoute } from "./ProtectedRoute";
import { CategoriesPage } from "../pages/categories/CategoriesPage";
import { CrmPage } from "../pages/crm/CrmPage";
import { DashboardPage } from "../pages/dashboard/DashboardPage";
import { GtoSyncPage } from "../pages/gto/GtoSyncPage";
import { LoginPage } from "../pages/login/LoginPage";
import { OrdersPage } from "../pages/orders/OrdersPage";
import { ProductsPage } from "../pages/products/ProductsPage";
import { PromotionsPage } from "../pages/promotions/PromotionsPage";
import { RefundsPage } from "../pages/refunds/RefundsPage";
import { ReportsPage } from "../pages/reports/ReportsPage";

export function AppRouter() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="products" element={<ProductsPage />} />
        <Route path="categories" element={<CategoriesPage />} />
        <Route path="orders" element={<OrdersPage />} />
        <Route path="refunds" element={<RefundsPage />} />
        <Route path="crm" element={<CrmPage />} />
        <Route path="promotions" element={<PromotionsPage />} />
        <Route path="reports" element={<ReportsPage />} />
        <Route path="gto-sync" element={<GtoSyncPage />} />
      </Route>
    </Routes>
  );
}
