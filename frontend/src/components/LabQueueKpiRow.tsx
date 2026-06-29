import { Link } from 'react-router-dom';
import type { LabQueueStats } from '../hooks/useLabQueueStats';

type LabQueueKpiRowProps = {
  stats: LabQueueStats;
  loading: boolean;
  activeStatus?: string;
  urgentActive?: boolean;
};

function formatCount(n: number | null, loading: boolean): string {
  if (loading) return '…';
  if (n === null) return '—';
  return String(n);
}

export function LabQueueKpiRow({ stats, loading, activeStatus, urgentActive }: LabQueueKpiRowProps) {
  return (
    <div className="dashboard-kpi-row lab-queue-kpi-row">
      <Link
        to="/lab/requests?status=PENDING"
        className={`dashboard-kpi-card dashboard-kpi-card--warning${activeStatus === 'PENDING' && !urgentActive ? ' lab-queue-kpi-row__card--active' : ''}`}
      >
        <span className="dashboard-kpi-card__label">En attente</span>
        <span className="dashboard-kpi-card__value">{formatCount(stats.pending, loading)}</span>
        <span className="dashboard-kpi-card__hint">Prélèvement à effectuer</span>
      </Link>

      <Link
        to="/lab/requests?status=PENDING&urgent=1"
        className={`dashboard-kpi-card dashboard-kpi-card--danger${urgentActive ? ' lab-queue-kpi-row__card--active' : ''}`}
      >
        <span className="dashboard-kpi-card__label">Urgentes</span>
        <span className="dashboard-kpi-card__value">{formatCount(stats.urgentPending, loading)}</span>
        <span className="dashboard-kpi-card__hint">Priorité immédiate</span>
      </Link>

      <Link
        to="/lab/requests?status=SPECIMEN_COLLECTED"
        className={`dashboard-kpi-card dashboard-kpi-card--accent${activeStatus === 'SPECIMEN_COLLECTED' ? ' lab-queue-kpi-row__card--active' : ''}`}
      >
        <span className="dashboard-kpi-card__label">Résultats à saisir</span>
        <span className="dashboard-kpi-card__value">{formatCount(stats.awaitingResults, loading)}</span>
        <span className="dashboard-kpi-card__hint">Prélèvement effectué</span>
      </Link>

      <Link
        to="/lab/requests?status=RESULTS_AVAILABLE"
        className={`dashboard-kpi-card dashboard-kpi-card--success${activeStatus === 'RESULTS_AVAILABLE' ? ' lab-queue-kpi-row__card--active' : ''}`}
      >
        <span className="dashboard-kpi-card__label">Publiées</span>
        <span className="dashboard-kpi-card__value">{formatCount(stats.resultsAvailable, loading)}</span>
        <span className="dashboard-kpi-card__hint">Résultats disponibles</span>
      </Link>
    </div>
  );
}
