import { useAuth } from '../auth/AuthContext';
import { isAdminPortalUser } from '../auth/roles';
import { AdminDashboardPage } from './AdminDashboardPage';
import { DashboardPage } from './DashboardPage';

/** Accueil selon le rôle : administration plateforme ou tableau de bord clinique. */
export function HomePage() {
  const { user } = useAuth();
  if (isAdminPortalUser(user)) {
    return <AdminDashboardPage />;
  }
  return <DashboardPage />;
}
