import axios from 'axios';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { PagePatientResponse, PageUrgenceResponse, PatientResponse, UrgenceCreateRequest, UrgenceResponse, UrgenceStatus } from '../api/types';
import { DataTableColumnHeader } from '../components/DataTableColumnHeader';
import { Drawer } from '../components/ui/Drawer';
import { EmptyState } from '../components/ui/EmptyState';
import { LoadingBlock } from '../components/ui/LoadingBlock';
import { PageHeader } from '../components/ui/PageHeader';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';
import { isPatientDeceased } from '../utils/patientStatus';
import {
  compareNumbers,
  compareStrings,
  defaultSortDirForColumn,
  toggleTableSort,
  type TableSortDir,
} from '../utils/tableSort';

function patientDisplayName(patient: PatientResponse | undefined, patientId: number): string {
  if (!patient) return String(patientId);
  const name = `${patient.firstName} ${patient.lastName}`.trim();
  return name || `Patient ${patientId}`;
}

type UrgenceSortKey = 'id' | 'patient' | 'priority' | 'status';

const statusLabels: Record<UrgenceStatus, string> = {
  EN_ATTENTE_TRIAGE: 'En attente triage',
  EN_COURS: 'En cours',
  ORIENTE: 'Transféré',
  CLOTURE: 'Clôturé',
};

function triageLevelLabel(value: string | null | undefined): string {
  if (!value) return '-';
  if (value === 'TRES_URGENT') return 'Très urgent';
  if (value === 'URGENT') return 'Urgent';
  if (value === 'MOYEN') return 'Moyen';
  return value;
}

function triageLevelBadgeClass(value: string | null | undefined): string {
  if (!value) return '';
  const normalized = value.toLowerCase();
  if (normalized === 'tres_urgent') return 'urgence-triage-badge urgence-triage-badge--tres_urgent';
  if (normalized === 'urgent') return 'urgence-triage-badge urgence-triage-badge--urgent';
  if (normalized === 'moyen') return 'urgence-triage-badge urgence-triage-badge--moyen';
  return 'urgence-triage-badge urgence-triage-badge--unknown';
}

const priorityOptions = [
  { value: 'P1', label: 'Très urgent' },
  { value: 'P2', label: 'Urgent' },
  { value: 'P3', label: 'Moyen' },
] as const;

function priorityLabel(value: string | null | undefined): string {
  if (!value) return '-';
  if (value === 'TRES_URGENT' || value === 'P1') return 'Très urgent';
  if (value === 'URGENT' || value === 'P2') return 'Urgent';
  if (value === 'MOYEN' || value === 'P3') return 'Moyen';
  return value;
}

function priorityBadgeClass(value: string | null | undefined): string {
  if (!value) return 'urgence-priority-badge';
  const normalized = value.toLowerCase();
  if (normalized === 'tres_urgent' || normalized === 'p1') {
    return 'urgence-priority-badge urgence-priority-badge--tres_urgent';
  }
  if (normalized === 'urgent' || normalized === 'p2') {
    return 'urgence-priority-badge urgence-priority-badge--urgent';
  }
  if (normalized === 'moyen' || normalized === 'p3') {
    return 'urgence-priority-badge urgence-priority-badge--moyen';
  }
  return 'urgence-priority-badge';
}

export function UrgencesPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState<PageUrgenceResponse | null>(null);
  const [patientsById, setPatientsById] = useState<Record<number, PatientResponse>>({});
  const [searchInput, setSearchInput] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');
  const [matchingPatientIds, setMatchingPatientIds] = useState<Set<number> | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [sortBy, setSortBy] = useState<UrgenceSortKey>('id');
  const [sortDir, setSortDir] = useState<TableSortDir>('desc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateDrawer, setShowCreateDrawer] = useState(false);
  const [createPatientQuery, setCreatePatientQuery] = useState('');
  const [createPatientOptions, setCreatePatientOptions] = useState<PatientResponse[]>([]);
  const [createPatientLoading, setCreatePatientLoading] = useState(false);
  const [selectedPatient, setSelectedPatient] = useState<PatientResponse | null>(null);
  const [motif, setMotif] = useState('');
  const [priority, setPriority] = useState('P2');
  const [submitting, setSubmitting] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  useEffect(() => {
    void loadUrgences();
  }, [sortBy, sortDir, reloadKey]);

  useEffect(() => {
    const query = appliedSearch.trim();
    if (!query) {
      setMatchingPatientIds(null);
      return;
    }
    let cancelled = false;
    const params = new URLSearchParams({ page: '0', size: '200', sortBy: 'id', sortDir: 'desc', query });
    void api
      .get<PagePatientResponse>(`/api/v1/patients?${params}`)
      .then((res) => {
        if (!cancelled) {
          setMatchingPatientIds(new Set(res.data.content.map((p) => p.id)));
        }
      })
      .catch(() => {
        if (!cancelled) {
          setMatchingPatientIds(new Set());
        }
      });
    return () => {
      cancelled = true;
    };
  }, [appliedSearch]);

  useEffect(() => {
    if (!showCreateDrawer) return;
    const query = createPatientQuery.trim();
    if (query.length < 2) {
      setCreatePatientOptions([]);
      return;
    }
    setCreatePatientLoading(true);
    const timer = window.setTimeout(() => {
      const params = new URLSearchParams({ page: '0', size: '8', sortBy: 'id', sortDir: 'desc', query });
      void api
        .get<PagePatientResponse>(`/api/v1/patients?${params}`)
        .then((res) => setCreatePatientOptions(res.data.content))
        .catch(() => setCreatePatientOptions([]))
        .finally(() => setCreatePatientLoading(false));
    }, 250);
    return () => {
      window.clearTimeout(timer);
      setCreatePatientLoading(false);
    };
  }, [createPatientQuery, showCreateDrawer]);

  async function loadUrgences() {
    setLoading(true);
    setError(null);
    try {
      const backendSortBy = sortBy === 'patient' ? 'patientId' : sortBy;
      const params = new URLSearchParams({
        sortBy: backendSortBy,
        sortDir,
        page: '0',
        size: String(LIST_FETCH_PAGE_SIZE),
      });
      const { data } = await api.get<PageUrgenceResponse>(`/api/v1/urgences?${params}`);
      setPage(data);
      await enrichPatients(data.content);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les urgences.'));
    } finally {
      setLoading(false);
    }
  }

  function onSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    setAppliedSearch(searchInput);
  }

  async function enrichPatients(rows: UrgenceResponse[]) {
    const missingPatientIds = Array.from(new Set(rows.map((r) => r.patientId))).filter((id) => !patientsById[id]);
    if (missingPatientIds.length === 0) return;
    const results = await Promise.all(
      missingPatientIds.map((id) =>
        api
          .get<PatientResponse>(`/api/v1/patients/${id}`)
          .then((res) => ({ id, patient: res.data }))
          .catch(() => null),
      ),
    );
    setPatientsById((prev) => {
      const next = { ...prev };
      results.forEach((r) => {
        if (r) next[r.id] = r.patient;
      });
      return next;
    });
  }

  function onToggleSort(column: UrgenceSortKey) {
    toggleTableSort(column, sortBy, setSortBy, setSortDir, defaultSortDirForColumn(column));
  }

  const sortedUrgences = useMemo(() => {
    if (!page) return [];
    const rows = [...page.content];
    rows.sort((a, b) => {
      if (sortBy === 'id') return compareNumbers(a.id, b.id, sortDir);
      if (sortBy === 'patient') {
        const aName = patientDisplayName(patientsById[a.patientId], a.patientId);
        const bName = patientDisplayName(patientsById[b.patientId], b.patientId);
        return compareStrings(aName, bName, sortDir);
      }
      if (sortBy === 'priority') {
        return compareStrings(a.priority, b.priority, sortDir);
      }
      return compareStrings(a.status, b.status, sortDir);
    });
    return rows;
  }, [page, sortBy, sortDir, patientsById]);

  const displayedUrgences = useMemo(() => {
    const q = appliedSearch.trim().toLowerCase();
    if (!q) {
      return sortedUrgences;
    }
    return sortedUrgences.filter((item) => {
      if (matchingPatientIds?.has(item.patientId)) {
        return true;
      }
      const patientName = patientDisplayName(patientsById[item.patientId], item.patientId).toLowerCase();
      if (patientName.includes(q)) {
        return true;
      }
      if (isPatientDeceased(patientsById[item.patientId]) && ('décès'.includes(q) || 'deces'.includes(q) || 'décédé'.includes(q))) {
        return true;
      }
      if (String(item.patientId).includes(q) || String(item.id).includes(q)) {
        return true;
      }
      if (item.motif?.toLowerCase().includes(q)) {
        return true;
      }
      if (item.priority.toLowerCase().includes(q)) {
        return true;
      }
      if (statusLabels[item.status].toLowerCase().includes(q)) {
        return true;
      }
      if (item.triageLevel?.toLowerCase().includes(q)) {
        return true;
      }
      if (item.orientation?.toLowerCase().includes(q)) {
        return true;
      }
      return false;
    });
  }, [sortedUrgences, appliedSearch, matchingPatientIds, patientsById]);

  function resetCreateForm() {
    setCreatePatientQuery('');
    setCreatePatientOptions([]);
    setCreatePatientLoading(false);
    setSelectedPatient(null);
    setMotif('');
    setPriority('P2');
    setCreateError(null);
  }

  async function onCreateSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedPatient) {
      setCreateError('Sélectionnez un patient.');
      return;
    }
    if (!priority.trim()) {
      setCreateError('La priorité est obligatoire.');
      return;
    }

    const payload: UrgenceCreateRequest = {
      patientId: selectedPatient.id,
      motif: motif.trim() || undefined,
      priority: priority.trim(),
    };

    setSubmitting(true);
    setCreateError(null);
    try {
      await api.post<UrgenceResponse>('/api/v1/urgences', payload);
      setShowCreateDrawer(false);
      resetCreateForm();
      setReloadKey((k) => k + 1);
    } catch (err) {
      const fallback =
        axios.isAxiosError(err) && err.response?.status === 409
          ? "Accès aux urgences non autorisé pour votre affectation."
          : "Impossible de créer l'urgence.";
      setCreateError(getApiErrorMessage(err, fallback));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <PageHeader title="Urgences" />

      {page?.scopeRestricted && (
        <div
          className="card"
          style={{
            marginBottom: '1rem',
            borderLeft: '4px solid var(--accent)',
            paddingLeft: '1rem',
          }}
        >
          <strong>Périmètre hospitalier</strong>
          <p style={{ margin: '0.35rem 0 0', color: 'var(--muted)', fontSize: '0.95rem', lineHeight: 1.45 }}>
            Votre affectation ne comprend pas le service « Urgences » : vous ne pouvez pas voir ni créer de passages aux
            urgences tant que cette unité ne vous est pas assignée.
          </p>
        </div>
      )}

      <div className="card filter-toolbar">
        <div className="filter-toolbar__inner">
          <form onSubmit={onSearchSubmit} className="filter-toolbar__form">
            <div className="field field--grow">
              <label htmlFor="urgence-search">Recherche</label>
              <input
                id="urgence-search"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Patient, n° dossier, priorité, statut…"
              />
            </div>
            <button type="submit" className="btn btn-secondary">
              Rechercher
            </button>
          </form>
          {!page?.scopeRestricted && (
            <button type="button" className="btn btn-primary" onClick={() => setShowCreateDrawer(true)}>
              Nouvelle urgence
            </button>
          )}
        </div>
      </div>
      {error && <div className="error-banner">{error}</div>}
      {loading && <LoadingBlock label="Chargement des urgences…" />}

      {!loading && page && displayedUrgences.length === 0 && (
        <div className="card table-wrap">
          <EmptyState
            title={
              appliedSearch.trim()
                ? 'Aucune urgence ne correspond à la recherche.'
                : 'Aucune urgence à afficher pour le moment.'
            }
            description={
              appliedSearch.trim()
                ? 'Modifiez les critères de recherche.'
                : 'Les nouveaux passages aux urgences apparaîtront ici.'
            }
          />
        </div>
      )}

      {!loading && page && displayedUrgences.length > 0 && (
        <div className="card table-wrap">
          <ScrollTableRegion>
            <table className="data-table urgences-table">
              <thead>
                <tr>
                  <th className="data-table-col--id">
                    <DataTableColumnHeader
                      label="ID"
                      sortable
                      sortActive={sortBy === 'id'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('id')}
                    />
                  </th>
                  <th>
                    <DataTableColumnHeader
                      label="Patient"
                      sortable
                      sortActive={sortBy === 'patient'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('patient')}
                    />
                  </th>
                  <th>
                    <DataTableColumnHeader
                      label="Priorité"
                      sortable
                      sortActive={sortBy === 'priority'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('priority')}
                    />
                  </th>
                  <th>Triage</th>
                  <th>Transfert</th>
                  <th>
                    <DataTableColumnHeader
                      label="Statut"
                      sortable
                      sortActive={sortBy === 'status'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('status')}
                    />
                  </th>
                </tr>
              </thead>
              <tbody>
                {displayedUrgences.map((item) => (
                  <tr
                    key={item.id}
                    className="data-table-row--clickable"
                    role="link"
                    tabIndex={0}
                    aria-label={`Ouvrir le passage urgence ${item.id}`}
                    onClick={() => navigate(`/urgences/${item.id}`)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        navigate(`/urgences/${item.id}`);
                      }
                    }}
                  >
                    <td className="data-table-col--id">{item.id}</td>
                    <td>{patientDisplayName(patientsById[item.patientId], item.patientId)}</td>
                    <td>
                      <span className={priorityBadgeClass(item.priority)}>{priorityLabel(item.priority)}</span>
                    </td>
                    <td>
                      {item.triageLevel ? (
                        <span className={triageLevelBadgeClass(item.triageLevel)}>
                          {triageLevelLabel(item.triageLevel)}
                        </span>
                      ) : (
                        '-'
                      )}
                    </td>
                    <td>
                      <div className="urgence-text-cell">{item.orientation || '-'}</div>
                    </td>
                    <td>
                      <div className="urgence-list-status">
                        <span
                          className={`urgence-status-badge urgence-status-badge--${item.status.toLowerCase()}`}
                        >
                          {statusLabels[item.status]}
                        </span>
                        {isPatientDeceased(patientsById[item.patientId]) ? (
                          <span className="patient-status-badge patient-status-badge--deceased">Décès</span>
                        ) : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </ScrollTableRegion>
          <TableResultFooter
            totalElements={appliedSearch.trim() ? displayedUrgences.length : page.totalElements}
            displayedCount={displayedUrgences.length}
            itemLabelPlural="urgence(s)"
          />
        </div>
      )}

      <Drawer
        open={showCreateDrawer}
        onClose={() => {
          setShowCreateDrawer(false);
          resetCreateForm();
        }}
        title="Créer une urgence"
        footer={
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            <button type="submit" form="urgence-create-form" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Création…' : 'Créer'}
            </button>
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => {
                setShowCreateDrawer(false);
                resetCreateForm();
              }}
            >
              Annuler
            </button>
          </div>
        }
      >
        {createError && <div className="error-banner">{createError}</div>}
        <form id="urgence-create-form" onSubmit={onCreateSubmit} className="card card--flat" style={{ display: 'grid', gap: '0.75rem' }}>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="drawer-urg-patient-search">Patient *</label>
            <input
              id="drawer-urg-patient-search"
              value={createPatientQuery}
              onChange={(e) => {
                setCreatePatientQuery(e.target.value);
                setSelectedPatient(null);
                setCreateError(null);
              }}
              placeholder="Rechercher par nom ou dossier (min 2 caractères)"
              required
            />
            {selectedPatient ? (
              <small style={{ color: 'var(--muted)' }}>
                Sélection : {selectedPatient.firstName} {selectedPatient.lastName} — {selectedPatient.dossierNumber}
              </small>
            ) : createPatientLoading ? (
              <small style={{ color: 'var(--muted)' }}>Recherche en cours…</small>
            ) : null}
            {!selectedPatient && createPatientOptions.length > 0 && (
              <div style={{ marginTop: '0.4rem', border: '1px solid var(--border)', borderRadius: '0.5rem', maxHeight: 220, overflowY: 'auto' }}>
                {createPatientOptions.map((patient) => (
                  <button
                    key={patient.id}
                    type="button"
                    className="btn btn-ghost"
                    style={{ width: '100%', justifyContent: 'flex-start', borderRadius: 0 }}
                    onClick={() => {
                      setSelectedPatient(patient);
                      setCreatePatientQuery(`${patient.firstName} ${patient.lastName} (${patient.dossierNumber})`);
                      setCreatePatientOptions([]);
                      setCreateError(null);
                    }}
                  >
                    {patient.firstName} {patient.lastName} — {patient.dossierNumber}
                  </button>
                ))}
              </div>
            )}
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="drawer-urg-priority">Priorité *</label>
            <select id="drawer-urg-priority" value={priority} onChange={(e) => setPriority(e.target.value)}>
              {priorityOptions.map((p) => (
                <option key={p.value} value={p.value}>
                  {p.label}
                </option>
              ))}
            </select>
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="drawer-urg-motif">Motif</label>
            <input
              id="drawer-urg-motif"
              value={motif}
              onChange={(e) => setMotif(e.target.value)}
              placeholder="Douleur thoracique, dyspnée…"
            />
          </div>
        </form>
      </Drawer>
    </>
  );
}
