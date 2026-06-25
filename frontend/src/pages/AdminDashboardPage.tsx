import { Link } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { api } from '../api/client';
import type {
  ExamTypeResponse,
  MetricResponse,
  PageHospitalServiceResponse,
  PageUserResponse,
} from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { platformFeatures } from '../config/features';

type AdminKpis = {
  hospitalServices: number | null;
  users: number | null;
  examTypes: number | null;
  occupancyPercent: number | null;
};

const emptyKpis: AdminKpis = {
  hospitalServices: null,
  users: null,
  examTypes: null,
  occupancyPercent: null,
};

type QuickVariant = 'accent' | 'success' | 'warning' | 'deep' | 'danger';

function QuickTile({
  to,
  title,
  description,
  variant,
  icon,
}: {
  to: string;
  title: string;
  description: string;
  variant: QuickVariant;
  icon: ReactNode;
}) {
  return (
    <Link to={to} className={`dashboard-quick-tile dashboard-quick-tile--${variant}`}>
      <span className="dashboard-quick-tile__icon-wrap">{icon}</span>
      <div className="dashboard-quick-tile__body">
        <h3 className="dashboard-quick-tile__title">{title}</h3>
        <p className="dashboard-quick-tile__desc">{description}</p>
      </div>
      <span className="dashboard-quick-tile__chevron" aria-hidden>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
          <path d="M9 6l6 6-6 6" />
        </svg>
      </span>
    </Link>
  );
}

function IconKpiBuilding() {
  return (
    <svg className="dashboard-kpi-card__icon" width="48" height="48" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M12 7V3H2v18h20V7H12zM6 19H4v-2h2v2zm0-4H4v-2h2v2zm0-4H4V9h2v2zm0-4H4V5h2v2zm4 12H8v-2h2v2zm0-4H8v-2h2v2zm0-4H8V9h2v2zm0-4H8V5h2v2zm10 12h-8v-2h2v-2h-2v-2h2v-2h-2V9h8v10zm-2-8h-2v2h2v-2zm0 4h-2v2h2v-2z" />
    </svg>
  );
}

function IconKpiUsers() {
  return (
    <svg className="dashboard-kpi-card__icon" width="48" height="48" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M12.65 10A5.99 5.99 0 007 6c-3.31 0-6 2.69-6 6s2.69 6 6 6a5.99 5.99 0 005.65-4H17v2h4v-2h2v-4H12.65zM7 15c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z" />
    </svg>
  );
}

function IconKpiLab() {
  return (
    <svg className="dashboard-kpi-card__icon" width="48" height="48" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M7 2v2h1v8.5c0 1.93-1.57 3.5-3.5 3.5S1 14.43 1 12.5 2.57 9 4.5 9c.46 0 .9.09 1.3.25V4H7V2zm10 0h2v6h2v2h-2v6.5c0 1.93-1.57 3.5-3.5 3.5S12 18.43 12 16.5 13.57 13 15.5 13c.46 0 .9.09 1.3.25V2z" />
    </svg>
  );
}

function IconKpiChart() {
  return (
    <svg className="dashboard-kpi-card__icon" width="48" height="48" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z" />
    </svg>
  );
}

function IconSmBuilding() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M12 7V3H2v18h20V7H12zM6 19H4v-2h2v2zm0-4H4v-2h2v2zm0-4H4V9h2v2zm0-4H4V5h2v2zm4 12H8v-2h2v2zm0-4H8v-2h2v2zm0-4H8V9h2v2zm0-4H8V5h2v2zm10 12h-8v-2h2v-2h-2v-2h2v-2h-2V9h8v10zm-2-8h-2v2h2v-2zm0 4h-2v2h2v-2z" />
    </svg>
  );
}

function IconSmUsers() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M12.65 10A5.99 5.99 0 007 6c-3.31 0-6 2.69-6 6s2.69 6 6 6a5.99 5.99 0 005.65-4H17v2h4v-2h2v-4H12.65zM7 15c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z" />
    </svg>
  );
}

function IconSmLab() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M7 2v2h1v8.5c0 1.93-1.57 3.5-3.5 3.5S1 14.43 1 12.5 2.57 9 4.5 9c.46 0 .9.09 1.3.25V4H7V2zm10 0h2v6h2v2h-2v6.5c0 1.93-1.57 3.5-3.5 3.5S12 18.43 12 16.5 13.57 13 15.5 13c.46 0 .9.09 1.3.25V2z" />
    </svg>
  );
}

function IconSmChart() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z" />
    </svg>
  );
}

function formatKpiValue(n: number | null, loading: boolean): string {
  if (loading) return '…';
  if (n === null) return '—';
  return String(n);
}

export function AdminDashboardPage() {
  const { user } = useAuth();
  const [kpis, setKpis] = useState<AdminKpis>(emptyKpis);
  const [kpiLoading, setKpiLoading] = useState(true);
  const [now, setNow] = useState(() => new Date());

  const displayName = user?.fullName?.trim() || user?.username?.trim() || 'Administrateur';

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 30000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function loadKpis() {
      setKpiLoading(true);
      const next: AdminKpis = { ...emptyKpis };
      const tasks: Promise<void>[] = [];

      tasks.push(
        api
          .get<PageHospitalServiceResponse>('/api/v1/hospital-services?page=0&size=1')
          .then((r) => {
            next.hospitalServices = r.data.totalElements;
          })
          .catch(() => {
            next.hospitalServices = null;
          }),
      );

      if (platformFeatures.usersAdmin) {
        tasks.push(
          api
            .get<PageUserResponse>('/api/v1/users?page=0&size=1')
            .then((r) => {
              next.users = r.data.totalElements;
            })
            .catch(() => {
              next.users = null;
            }),
        );
      }

      if (platformFeatures.labModule && platformFeatures.labExamTypesAdmin) {
        tasks.push(
          api
            .get<ExamTypeResponse[]>('/api/v1/lab/exam-types')
            .then((r) => {
              next.examTypes = r.data.length;
            })
            .catch(() => {
              next.examTypes = null;
            }),
        );
      }

      if (platformFeatures.statsDashboard) {
        tasks.push(
          api
            .get<MetricResponse>('/api/v1/stats/occupancy')
            .then((r) => {
              const v = r.data.value;
              if (v && typeof v === 'object' && v !== null && 'overallRatePercent' in v) {
                next.occupancyPercent = Number((v as { overallRatePercent: number }).overallRatePercent);
              }
            })
            .catch(() => {
              next.occupancyPercent = null;
            }),
        );
      }

      await Promise.all(tasks);
      if (!cancelled) {
        setKpis(next);
        setKpiLoading(false);
      }
    }

    void loadKpis();
    return () => {
      cancelled = true;
    };
  }, []);

  const datetimeLabel = useMemo(
    () =>
      now.toLocaleString('fr-FR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      }),
    [now],
  );

  return (
    <>
      <header className="dashboard-header">
        <div>
          <h1>Bon retour, {displayName}</h1>
          <p className="dashboard-subtitle">Administration de la plateforme Afya</p>
        </div>
        <div className="dashboard-datetime">{datetimeLabel}</div>
      </header>

      <div className="dashboard-kpi-row">
        <Link to="/hospital-services" className="dashboard-kpi-card dashboard-kpi-card--accent">
          <IconKpiBuilding />
          <span className="dashboard-kpi-card__label">Services</span>
          <span className="dashboard-kpi-card__value">{formatKpiValue(kpis.hospitalServices, kpiLoading)}</span>
          <span className="dashboard-kpi-card__hint">Départements et lits</span>
        </Link>

        {platformFeatures.usersAdmin ? (
          <Link to="/users" className="dashboard-kpi-card dashboard-kpi-card--danger">
            <IconKpiUsers />
            <span className="dashboard-kpi-card__label">Utilisateurs</span>
            <span className="dashboard-kpi-card__value">{formatKpiValue(kpis.users, kpiLoading)}</span>
            <span className="dashboard-kpi-card__hint">Comptes actifs</span>
          </Link>
        ) : (
          <div className="dashboard-kpi-card dashboard-kpi-card--danger dashboard-kpi-card--static">
            <IconKpiUsers />
            <span className="dashboard-kpi-card__label">Utilisateurs</span>
            <span className="dashboard-kpi-card__value">—</span>
            <span className="dashboard-kpi-card__hint">Module indisponible</span>
          </div>
        )}

        {platformFeatures.labModule && platformFeatures.labExamTypesAdmin ? (
          <Link to="/lab/exam-types" className="dashboard-kpi-card dashboard-kpi-card--success">
            <IconKpiLab />
            <span className="dashboard-kpi-card__label">Types d&apos;examens</span>
            <span className="dashboard-kpi-card__value">{formatKpiValue(kpis.examTypes, kpiLoading)}</span>
            <span className="dashboard-kpi-card__hint">Catalogue laboratoire</span>
          </Link>
        ) : (
          <div className="dashboard-kpi-card dashboard-kpi-card--success dashboard-kpi-card--static">
            <IconKpiLab />
            <span className="dashboard-kpi-card__label">Types d&apos;examens</span>
            <span className="dashboard-kpi-card__value">—</span>
            <span className="dashboard-kpi-card__hint">Catalogue laboratoire</span>
          </div>
        )}

        <Link to="/reporting" className="dashboard-kpi-card dashboard-kpi-card--deep">
          <IconKpiChart />
          <span className="dashboard-kpi-card__label">Occupation lits</span>
          <span className="dashboard-kpi-card__value">
            {kpiLoading ? '…' : kpis.occupancyPercent === null ? '—' : `${Math.round(kpis.occupancyPercent)}%`}
          </span>
          <span className="dashboard-kpi-card__hint">Indicateur global</span>
        </Link>
      </div>

      <h2 className="dashboard-section-title">Modules</h2>
      <div className="dashboard-quick-grid">
        <QuickTile
          to="/hospital-services"
          title="Organisation"
          description="Départements, services hospitaliers et provision des lits"
          variant="accent"
          icon={<IconSmBuilding />}
        />
        {platformFeatures.usersAdmin && (
          <QuickTile
            to="/users"
            title="Utilisateurs"
            description="Comptes, rôles, mots de passe et affectations"
            variant="danger"
            icon={<IconSmUsers />}
          />
        )}
        <QuickTile
          to="/reporting"
          title="Reporting"
          description="Occupation, volumes, audit et exports d&apos;activité"
          variant="deep"
          icon={<IconSmChart />}
        />
        {platformFeatures.labModule && platformFeatures.labExamTypesAdmin && (
          <QuickTile
            to="/lab/exam-types"
            title="Types d'examens"
            description="Catalogue laboratoire (référentiel administratif)"
            variant="success"
            icon={<IconSmLab />}
          />
        )}
      </div>
    </>
  );
}
