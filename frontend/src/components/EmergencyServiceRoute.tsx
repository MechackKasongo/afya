import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { isEmergencyStaffUser } from '../auth/roles';

/** Accès réservé au personnel affecté à un service des urgences. */
export function EmergencyServiceRoute() {
  const { user, loading } = useAuth();

  if (loading) {
    return <p style={{ color: 'var(--muted)' }}>Chargement…</p>;
  }
  if (!isEmergencyStaffUser(user)) {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}
