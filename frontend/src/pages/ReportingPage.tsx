import { useCallback, useEffect, useMemo, useState, type ReactNode, type UIEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type {
  AuditEventResponse,
  MetricResponse,
  OccupancyStatsValue,
  OperationalStatsResponse,
  PlatformVolumesValue,
  ServiceOccupancyStats,
  PageAuditEventResponse,
  PageUserResponse,
  RoleOptionResponse,
} from '../api/types';
import {
  DataTableColumnHeader,
  DataTableFilterCell,
  DataTableFilterHint,
  DataTableFilterSelectCell,
} from '../components/DataTableColumnHeader';
import { ScrollTableRegion } from '../components/ScrollTableRegion';
import { EmptyState } from '../components/ui/EmptyState';
import { LoadingBlock } from '../components/ui/LoadingBlock';
import { PageHeader } from '../components/ui/PageHeader';
import { ReportingTabs, resolveReportingTab } from '../components/ui/ReportingTabs';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';
import {
  formatAuditActionLabel,
  formatAuditDateTime,
  formatAuditMetadataHint,
  formatAuditResource,
  formatInstantRangeFr,
  formatNumberFr,
  formatServiceLabel,
  parseActorRolesFromMetadata,
  periodToRange,
  type ReportingPeriod,
} from '../utils/reporting';
import {
  compareStrings,
  defaultSortDirForColumn,
  toggleTableSort,
  type TableSortDir,
} from '../utils/tableSort';
import { roleLabelFor } from '../utils/users';

const AUDIT_PAGE_SIZE = 25;

type AuditSortKey = 'occurredAt' | 'actor' | 'action' | 'object' | 'service' | 'role';
type AuditFilterColumn = 'actor' | 'action' | 'object' | 'service' | 'role';

type AuditColumnFilters = {
  actor: string;
  action: string;
  object: string;
  service: string;
  role: string;
};

const emptyAuditFilters: AuditColumnFilters = {
  actor: '',
  action: '',
  object: '',
  service: '',
  role: '',
};

function parseOccupancy(value: unknown): OccupancyStatsValue | null {
  if (!value || typeof value !== 'object') return null;
  const v = value as Record<string, unknown>;
  if (
    typeof v.overallRatePercent !== 'number' ||
    typeof v.totalBeds !== 'number' ||
    typeof v.occupiedBeds !== 'number' ||
    typeof v.availableBeds !== 'number'
  ) {
    return null;
  }
  const byService = Array.isArray(v.byService)
    ? (v.byService as ServiceOccupancyStats[]).filter(
        (row) =>
          row &&
          typeof row.serviceName === 'string' &&
          typeof row.totalBeds === 'number' &&
          typeof row.occupiedBeds === 'number',
      )
    : [];
  return {
    overallRatePercent: v.overallRatePercent,
    totalBeds: v.totalBeds,
    occupiedBeds: v.occupiedBeds,
    availableBeds: v.availableBeds,
    byService,
  };
}

function formatRatePercent(rate: number): string {
  return `${Math.min(100, Math.max(0, rate)).toFixed(1)} %`;
}

function parseVolumes(value: unknown): PlatformVolumesValue | null {
  if (!value || typeof value !== 'object') return null;
  const v = value as Record<string, unknown>;
  if (
    typeof v.activeAdmissions !== 'number' ||
    typeof v.transferredAdmissions !== 'number' ||
    typeof v.dischargedAdmissions !== 'number' ||
    typeof v.deceasedAdmissions !== 'number'
  ) {
    return null;
  }
  return value as PlatformVolumesValue;
}

type ReportingMetricTone = 'deep' | 'accent' | 'success';

function ReportingMetricCard({
  title,
  tone,
  children,
}: {
  title: string;
  tone: ReportingMetricTone;
  children: ReactNode;
}) {
  return (
    <article className={`reporting-metric-card reporting-metric-card--${tone}`}>
      <h3 className="reporting-metric-card__title">{title}</h3>
      <div className="reporting-metric-card__body">{children}</div>
    </article>
  );
}

function OccupationReportingPanel({
  occupancy,
  volumes,
}: {
  occupancy: OccupancyStatsValue | null;
  volumes: PlatformVolumesValue | null;
}) {
  const stayRows = volumes
    ? [
        { label: 'Patients admis', hint: 'Séjours ouverts ou transférés', value: volumes.activeAdmissions },
        { label: 'Transferts', hint: 'Vers un autre service', value: volumes.transferredAdmissions },
        { label: 'Sorties', hint: 'Sorties vivantes enregistrées', value: volumes.dischargedAdmissions },
        { label: 'Décès', hint: 'Déclarations enregistrées', value: volumes.deceasedAdmissions },
      ]
    : [];

  return (
    <div className="reporting-metrics-grid">
      {occupancy ? (
        <ReportingMetricCard title="Occupation globale" tone="deep">
          <table className="data-table reporting-metric-table">
            <thead>
              <tr>
                <th scope="col">Périmètre</th>
                <th scope="col" className="num">Total</th>
                <th scope="col" className="num">Occupés</th>
                <th scope="col" className="num">Disponibles</th>
                <th scope="col" className="num">Taux</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Établissement</td>
                <td className="num">{formatNumberFr(occupancy.totalBeds)}</td>
                <td className="num">{formatNumberFr(occupancy.occupiedBeds)}</td>
                <td className="num">{formatNumberFr(occupancy.availableBeds)}</td>
                <td className="num reporting-metric-table__emphasis">
                  {formatRatePercent(occupancy.overallRatePercent)}
                </td>
              </tr>
            </tbody>
          </table>
        </ReportingMetricCard>
      ) : null}

      {occupancy ? (
        <ReportingMetricCard title="Occupation par service" tone="accent">
          <table className="data-table reporting-metric-table">
            <thead>
              <tr>
                <th scope="col">Service</th>
                <th scope="col">Département</th>
                <th scope="col" className="num">Total</th>
                <th scope="col" className="num">Occupés</th>
                <th scope="col" className="num">Disponibles</th>
                <th scope="col" className="num">Taux</th>
              </tr>
            </thead>
            <tbody>
              {occupancy.byService.length === 0 ? (
                <tr>
                  <td colSpan={6} className="muted-text">
                    Aucun lit provisionné pour un service actif.
                  </td>
                </tr>
              ) : (
                occupancy.byService.map((row) => (
                  <tr key={row.hospitalServiceId}>
                    <td>{row.serviceName}</td>
                    <td className="muted-text">{row.departmentName}</td>
                    <td className="num">{formatNumberFr(row.totalBeds)}</td>
                    <td className="num">{formatNumberFr(row.occupiedBeds)}</td>
                    <td className="num">{formatNumberFr(row.availableBeds)}</td>
                    <td className="num reporting-metric-table__emphasis">
                      {formatRatePercent(row.ratePercent)}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </ReportingMetricCard>
      ) : null}

      {volumes ? (
        <ReportingMetricCard title="Séjours hospitaliers" tone="success">
          <table className="data-table reporting-metric-table">
            <thead>
              <tr>
                <th scope="col">Indicateur</th>
                <th scope="col">Détail</th>
                <th scope="col" className="num">Nombre</th>
              </tr>
            </thead>
            <tbody>
              {stayRows.map((row) => (
                <tr key={row.label}>
                  <td>{row.label}</td>
                  <td className="muted-text">{row.hint}</td>
                  <td className="num reporting-metric-table__emphasis">{formatNumberFr(row.value)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </ReportingMetricCard>
      ) : null}
    </div>
  );
}

type EnrichedAuditEvent = AuditEventResponse & { roleLabel: string };

function resolveRoleLabel(
  event: AuditEventResponse,
  roleByUsername: Map<string, string>,
  rolesCatalog: RoleOptionResponse[],
): string {
  const fromMeta = parseActorRolesFromMetadata(event.metadataJson);
  if (fromMeta.length > 0) {
    return fromMeta.map((r) => roleLabelFor(rolesCatalog, r)).join(', ');
  }
  const role = roleByUsername.get(event.actorUsername);
  return role ? roleLabelFor(rolesCatalog, role) : '—';
}

function parseDownloadFileName(contentDisposition: string | undefined, fallback: string): string {
  if (!contentDisposition) return fallback;
  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1]);
    } catch {
      return utf8Match[1];
    }
  }
  const plainMatch = /filename="?([^";]+)"?/i.exec(contentDisposition);
  return plainMatch?.[1] ?? fallback;
}

async function downloadActivityExport(format: 'pdf' | 'xlsx', from: string, to: string): Promise<void> {
  const response = await api.get<Blob>('/api/v1/reports/activity/export', {
    params: { format, from, to },
    responseType: 'blob',
  });
  const fallback = format === 'pdf' ? 'rapport-activite.pdf' : 'rapport-activite.xlsx';
  const fileName = parseDownloadFileName(response.headers['content-disposition'], fallback);
  const blobUrl = URL.createObjectURL(response.data);
  const anchor = document.createElement('a');
  anchor.href = blobUrl;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(blobUrl);
}

function OperationalStatsPanel({
  period,
  onPeriodChange,
  refreshToken,
  onRefresh,
}: {
  period: ReportingPeriod;
  onPeriodChange: (period: ReportingPeriod) => void;
  refreshToken: number;
  onRefresh: () => void;
}) {
  const [stats, setStats] = useState<OperationalStatsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [exporting, setExporting] = useState<'pdf' | 'xlsx' | null>(null);

  const range = useMemo(() => periodToRange(period), [period]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    void (async () => {
      try {
        const { data } = await api.get<OperationalStatsResponse>('/api/v1/reports/operational-stats', {
          params: { from: range.from, to: range.to },
        });
        if (!cancelled) {
          setStats(data);
        }
      } catch (err) {
        if (!cancelled) {
          setStats(null);
          setError(getApiErrorMessage(err, 'Impossible de charger les statistiques opérationnelles.'));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [range.from, range.to, refreshToken]);

  async function handleExport(format: 'pdf' | 'xlsx') {
    setExporting(format);
    setError(null);
    try {
      await downloadActivityExport(format, range.from, range.to);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Export du rapport impossible.'));
    } finally {
      setExporting(null);
    }
  }

  const labRows = stats
    ? [
        { label: 'Demandes d’examens', value: stats.lab.examRequests },
        { label: 'En attente', value: stats.lab.pendingRequests },
        { label: 'Prélèvements enregistrés', value: stats.lab.specimenCollected },
        { label: 'Résultats disponibles', value: stats.lab.resultsAvailable },
        { label: 'Paramètres anormaux', value: stats.lab.abnormalParameters },
      ]
    : [];

  const nursingRows = stats
    ? [
        { label: 'Relevés de constantes', value: stats.nursing.vitalSignReadings },
        { label: 'Alertes constantes', value: stats.nursing.vitalSignAlerts },
        { label: 'Notifications prescription', value: stats.nursing.prescriptionNotifications },
        { label: 'Prescriptions exécutées', value: stats.nursing.executedPrescriptions },
      ]
    : [];

  const activityRows = stats
    ? [
        { label: 'Événements audités', value: stats.activity.totalEvents },
        { label: 'Types d’action distincts', value: stats.activity.byAction.length },
        { label: 'Services sources', value: stats.activity.bySourceService.length },
        { label: 'Acteurs actifs', value: stats.activity.topActors.length },
      ]
    : [];

  const degradedNotices = [
    stats?.lab.degraded ? stats.lab.notice : null,
    stats?.nursing.degraded ? stats.nursing.notice : null,
    stats?.activity.degraded ? stats.activity.notice : null,
  ].filter((notice): notice is string => Boolean(notice));

  return (
    <>
      <div className="card filter-toolbar reporting-audit-toolbar reporting-audit-toolbar--compact">
        <div className="filter-toolbar__inner reporting-audit-toolbar__row">
          <div className="field reporting-audit-toolbar__period">
            <label htmlFor="stats-period">Période</label>
            <select
              id="stats-period"
              value={period}
              onChange={(e) => onPeriodChange(e.target.value as ReportingPeriod)}
            >
              <option value="7">7 derniers jours</option>
              <option value="30">30 derniers jours</option>
              <option value="90">90 derniers jours</option>
            </select>
          </div>
          <p className="reporting-audit-events-count muted-text">
            {formatInstantRangeFr(range.from, range.to)}
          </p>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => void handleExport('pdf')}
            disabled={loading || exporting !== null}
          >
            {exporting === 'pdf' ? 'Export PDF…' : 'Export PDF'}
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => void handleExport('xlsx')}
            disabled={loading || exporting !== null}
          >
            {exporting === 'xlsx' ? 'Export Excel…' : 'Export Excel'}
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm reporting-audit-toolbar__refresh"
            onClick={onRefresh}
            disabled={loading}
          >
            Actualiser
          </button>
        </div>
      </div>

      {error ? <div className="error-banner">{error}</div> : null}

      {degradedNotices.length > 0 ? (
        <div className="reporting-notice">
          {degradedNotices.map((notice) => (
            <p key={notice}>{notice}</p>
          ))}
        </div>
      ) : null}

      {loading && !stats ? <LoadingBlock label="Chargement des statistiques…" /> : null}

      {stats ? (
        <div className="reporting-metrics-grid">
          <ReportingMetricCard title="Laboratoire" tone="accent">
            <table className="data-table reporting-metric-table">
              <thead>
                <tr>
                  <th scope="col">Indicateur</th>
                  <th scope="col" className="num">Nombre</th>
                </tr>
              </thead>
              <tbody>
                {labRows.map((row) => (
                  <tr key={row.label}>
                    <td>{row.label}</td>
                    <td className="num reporting-metric-table__emphasis">{formatNumberFr(row.value)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </ReportingMetricCard>

          <ReportingMetricCard title="Soins infirmiers" tone="success">
            <table className="data-table reporting-metric-table">
              <thead>
                <tr>
                  <th scope="col">Indicateur</th>
                  <th scope="col" className="num">Nombre</th>
                </tr>
              </thead>
              <tbody>
                {nursingRows.map((row) => (
                  <tr key={row.label}>
                    <td>{row.label}</td>
                    <td className="num reporting-metric-table__emphasis">{formatNumberFr(row.value)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </ReportingMetricCard>

          <ReportingMetricCard title="Activité auditée" tone="deep">
            <table className="data-table reporting-metric-table">
              <thead>
                <tr>
                  <th scope="col">Indicateur</th>
                  <th scope="col" className="num">Nombre</th>
                </tr>
              </thead>
              <tbody>
                {activityRows.map((row) => (
                  <tr key={row.label}>
                    <td>{row.label}</td>
                    <td className="num reporting-metric-table__emphasis">{formatNumberFr(row.value)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </ReportingMetricCard>
        </div>
      ) : null}

      {!loading && !stats && !error ? (
        <EmptyState
          title="Statistiques indisponibles"
          description="Les services labo, soins ou audit ne répondent pas."
        />
      ) : null}
    </>
  );
}

function AuditJournal({
  period,
  onPeriodChange,
  refreshToken,
  onRefresh,
}: {
  period: ReportingPeriod;
  onPeriodChange: (period: ReportingPeriod) => void;
  refreshToken: number;
  onRefresh: () => void;
}) {
  const [page, setPage] = useState<PageAuditEventResponse | null>(null);
  const [loadedEvents, setLoadedEvents] = useState<AuditEventResponse[]>([]);
  const [pageIndex, setPageIndex] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [rolesCatalog, setRolesCatalog] = useState<RoleOptionResponse[]>([]);
  const [roleByUsername, setRoleByUsername] = useState<Map<string, string>>(new Map());
  const [sortBy, setSortBy] = useState<AuditSortKey>('occurredAt');
  const [sortDir, setSortDir] = useState<TableSortDir>('desc');
  const [openFilterCol, setOpenFilterCol] = useState<AuditFilterColumn | null>(null);
  const [filterDraft, setFilterDraft] = useState<AuditColumnFilters>({ ...emptyAuditFilters });
  const [appliedFilters, setAppliedFilters] = useState<AuditColumnFilters>({ ...emptyAuditFilters });
  const [exporting, setExporting] = useState<'pdf' | 'xlsx' | null>(null);

  const range = useMemo(() => periodToRange(period), [period]);
  const apiSortBy = sortBy === 'role' ? 'occurredAt' : sortBy;

  useEffect(() => {
    void (async () => {
      try {
        const [rolesRes, usersRes] = await Promise.all([
          api.get<RoleOptionResponse[]>('/api/v1/users/roles'),
          api.get<PageUserResponse>(`/api/v1/users?page=0&size=${LIST_FETCH_PAGE_SIZE}`),
        ]);
        setRolesCatalog(rolesRes.data);
        const map = new Map<string, string>();
        for (const user of usersRes.data.content) {
          if (user.roles.length > 0) {
            map.set(user.username, user.roles[0]);
          }
        }
        setRoleByUsername(map);
      } catch {
        setRolesCatalog([]);
        setRoleByUsername(new Map());
      }
    })();
  }, []);

  useEffect(() => {
    setPageIndex(0);
    setLoadedEvents([]);
    setPage(null);
  }, [period, appliedFilters, sortBy, sortDir]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    void (async () => {
      try {
        const params: Record<string, string | number> = {
          from: range.from,
          to: range.to,
          page: pageIndex,
          size: AUDIT_PAGE_SIZE,
          sortBy: apiSortBy,
          sortDir,
        };
        if (appliedFilters.actor.trim()) params.actorUsername = appliedFilters.actor.trim();
        if (appliedFilters.action.trim()) params.action = appliedFilters.action.trim();
        if (appliedFilters.service.trim()) params.sourceService = appliedFilters.service.trim();
        if (appliedFilters.object.trim()) params.resource = appliedFilters.object.trim();

        const { data } = await api.get<PageAuditEventResponse>('/api/v1/audit/events', { params });
        if (!cancelled) {
          setPage(data);
          setLoadedEvents((prev) =>
            pageIndex === 0 ? data.content : [...prev, ...data.content],
          );
        }
      } catch (err) {
        if (!cancelled) {
          if (pageIndex === 0) {
            setPage(null);
            setLoadedEvents([]);
          }
          setError(getApiErrorMessage(err, 'Impossible de charger le journal d’audit.'));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [range.from, range.to, pageIndex, refreshToken, appliedFilters, apiSortBy, sortDir]);

  const roleFilterOptions = useMemo(
    () => rolesCatalog.map((r) => ({ value: r.name, label: r.label })),
    [rolesCatalog],
  );

  const displayEvents = useMemo(() => {
    if (loadedEvents.length === 0) return [];
    let rows: EnrichedAuditEvent[] = loadedEvents.map((event) => ({
      ...event,
      roleLabel: resolveRoleLabel(event, roleByUsername, rolesCatalog),
    }));

    if (appliedFilters.role) {
      const wanted = appliedFilters.role;
      rows = rows.filter((row) => {
        const metaRoles = parseActorRolesFromMetadata(row.metadataJson);
        if (metaRoles.includes(wanted)) return true;
        const userRole = roleByUsername.get(row.actorUsername);
        return userRole === wanted;
      });
    }

    if (sortBy === 'role') {
      rows = [...rows].sort((a, b) => compareStrings(a.roleLabel, b.roleLabel, sortDir));
    }

    return rows;
  }, [loadedEvents, roleByUsername, rolesCatalog, appliedFilters.role, sortBy, sortDir]);

  const total = page?.totalElements ?? 0;
  const hasMorePages = page != null && page.number < page.totalPages - 1;

  function handleAuditTableScroll(event: UIEvent<HTMLDivElement>) {
    if (loading || !hasMorePages) return;
    const el = event.currentTarget;
    const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 72;
    if (nearBottom) {
      setPageIndex(page!.number + 1);
    }
  }
  const showFilterRow = openFilterCol !== null;

  function toggleFilterColumn(col: AuditFilterColumn) {
    setOpenFilterCol((current) => (current === col ? null : col));
    setFilterDraft({ ...appliedFilters });
  }

  function applyFilter(col: AuditFilterColumn) {
    setAppliedFilters((prev) => ({ ...prev, [col]: filterDraft[col] }));
    setOpenFilterCol(null);
  }

  function clearFilter(col: AuditFilterColumn) {
    setFilterDraft((prev) => ({ ...prev, [col]: '' }));
    setAppliedFilters((prev) => ({ ...prev, [col]: '' }));
    setOpenFilterCol(null);
  }

  function clearAllFilters() {
    setFilterDraft({ ...emptyAuditFilters });
    setAppliedFilters({ ...emptyAuditFilters });
    setOpenFilterCol(null);
  }

  function onToggleSort(column: AuditSortKey) {
    toggleTableSort(column, sortBy, setSortBy, setSortDir, defaultSortDirForColumn(column));
  }

  async function handleExport(format: 'pdf' | 'xlsx') {
    setExporting(format);
    setError(null);
    try {
      await downloadActivityExport(format, range.from, range.to);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Export du journal impossible.'));
    } finally {
      setExporting(null);
    }
  }

  const hasActiveFilters = Object.values(appliedFilters).some((v) => v !== '');
  const filterHint = [
    appliedFilters.actor.trim() ? `acteur « ${appliedFilters.actor.trim()} »` : '',
    appliedFilters.action.trim() ? `action « ${appliedFilters.action.trim()} »` : '',
    appliedFilters.object.trim() ? `objet « ${appliedFilters.object.trim()} »` : '',
    appliedFilters.service.trim() ? `service « ${appliedFilters.service.trim()} »` : '',
    appliedFilters.role
      ? `rôle « ${roleLabelFor(rolesCatalog, appliedFilters.role)} » (page courante)`
      : '',
  ]
    .filter(Boolean)
    .join(' · ');

  return (
    <>
      <div className="card filter-toolbar reporting-audit-toolbar reporting-audit-toolbar--compact">
        <div className="filter-toolbar__inner reporting-audit-toolbar__row">
          <div className="field reporting-audit-toolbar__period">
            <label htmlFor="report-period">Période</label>
            <select
              id="report-period"
              value={period}
              onChange={(e) => onPeriodChange(e.target.value as ReportingPeriod)}
            >
              <option value="7">7 derniers jours</option>
              <option value="30">30 derniers jours</option>
              <option value="90">90 derniers jours</option>
            </select>
          </div>
          <p className="reporting-audit-events-count">
            Événements tracés{' '}
            <strong>{loading && !page ? '…' : formatNumberFr(total)}</strong>
          </p>
          {page && page.totalPages > 1 ? (
            <span className="reporting-audit-page-indicator muted-text">
              Page {page.number + 1} / {page.totalPages}
              {loading && hasMorePages ? ' · chargement…' : ''}
            </span>
          ) : null}
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => void handleExport('pdf')}
            disabled={loading || exporting !== null}
          >
            {exporting === 'pdf' ? 'Export PDF…' : 'Export PDF'}
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => void handleExport('xlsx')}
            disabled={loading || exporting !== null}
          >
            {exporting === 'xlsx' ? 'Export Excel…' : 'Export Excel'}
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm reporting-audit-toolbar__refresh"
            onClick={onRefresh}
            disabled={loading}
          >
            Actualiser
          </button>
        </div>
      </div>

      {error ? <div className="error-banner">{error}</div> : null}

      {hasActiveFilters ? (
        <DataTableFilterHint>
          Filtres : {filterHint}
          {' — '}
          <button
            type="button"
            className="btn btn-ghost"
            style={{ padding: 0, verticalAlign: 'baseline' }}
            onClick={clearAllFilters}
          >
            Tout effacer
          </button>
        </DataTableFilterHint>
      ) : null}

      {loading && loadedEvents.length === 0 ? <LoadingBlock label="Chargement du journal…" /> : null}

      {loadedEvents.length > 0 || (page && !loading) ? (
        <div className={`card table-wrap reporting-table-card${loading ? ' reporting-table-card--loading' : ''}`}>
          {loading ? <p className="reporting-table-loading-hint muted-text">Actualisation…</p> : null}
          <ScrollTableRegion onScroll={handleAuditTableScroll}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>
                    <DataTableColumnHeader
                      label="Date et heure"
                      sortable
                      sortActive={sortBy === 'occurredAt'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('occurredAt')}
                    />
                  </th>
                  <th>
                    <DataTableColumnHeader
                      label="Acteur"
                      sortable
                      sortActive={sortBy === 'actor'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('actor')}
                      filterable
                      filterOpen={openFilterCol === 'actor'}
                      filterActive={appliedFilters.actor.trim() !== ''}
                      onToggleFilter={() => toggleFilterColumn('actor')}
                    />
                  </th>
                  <th>
                    <DataTableColumnHeader
                      label="Rôle"
                      sortable
                      sortActive={sortBy === 'role'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('role')}
                      filterable
                      filterOpen={openFilterCol === 'role'}
                      filterActive={appliedFilters.role !== ''}
                      onToggleFilter={() => toggleFilterColumn('role')}
                    />
                  </th>
                  <th>
                    <DataTableColumnHeader
                      label="Action"
                      sortable
                      sortActive={sortBy === 'action'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('action')}
                      filterable
                      filterOpen={openFilterCol === 'action'}
                      filterActive={appliedFilters.action.trim() !== ''}
                      onToggleFilter={() => toggleFilterColumn('action')}
                    />
                  </th>
                  <th>
                    <DataTableColumnHeader
                      label="Objet"
                      sortable
                      sortActive={sortBy === 'object'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('object')}
                      filterable
                      filterOpen={openFilterCol === 'object'}
                      filterActive={appliedFilters.object.trim() !== ''}
                      onToggleFilter={() => toggleFilterColumn('object')}
                    />
                  </th>
                  <th>
                    <DataTableColumnHeader
                      label="Service"
                      sortable
                      sortActive={sortBy === 'service'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('service')}
                      filterable
                      filterOpen={openFilterCol === 'service'}
                      filterActive={appliedFilters.service.trim() !== ''}
                      onToggleFilter={() => toggleFilterColumn('service')}
                    />
                  </th>
                </tr>
                {showFilterRow ? (
                  <tr className="table-filter-row">
                    <th />
                    <th>
                      <DataTableFilterCell
                        open={openFilterCol === 'actor'}
                        value={filterDraft.actor}
                        onChange={(v) => setFilterDraft((d) => ({ ...d, actor: v }))}
                        onApply={() => applyFilter('actor')}
                        onClear={() => clearFilter('actor')}
                        placeholder="Identifiant…"
                      />
                    </th>
                    <th>
                      <DataTableFilterSelectCell
                        open={openFilterCol === 'role'}
                        value={filterDraft.role}
                        onChange={(v) => setFilterDraft((d) => ({ ...d, role: v }))}
                        onApply={() => applyFilter('role')}
                        onClear={() => clearFilter('role')}
                        options={roleFilterOptions}
                        ariaLabel="Filtrer par rôle"
                        allLabel="Tous les rôles"
                      />
                    </th>
                    <th>
                      <DataTableFilterCell
                        open={openFilterCol === 'action'}
                        value={filterDraft.action}
                        onChange={(v) => setFilterDraft((d) => ({ ...d, action: v }))}
                        onApply={() => applyFilter('action')}
                        onClear={() => clearFilter('action')}
                        placeholder="ex. LOGIN, PATIENT…"
                      />
                    </th>
                    <th>
                      <DataTableFilterCell
                        open={openFilterCol === 'object'}
                        value={filterDraft.object}
                        onChange={(v) => setFilterDraft((d) => ({ ...d, object: v }))}
                        onApply={() => applyFilter('object')}
                        onClear={() => clearFilter('object')}
                        placeholder="Type ou n° ressource…"
                      />
                    </th>
                    <th>
                      <DataTableFilterCell
                        open={openFilterCol === 'service'}
                        value={filterDraft.service}
                        onChange={(v) => setFilterDraft((d) => ({ ...d, service: v }))}
                        onApply={() => applyFilter('service')}
                        onClear={() => clearFilter('service')}
                        placeholder="ex. identity, patient…"
                      />
                    </th>
                  </tr>
                ) : null}
              </thead>
              <tbody>
                {displayEvents.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="muted-text">
                      Aucun événement pour ces critères.
                    </td>
                  </tr>
                ) : (
                  displayEvents.map((event) => {
                    const meta = formatAuditMetadataHint(event.metadataJson);
                    return (
                      <tr key={event.id}>
                        <td className="reporting-audit-cell--nowrap">
                          {formatAuditDateTime(event.occurredAt)}
                        </td>
                        <td>{event.actorUsername}</td>
                        <td>{event.roleLabel}</td>
                        <td>{formatAuditActionLabel(event.action)}</td>
                        <td title={meta ?? undefined}>
                          {formatAuditResource(event.resourceType, event.resourceId)}
                        </td>
                        <td className="muted-text">{formatServiceLabel(event.sourceService)}</td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </ScrollTableRegion>
        </div>
      ) : null}
    </>
  );
}

export function ReportingPage() {
  const [searchParams] = useSearchParams();
  const activeTab = resolveReportingTab(searchParams);
  const [period, setPeriod] = useState<ReportingPeriod>('30');
  const [refreshToken, setRefreshToken] = useState(0);

  const [occupancy, setOccupancy] = useState<OccupancyStatsValue | null>(null);
  const [volumes, setVolumes] = useState<PlatformVolumesValue | null>(null);
  const [occupancyLoading, setOccupancyLoading] = useState(false);
  const [occupancyError, setOccupancyError] = useState<string | null>(null);
  const [volumesError, setVolumesError] = useState<string | null>(null);

  const loadOccupancy = useCallback(async () => {
    setOccupancyLoading(true);
    setOccupancyError(null);
    setVolumesError(null);
    const [occupancyResult, volumesResult] = await Promise.allSettled([
      api.get<MetricResponse>('/api/v1/stats/occupancy'),
      api.get<MetricResponse>('/api/v1/stats/volumes'),
    ]);

    if (occupancyResult.status === 'fulfilled') {
      const parsedOccupancy = parseOccupancy(occupancyResult.value.data.value);
      setOccupancy(parsedOccupancy);
      if (!parsedOccupancy) {
        setOccupancyError('Occupation des lits indisponible.');
      }
    } else {
      setOccupancy(null);
      setOccupancyError(
        getApiErrorMessage(occupancyResult.reason, 'Occupation des lits indisponible.'),
      );
    }

    if (volumesResult.status === 'fulfilled') {
      const parsed = parseVolumes(volumesResult.value.data.value);
      setVolumes(parsed);
      if (!parsed) {
        setVolumesError('Statistiques d’admission indisponibles.');
      }
    } else {
      setVolumes(null);
      setVolumesError(
        getApiErrorMessage(volumesResult.reason, 'Statistiques d’admission indisponibles.'),
      );
    }

    setOccupancyLoading(false);
  }, []);

  useEffect(() => {
    if (activeTab === 'occupation') {
      void loadOccupancy();
    }
  }, [activeTab, loadOccupancy, refreshToken]);

  function handleRefresh() {
    setRefreshToken((t) => t + 1);
    if (activeTab === 'occupation') {
      void loadOccupancy();
    }
  }

  return (
    <>
      <PageHeader title="Reporting" />

      <ReportingTabs />

      {activeTab === 'occupation' ? (
        <div className="reporting-panel">
            <section aria-label="Occupation des lits">
              {occupancyLoading && !occupancy && !volumes ? (
                <LoadingBlock label="Chargement des indicateurs…" />
              ) : null}
              {occupancyError ? <div className="error-banner">{occupancyError}</div> : null}
              {volumesError ? <div className="error-banner">{volumesError}</div> : null}
              {!occupancyLoading && (occupancy || volumes) ? (
                <OccupationReportingPanel occupancy={occupancy} volumes={volumes} />
              ) : null}
              {!occupancyLoading && !occupancyError && !occupancy && !volumesError && !volumes ? (
                <EmptyState
                  title="Indicateurs indisponibles"
                  description="Les services ne répondent pas ou n’ont pas renvoyé de statistiques."
                />
              ) : null}
            </section>
        </div>
      ) : null}

      {activeTab === 'stats' ? (
        <div className="reporting-panel">
          <section aria-label="Statistiques labo et soins">
            <OperationalStatsPanel
              key={period}
              period={period}
              onPeriodChange={setPeriod}
              refreshToken={refreshToken}
              onRefresh={handleRefresh}
            />
          </section>
        </div>
      ) : null}

      {activeTab === 'audit' ? (
        <div className="reporting-panel">
          <section aria-label="Journal d'audit">
            <AuditJournal
              key={period}
              period={period}
              onPeriodChange={setPeriod}
              refreshToken={refreshToken}
              onRefresh={handleRefresh}
            />
          </section>
        </div>
      ) : null}
    </>
  );
}
