import { Link } from 'react-router-dom';
import type { LabDoctorStats } from '../hooks/useLabDoctorStats';

type LabDoctorKpiRowProps = {
  stats: LabDoctorStats;
  loading: boolean;
  activeStatus?: string;
  mineActive?: boolean;
};

function formatCount(n: number | null, loading: boolean): string {
  if (loading) return '…';
  if (n === null) return '—';
  return String(n);
}

export function LabDoctorKpiRow({ stats, loading, activeStatus, mineActive }: LabDoctorKpiRowProps) {
  return (
    <div className="dashboard-kpi-row lab-queue-kpi-row">
      <Link
        to="/lab/requests?mine=1&status=RESULTS_AVAILABLE"
        className={`dashboard-kpi-card dashboard-kpi-card--success${
          activeStatus === 'RESULTS_AVAILABLE' && mineActive ? ' lab-queue-kpi-row__card--active' : ''
        }`}
      >
        <span className="dashboard-kpi-card__label">Résultats à consulter</span>
        <span className="dashboard-kpi-card__value">{formatCount(stats.resultsAvailable, loading)}</span>
        <span className="dashboard-kpi-card__hint">Mes demandes — compte rendu publié</span>
      </Link>

      <Link
        to="/lab/requests?mine=1&status=PENDING"
        className={`dashboard-kpi-card dashboard-kpi-card--warning${
          activeStatus === 'PENDING' && mineActive ? ' lab-queue-kpi-row__card--active' : ''
        }`}
      >
        <span className="dashboard-kpi-card__label">En attente labo</span>
        <span className="dashboard-kpi-card__value">{formatCount(stats.pending, loading)}</span>
        <span className="dashboard-kpi-card__hint">Mes demandes — prélèvement à faire</span>
      </Link>

      <Link
        to="/lab/requests?mine=1&status=SPECIMEN_COLLECTED"
        className={`dashboard-kpi-card dashboard-kpi-card--accent${
          activeStatus === 'SPECIMEN_COLLECTED' && mineActive ? ' lab-queue-kpi-row__card--active' : ''
        }`}
      >
        <span className="dashboard-kpi-card__label">Analyse en cours</span>
        <span className="dashboard-kpi-card__value">{formatCount(stats.awaitingResults, loading)}</span>
        <span className="dashboard-kpi-card__hint">Prélèvement OK — résultats attendus</span>
      </Link>

      <Link
        to="/lab/requests?mine=1"
        className={`dashboard-kpi-card dashboard-kpi-card--deep${mineActive && !activeStatus ? ' lab-queue-kpi-row__card--active' : ''}`}
      >
        <span className="dashboard-kpi-card__label">Toutes mes demandes</span>
        <span className="dashboard-kpi-card__value">→</span>
        <span className="dashboard-kpi-card__hint">Historique complet</span>
      </Link>
    </div>
  );
}
