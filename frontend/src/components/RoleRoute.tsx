import type { ReactNode } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { hasAnyRole, type AppRole } from '../auth/roles';

type RoleRouteProps = {
  allowed: AppRole[];
  /** Si fourni, évite une route layout sans path (source fréquente de page blanche). */
  children?: ReactNode;
};

export function RoleRoute({ allowed, children }: RoleRouteProps) {
  const { user, loading } = useAuth();

  if (loading) {
    return <p style={{ color: 'var(--muted)' }}>Chargement…</p>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (!hasAnyRole(user, allowed)) {
    return <Navigate to="/" replace />;
  }
  if (children != null) {
    return <>{children}</>;
  }
  return <Outlet />;
}
