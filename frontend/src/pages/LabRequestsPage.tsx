import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import { hasRole } from '../auth/roles';
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

export function LabRequestsPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const canCreate = hasRole(user, 'ROLE_MEDECIN');

  const [page, setPage] = useState<PageExamRequestResponse | null>(null);
  const [patientsById, setPatientsById] = useState<Record<number, PatientResponse>>({});
  const [statusFilter, setStatusFilter] = useState<'' | ExamRequestStatus>('');
  const [sortBy, setSortBy] = useState<LabSortKey>('requestedAt');
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
  }, [statusFilter, reloadKey]);

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
    const rows = [...(page?.content ?? [])];
    rows.sort((a, b) => {
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
  }, [page, patientsById, sortBy, sortDir]);

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
      <PageHeader title="Laboratoire" subtitle="Demandes d'examens — prescription, prélèvement et résultats">
        {canCreate ? (
          <button type="button" className="btn btn-primary" onClick={openCreateDrawer}>
            Nouvelle demande
          </button>
        ) : null}
      </PageHeader>

      <div className="toolbar-row">
        <label htmlFor="lab-status-filter">Statut</label>
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

      {error && <p className="form-error">{error}</p>}
      {loading && <LoadingBlock label="Chargement des demandes…" />}

      {!loading && page && (
        <ScrollTableRegion>
          <table className="data-table">
            <thead>
              <tr>
                <DataTableColumnHeader
                  label="N°"
                  sortActive={sortBy === 'id'}
                  sortDir={sortDir}
                  onSort={() => handleSort('id')}
                />
                <DataTableColumnHeader
                  label="Patient"
                  sortActive={sortBy === 'patient'}
                  sortDir={sortDir}
                  onSort={() => handleSort('patient')}
                />
                <th>Examens</th>
                <DataTableColumnHeader
                  label="Urgence"
                  sortActive={sortBy === 'urgency'}
                  sortDir={sortDir}
                  onSort={() => handleSort('urgency')}
                />
                <DataTableColumnHeader
                  label="Statut"
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
              </tr>
            </thead>
            <tbody>
              {sortedRows.map((item) => (
                <tr
                  key={item.id}
                  className="data-table__row--clickable"
                  onClick={() => navigate(`/lab/requests/${item.id}`)}
                >
                  <td>{item.id}</td>
                  <td>{patientDisplayName(patientsById[item.patientId], item.patientId)}</td>
                  <td>{examTypesSummary(item.examTypes.map((t) => t.name))}</td>
                  <td>{examUrgencyLabels[item.urgency]}</td>
                  <td>{examRequestStatusLabels[item.status]}</td>
                  <td>{formatLabInstant(item.requestedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <TableResultFooter
            totalElements={page.totalElements}
            displayedCount={sortedRows.length}
            itemLabelPlural="demande(s)"
          />
        </ScrollTableRegion>
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
