import {
  Navigate,
  Route,
  Routes,
} from 'react-router-dom';

import LoginPage from './pages/LoginPage';
import IntegrationPage from './pages/IntegrationPage';
import ClientePage from './pages/ClientePage';
import AdminPage from './pages/AdminPage';
import CatalogoPage from './pages/CatalogoPage';

import AppLayout from './components/AppLayout';

import ProtectedRoute from './routes/ProtectedRoute';
import RoleRoute from './routes/RoleRoute';

export default function App() {
  return (
    <Routes>
      <Route
        path="/login"
        element={<LoginPage />}
      />

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route
            path="/"
            element={
              <Navigate
                to="/catalogo"
                replace
              />
            }
          />

          <Route
            path="/catalogo"
            element={<CatalogoPage />}
          />

          <Route
            path="/diagnostico"
            element={<IntegrationPage />}
          />

          <Route
            element={
              <RoleRoute
                allowedRoles={['CLIENTE']}
              />
            }
          >
            <Route
              path="/cliente"
              element={<ClientePage />}
            />
          </Route>

          <Route
            element={
              <RoleRoute
                allowedRoles={['ADMIN']}
              />
            }
          >
            <Route
              path="/admin"
              element={<AdminPage />}
            />
          </Route>
        </Route>
      </Route>

      <Route
        path="*"
        element={<Navigate to="/" replace />}
      />
    </Routes>
  );
}