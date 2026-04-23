import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "../auth/AuthContext";
import { ProtectedRoute } from "../auth/ProtectedRoute";
import { BulkUploadPage } from "../pages/BulkUploadPage";
import { CustomerFormPage } from "../pages/CustomerFormPage";
import { CustomerViewPage } from "../pages/CustomerViewPage";
import { CustomersPage } from "../pages/CustomersPage";
import { LoginPage } from "../pages/LoginPage";

export default function AppRouter() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<LoginPage />} path="/login" />
          <Route
            element={
              <ProtectedRoute>
                <CustomersPage />
              </ProtectedRoute>
            }
            path="/customers"
          />
          <Route
            element={
              <ProtectedRoute>
                <CustomerFormPage mode="create" />
              </ProtectedRoute>
            }
            path="/customers/new"
          />
          <Route
            element={
              <ProtectedRoute>
                <CustomerViewPage />
              </ProtectedRoute>
            }
            path="/customers/:id"
          />
          <Route
            element={
              <ProtectedRoute>
                <CustomerFormPage mode="edit" />
              </ProtectedRoute>
            }
            path="/customers/:id/edit"
          />
          <Route
            element={
              <ProtectedRoute>
                <BulkUploadPage />
              </ProtectedRoute>
            }
            path="/customers/bulk-upload"
          />
          <Route element={<Navigate replace to="/customers" />} path="*" />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
