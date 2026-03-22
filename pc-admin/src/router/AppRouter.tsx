import { Navigate, Route, Routes } from "react-router-dom";
import { AdminLayout } from "../layout/AdminLayout";
import { ProtectedRoute } from "./ProtectedRoute";
import { CategoriesPage } from "../pages/categories/CategoriesPage";
import { DashboardPage } from "../pages/dashboard/DashboardPage";
import { LoginPage } from "../pages/login/LoginPage";
import { OrdersPage } from "../pages/orders/OrdersPage";
import { ProductsPage } from "../pages/products/ProductsPage";
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
        <Route path="reports" element={<ReportsPage />} />
      </Route>
    </Routes>
  );
}
