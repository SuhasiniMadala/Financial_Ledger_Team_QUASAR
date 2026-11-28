import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import type { Role } from '../api/types';
import { ForbiddenPage } from '../pages/ForbiddenPage';
import { useAuth } from './AuthContext';

/**
 * UI-03: only the screens of the logged-in role are reachable. This is a convenience for the user; the backend
 * still checks the role and ownership of every request (FR-AUTH-08, NFR-SEC-03).
 */
export function RequireRole({ role, children }: { role: Role; children: ReactNode }) {
  const { session } = useAuth();
  const location = useLocation();
  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (session.role !== role) {
    return <ForbiddenPage />;
  }
  return <>{children}</>;
}
