import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import { hasRole, isLabPortalUser } from '../auth/roles';
import { LabQueueKpiRow } from '../components/LabQueueKpiRow';
import { useLabQueueStats } from '../hooks/useLabQueueStats';
import type {
  ExamRequestCreateRequest,
  ExamRequestResponse,
  ExamRequestStatus,
  ExamTypeResponse,
  ExamUrgency,
  PageExamRequestResponse,
  PagePatientResponse,
  PatientResponse,
} from '../api/types';
import { DataTableColumnHeader } from '../components/DataTableColumnHeader';
import { LoadingBlock } from '../components/ui/LoadingBlock';
import { PageHeader } from '../components/ui/PageHeader';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';
import {
  examRequestStatusLabels,
  examTypesSummary,
  examUrgencyLabels,
  formatLabInstant,
  labDoctorFollowUpLabel,
  labNextActionLabel,
  labStatusBadgeClass,
} from '../utils/labDisplay';
import {
  compareNumbers,
  compareStrings,
  defaultSortDirForColumn,
  toggleTableSort,
  type TableSortDir,
} from '../utils/tableSort';

type LabSortKey = 'id' | 'patient' | 'status' | 'urgency' | 'requestedAt';

function patientDisplayName(patient: PatientResponse | undefined, patientId: number): string {
  if (!patient) return String(patientId);
  const name = `${patient.firstName} ${patient.lastName}`.trim();
  return name || `Patient ${patientId}`;
}

const statusFilterOptions: { value: '' | ExamRequestStatus; label: string }[] = [
  { value: '', label: 'Tous les statuts' },
  { value: 'PENDING', label: examRequestStatusLabels.PENDING },
  { value: 'SPECIMEN_COLLECTED', label: examRequestStatusLabels.SPECIMEN_COLLECTED },
  { value: 'RESULTS_AVAILABLE', label: examRequestStatusLabels.RESULTS_AVAILABLE },
  { value: 'POSTPONED', label: examRequestStatusLabels.POSTPONED },
];

const VALID_STATUSES: ExamRequestStatus[] = [
  'PENDING',
  'SPECIMEN_COLLECTED',
  'RESULTS_AVAILABLE',
  'POSTPONED',
];

function parseStatusParam(value: string | null, isLaborantin: boolean): '' | ExamRequestStatus {
  if (value && VALID_STATUSES.includes(value as ExamRequestStatus)) {
    return value as ExamRequestStatus;
  }
  return isLaborantin ? 'PENDING' : '';
}

function actionBadgeClass(status: ExamRequestStatus): string {
  switch (status) {
    case 'PENDING':
      return 'lab-action-badge lab-action-badge--pending';
    case 'SPECIMEN_COLLECTED':
      return 'lab-action-badge lab-action-badge--specimen';
    case 'RESULTS_AVAILABLE':
      return 'lab-action-badge lab-action-badge--done';
    default:
      return 'lab-action-badge lab-action-badge--muted';
  }
}

function filterSummaryLabel(
  statusFilter: '' | ExamRequestStatus,
  mineOnly: boolean,
  urgentOnly: boolean,
  isLaborantin: boolean,
): string {
  const parts: string[] = [];
  if (mineOnly) parts.push('mes demandes');
  if (urgentOnly) parts.push('urgentes uniquement');
  if (statusFilter) parts.push(examRequestStatusLabels[statusFilter].toLowerCase());
  if (parts.length === 0) {
    return isLaborantin ? 'Toutes les demandes' : 'Toutes les demandes — tous prescripteurs';
  }
  return parts.map((p) => p.charAt(0).toUpperCase() + p.slice(1)).join(' · ');
}

export function LabRequestsPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { user } = useAuth();
  const isLaborantin = isLabPortalUser(user);
  const canCreate = hasRole(user, 'ROLE_MEDECIN');
  /** File de traitement (prélèvement + résultats) : laborantin uniquement. */
  const showQueueTools = isLaborantin;
  const showDoctorTools = canCreate;

  const mineOnly = searchParams.get('mine') === '1';
  const statusFilter = parseStatusParam(searchParams.get('status'), isLaborantin);
  const urgentOnly = searchParams.get('urgent') === '1';
  const isDoctorView = showDoctorTools && !isLaborantin;
  const showUrgencyColumn = !isDoctorView;

  const { stats: queueStats, loading: queueStatsLoading } = useLabQueueStats(showQueueTools);

  const [page, setPage] = useState<PageExamRequestResponse | null>(null);
  const [patientsById, setPatientsById] = useState<Record<number, PatientResponse>>({});
  const [sortBy, setSortBy] = useState<LabSortKey>(isLaborantin ? 'urgency' : 'requestedAt');
  const [sortDir, setSortDir] = useState<TableSortDir>('desc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  const [showCreateDrawer, setShowCreateDrawer] = useState(false);
  const [examTypes, setExamTypes] = useState<ExamTypeResponse[]>([]);
  const [createPatientQuery, setCreatePatientQuery] = useState('');
  const [createPatientOptions, setCreatePatientOptions] = useState<PatientResponse[]>([]);
  const [createPatientLoading, setCreatePatientLoading] = useState(false);
  const [selectedPatient, setSelectedPatient] = useState<PatientResponse | null>(null);
  const [urgency, setUrgency] = useState<ExamUrgency>('NORMAL');
  const [comment, setComment] = useState('');
  const [selectedTypeIds, setSelectedTypeIds] = useState<number[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    const params: Record<string, string | number> = { page: 0, size: LIST_FETCH_PAGE_SIZE };
    if (statusFilter) params.status = statusFilter;
    if (mineOnly && user?.id != null) params.doctorId = user.id;

    api
      .get<PageExamRequestResponse>('/api/v1/lab/exam-requests', { params })
      .then(({ data }) => {
        if (!cancelled) setPage(data);
      })
      .catch((err) => {
        if (!cancelled) setError(getApiErrorMessage(err, 'Impossible de charger les demandes laboratoire.'));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [statusFilter, mineOnly, user?.id, reloadKey]);

  function setStatusFilter(next: '' | ExamRequestStatus) {
    const params = new URLSearchParams(searchParams);
    if (next) {
      params.set('status', next);
    } else {
      params.delete('status');
    }
    params.delete('urgent');
    setSearchParams(params, { replace: true });
  }

  function setMineOnly(active: boolean) {
    const params = new URLSearchParams(searchParams);
    if (active) {
      params.set('mine', '1');
    } else {
      params.delete('mine');
    }
    setSearchParams(params, { replace: true });
  }

  function setUrgentOnly(active: boolean) {
    const params = new URLSearchParams(searchParams);
    params.set('status', 'PENDING');
    if (active) {
      params.set('urgent', '1');
    } else {
      params.delete('urgent');
    }
    setSearchParams(params, { replace: true });
  }

  useEffect(() => {
    const ids = [...new Set((page?.content ?? []).map((item) => item.patientId))];
    const missing = ids.filter((id) => patientsById[id] == null);
    if (missing.length === 0) return;

    let cancelled = false;
    void Promise.all(
      missing.map((id) =>
        api.get<PatientResponse>(`/api/v1/patients/${id}`).then((res) => res.data),
      ),
    )
      .then((patients) => {
        if (cancelled) return;
        setPatientsById((prev) => {
          const next = { ...prev };
          for (const patient of patients) next[patient.id] = patient;
          return next;
        });
      })
      .catch(() => {
        /* noms manquants non bloquants */
      });
    return () => {
      cancelled = true;
    };
  }, [page, patientsById]);

  useEffect(() => {
    if (!showCreateDrawer) return;
    let cancelled = false;
    api
      .get<ExamTypeResponse[]>('/api/v1/lab/exam-types')
      .then(({ data }) => {
        if (!cancelled) setExamTypes(data);
      })
      .catch(() => {
        if (!cancelled) setExamTypes([]);
      });
    return () => {
      cancelled = true;
    };
  }, [showCreateDrawer]);

  useEffect(() => {
    if (!showCreateDrawer || createPatientQuery.trim().length < 2) {
      setCreatePatientOptions([]);
      return;
    }
    let cancelled = false;
    setCreatePatientLoading(true);
    api
      .get<PagePatientResponse>('/api/v1/patients', {
        params: { query: createPatientQuery.trim(), page: 0, size: 8 },
      })
      .then(({ data }) => {
        if (!cancelled) setCreatePatientOptions(data.content ?? []);
      })
      .catch(() => {
        if (!cancelled) setCreatePatientOptions([]);
      })
      .finally(() => {
        if (!cancelled) setCreatePatientLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [createPatientQuery, showCreateDrawer]);

  const sortedRows = useMemo(() => {
    let rows = [...(page?.content ?? [])];
    if (urgentOnly) {
      rows = rows.filter((item) => item.urgency === 'URGENT');
    }
    rows.sort((a, b) => {
      if (isLaborantin && sortBy === 'urgency') {
        const urgencyRank = (u: ExamRequestResponse['urgency']) => (u === 'URGENT' ? 0 : 1);
        const byUrgency = urgencyRank(a.urgency) - urgencyRank(b.urgency);
        if (byUrgency !== 0) return byUrgency;
        return compareStrings(a.requestedAt, b.requestedAt, 'asc');
      }
      switch (sortBy) {
        case 'id':
          return compareNumbers(a.id, b.id, sortDir);
        case 'patient':
          return compareStrings(
            patientDisplayName(patientsById[a.patientId], a.patientId),
            patientDisplayName(patientsById[b.patientId], b.patientId),
            sortDir,
          );
        case 'status':
          return compareStrings(a.status, b.status, sortDir);
        case 'urgency':
          return compareStrings(a.urgency, b.urgency, sortDir);
        case 'requestedAt':
          return compareStrings(a.requestedAt, b.requestedAt, sortDir);
        default:
          return 0;
      }
    });
    return rows;
  }, [page, patientsById, sortBy, sortDir, urgentOnly, isLaborantin]);

  function handleSort(column: LabSortKey) {
    toggleTableSort(column, sortBy, setSortBy, setSortDir, defaultSortDirForColumn(column));
  }

  function openCreateDrawer() {
    setShowCreateDrawer(true);
    setCreateError(null);
    setSelectedPatient(null);
    setCreatePatientQuery('');
    setUrgency('NORMAL');
    setComment('');
    setSelectedTypeIds([]);
  }

  function toggleExamType(id: number) {
    setSelectedTypeIds((prev) =>
      prev.includes(id) ? prev.filter((value) => value !== id) : [...prev, id],
    );
  }

  async function submitCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!user || !selectedPatient) {
      setCreateError('Sélectionnez un patient.');
      return;
    }
    if (selectedTypeIds.length === 0) {
      setCreateError('Sélectionnez au moins un type d\'examen.');
      return;
    }
    setSubmitting(true);
    setCreateError(null);
    const payload: ExamRequestCreateRequest = {
      patientId: selectedPatient.id,
      doctorId: user.id,
      urgency,
      comment: comment.trim() || null,
      examTypeIds: selectedTypeIds,
    };
    try {
      const { data } = await api.post<ExamRequestResponse>('/api/v1/lab/exam-requests', payload);
      setShowCreateDrawer(false);
      setReloadKey((k) => k + 1);
      navigate(`/lab/requests/${data.id}`);
    } catch (err) {
      setCreateError(getApiErrorMessage(err, 'Impossible de créer la demande.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="page-stack">
      <PageHeader
        title="Laboratoire"
        subtitle={
          isLaborantin
            ? 'File d\'attente — prélèvements et publication des résultats'
            : showDoctorTools
              ? 'Prescription et suivi de vos demandes d\'examens'
              : 'Demandes d\'examens — prescription, prélèvement et résultats'
        }
      >
        {canCreate ? (
          <button type="button" className="btn btn-primary" onClick={openCreateDrawer}>
            Nouvelle demande
          </button>
        ) : null}
      </PageHeader>

      {showQueueTools && (
        <>
          <LabQueueKpiRow
            stats={queueStats}
            loading={queueStatsLoading}
            activeStatus={statusFilter || undefined}
            urgentActive={urgentOnly}
          />
          <p className="lab-workflow-banner">
            <strong>Parcours :</strong> demande en attente → enregistrer le prélèvement → saisir et
            publier les résultats → résultats disponibles pour le médecin.
          </p>
        </>
      )}

      <div className="card filter-toolbar">
        <div className="filter-toolbar__inner filter-toolbar__inner--stacked">
          <div className="filter-toolbar__form filter-toolbar__form--row lab-requests-filters">
            <div className="field field--status">
              <label htmlFor="lab-status-filter">Filtrer par statut</label>
              <select
                id="lab-status-filter"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value as '' | ExamRequestStatus)}
              >
                {statusFilterOptions.map((opt) => (
                  <option key={opt.value || 'all'} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            {(showDoctorTools || (isLaborantin && statusFilter === 'PENDING')) && (
              <div className="field field--toggle">
                <label>Options</label>
                <div className="lab-filter-toggles">
                  {showDoctorTools && (
                    <button
                      type="button"
                      className={`lab-filter-toggle${mineOnly ? ' is-active' : ''}`}
                      onClick={() => setMineOnly(!mineOnly)}
                      aria-pressed={mineOnly}
                    >
                      Mes demandes
                    </button>
                  )}
                  {isLaborantin && statusFilter === 'PENDING' && (
                    <button
                      type="button"
                      className={`lab-filter-toggle${urgentOnly ? ' is-active' : ''}`}
                      onClick={() => setUrgentOnly(!urgentOnly)}
                      aria-pressed={urgentOnly}
                    >
                      Urgentes uniquement
                    </button>
                  )}
                </div>
              </div>
            )}
          </div>

          <p className="filter-toolbar__summary">
            Affichage : <strong>{filterSummaryLabel(statusFilter, mineOnly, urgentOnly, isLaborantin)}</strong>
          </p>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}
      {loading && <LoadingBlock label="Chargement des demandes…" />}

      {!loading && page && (
        <div className="card table-wrap">
          <ScrollTableRegion>
            <table className="data-table">
              <thead>
                <tr>
                  <th className="data-table-col--id">
                    <DataTableColumnHeader
                      label="N°"
                      sortActive={sortBy === 'id'}
                      sortDir={sortDir}
                      onSort={() => handleSort('id')}
                    />
                  </th>
                  <DataTableColumnHeader
                    label="Patient"
                    sortActive={sortBy === 'patient'}
                    sortDir={sortDir}
                    onSort={() => handleSort('patient')}
                  />
                  <th>Examens</th>
                  {showUrgencyColumn && (
                    <DataTableColumnHeader
                      label="Urgence"
                      sortActive={sortBy === 'urgency'}
                      sortDir={sortDir}
                      onSort={() => handleSort('urgency')}
                    />
                  )}
                  <DataTableColumnHeader
                    label="État"
                    sortActive={sortBy === 'status'}
                    sortDir={sortDir}
                    onSort={() => handleSort('status')}
                  />
                  <DataTableColumnHeader
                    label="Demandé le"
                    sortActive={sortBy === 'requestedAt'}
                    sortDir={sortDir}
                    onSort={() => handleSort('requestedAt')}
                  />
                  {isLaborantin && <th>Action</th>}
                </tr>
              </thead>
              <tbody>
                {sortedRows.length === 0 ? (
                  <tr>
                    <td
                      colSpan={5 + (showUrgencyColumn ? 1 : 0) + (isLaborantin ? 1 : 0)}
                      className="empty-cell"
                    >
                    {urgentOnly
                      ? 'Aucune demande urgente en attente.'
                      : mineOnly && statusFilter === 'RESULTS_AVAILABLE'
                        ? 'Aucun résultat à consulter pour vos demandes.'
                        : statusFilter === 'PENDING'
                          ? 'Aucune demande en attente — file vide.'
                          : 'Aucune demande pour ce filtre.'}
                  </td>
                </tr>
              ) : (
                sortedRows.map((item) => (
                  <tr
                    key={item.id}
                    className={`data-table-row--clickable${
                      showDoctorTools && item.status === 'RESULTS_AVAILABLE' ? ' data-table__row--highlight' : ''
                    }`}
                    onClick={() => navigate(`/lab/requests/${item.id}`)}
                  >
                    <td className="data-table-col--id">{item.id}</td>
                    <td>{patientDisplayName(patientsById[item.patientId], item.patientId)}</td>
                    <td className="lab-table-exams">{examTypesSummary(item.examTypes.map((t) => t.name))}</td>
                    {showUrgencyColumn && (
                      <td>
                        {item.urgency === 'URGENT' ? (
                          <span className="lab-urgency-pill lab-urgency-pill--urgent">
                            {examUrgencyLabels[item.urgency]}
                          </span>
                        ) : (
                          examUrgencyLabels[item.urgency]
                        )}
                      </td>
                    )}
                    <td>
                      <div className="lab-table-state">
                        <span className={labStatusBadgeClass(item.status)}>
                          {examRequestStatusLabels[item.status]}
                        </span>
                        {isDoctorView && item.urgency === 'URGENT' && (
                          <span className="lab-urgency-pill lab-urgency-pill--urgent">
                            {examUrgencyLabels[item.urgency]}
                          </span>
                        )}
                        {isDoctorView && (
                          <span className="lab-table-state__hint">{labDoctorFollowUpLabel(item.status)}</span>
                        )}
                      </div>
                    </td>
                    <td>{formatLabInstant(item.requestedAt)}</td>
                    {isLaborantin && (
                      <td>
                        <span className={actionBadgeClass(item.status)}>{labNextActionLabel(item.status)}</span>
                      </td>
                    )}
                  </tr>
                ))
              )}
            </tbody>
          </table>
          </ScrollTableRegion>
          <TableResultFooter
            totalElements={page.totalElements}
            displayedCount={sortedRows.length}
            itemLabelPlural="demande(s)"
          />
        </div>
      )}

      {showCreateDrawer && (
        <div className="drawer-backdrop" role="presentation" onClick={() => setShowCreateDrawer(false)}>
          <aside
            className="drawer-panel"
            role="dialog"
            aria-labelledby="lab-create-title"
            onClick={(e) => e.stopPropagation()}
          >
            <header className="drawer-panel__header">
              <h2 id="lab-create-title">Nouvelle demande d&apos;examen</h2>
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => setShowCreateDrawer(false)}>
                Fermer
              </button>
            </header>
            <form className="drawer-panel__body form-stack" onSubmit={(e) => void submitCreate(e)}>
              <label htmlFor="lab-patient-search">Patient *</label>
              <input
                id="lab-patient-search"
                value={createPatientQuery}
                onChange={(e) => {
                  setCreatePatientQuery(e.target.value);
                  setSelectedPatient(null);
                }}
                placeholder="Rechercher par nom ou n° dossier"
                autoComplete="off"
              />
              {createPatientLoading && <p className="hint">Recherche…</p>}
              {selectedPatient && (
                <p className="hint">
                  Sélectionné : {patientDisplayName(selectedPatient, selectedPatient.id)}
                </p>
              )}
              {!selectedPatient && createPatientOptions.length > 0 && (
                <ul className="patient-picker">
                  {createPatientOptions.map((patient) => (
                    <li key={patient.id}>
                      <button
                        type="button"
                        className="patient-picker__item"
                        onClick={() => {
                          setSelectedPatient(patient);
                          setCreatePatientQuery(patientDisplayName(patient, patient.id));
                          setCreatePatientOptions([]);
                        }}
                      >
                        {patientDisplayName(patient, patient.id)}
                        {patient.dossierNumber ? ` — ${patient.dossierNumber}` : ''}
                      </button>
                    </li>
                  ))}
                </ul>
              )}

              <label htmlFor="lab-urgency">Urgence</label>
              <select
                id="lab-urgency"
                value={urgency}
                onChange={(e) => setUrgency(e.target.value as ExamUrgency)}
              >
                <option value="NORMAL">{examUrgencyLabels.NORMAL}</option>
                <option value="URGENT">{examUrgencyLabels.URGENT}</option>
              </select>

              <fieldset>
                <legend>Types d&apos;examen *</legend>
                {examTypes.length === 0 ? (
                  <p className="hint">Aucun type disponible — démarrez lab-service.</p>
                ) : (
                  <ul className="checkbox-list">
                    {examTypes.map((type) => (
                      <li key={type.id}>
                        <label>
                          <input
                            type="checkbox"
                            checked={selectedTypeIds.includes(type.id)}
                            onChange={() => toggleExamType(type.id)}
                          />
                          {type.name}
                        </label>
                      </li>
                    ))}
                  </ul>
                )}
              </fieldset>

              <label htmlFor="lab-comment">Commentaire</label>
              <textarea id="lab-comment" rows={3} value={comment} onChange={(e) => setComment(e.target.value)} />

              {createError && <p className="form-error">{createError}</p>}

              <div className="drawer-panel__actions">
                <button type="button" className="btn btn-ghost" onClick={() => setShowCreateDrawer(false)}>
                  Annuler
                </button>
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? 'Enregistrement…' : 'Créer la demande'}
                </button>
              </div>
            </form>
          </aside>
        </div>
      )}
    </div>
  );
}
