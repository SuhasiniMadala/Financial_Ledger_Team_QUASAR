import { Navigate, Route, Routes } from 'react-router-dom';
import type { Role } from './api/types';
import { useAuth } from './auth/AuthContext';
import { RequireRole } from './auth/RequireRole';
import { HOME_BY_ROLE } from './auth/session';
import { Layout } from './components/Layout';
import { routesFor } from './features/registry';
import { LoginPage } from './pages/LoginPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { RegisterPage } from './pages/RegisterPage';

const ROLES: Role[] = ['USER', 'MERCHANT', 'ADMIN'];

function HomeRedirect() {
  const { session } = useAuth();
  return <Navigate to={session ? HOME_BY_ROLE[session.role] : '/login'} replace />;
}

export function App() {
  return (
    <Routes>
      <Route path="/" element={<HomeRedirect />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      {ROLES.map((role) => (
        <Route
          key={role}
          element={
            <RequireRole role={role}>
              <Layout />
            </RequireRole>
          }
        >
          {routesFor(role).map((route) => (
            <Route key={route.path} path={route.path} element={route.element} />
          ))}
        </Route>
      ))}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
