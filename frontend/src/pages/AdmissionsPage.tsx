import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type {
  AdmissionCreateRequest,
  AdmissionResponse,
  AdmissionStatus,
  BedSuggestionResponse,
  HospitalServiceResponse,
  PageAdmissionResponse,
  PageHospitalServiceResponse,
  PagePatientResponse,
  PatientResponse,
} from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { hasRole } from '../auth/roles';
import { DataTableColumnHeader } from '../components/DataTableColumnHeader';
import { Drawer } from '../components/ui/Drawer';
import { EmptyState } from '../components/ui/EmptyState';
import { LoadingBlock } from '../components/ui/LoadingBlock';
import { PageHeader } from '../components/ui/PageHeader';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { usePatientCareIndex } from '../hooks/usePatientCareIndex';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';
import {
  compareNumbers,
  compareStrings,
  defaultSortDirForColumn,
  toggleTableSort,
  type TableSortDir,
} from '../utils/tableSort';
import {
  patientCareStatusBadgeClass,
  patientCareStatusLabel,
  resolvePatientCareStatus,
} from '../utils/patientCareStatus';

type AdmissionSortKey = 'id' | 'patient' | 'serviceName' | 'admissionDateTime' | 'status';

const statusLabels: Record<AdmissionStatus, string> = {
  EN_COURS: 'En cours',
  TRANSFERE: 'Transféré',
  SORTI: 'Sorti',
  DECEDE: 'Sorti',
};

export function AdmissionsPage() {
  const { user } = useAuth();
  const canCreateAdmission =
    hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_RECEPTION') || hasRole(user, 'ROLE_MEDECIN');
  /** Aligné avec les routes dossier médical (admin / médecin / infirmier). */
  const canAccessMedicalRecord =
    hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_MEDECIN') || hasRole(user, 'ROLE_INFIRMIER');
  const [page, setPage] = useState<PageAdmissionResponse | null>(null);
  const [patientNamesById, setPatientNamesById] = useState<Record<number, string>>({});
  const [patientsById, setPatientsById] = useState<Record<number, PatientResponse>>({});
  const { index: patientCareIndex } = usePatientCareIndex();
  const [searchInput, setSearchInput] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');
  const [matchingPatientIds, setMatchingPatientIds] = useState<Set<number> | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [sortBy, setSortBy] = useState<AdmissionSortKey>('admissionDateTime');
  const [sortDir, setSortDir] = useState<TableSortDir>('desc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateDrawer, setShowCreateDrawer] = useState(false);
  const [services, setServices] = useState<HospitalServiceResponse[]>([]);
  const [createError, setCreateError] = useState<string | null>(null);
  const [submittingCreate, setSubmittingCreate] = useState(false);
  const [createPatientQuery, setCreatePatientQuery] = useState('');
  const [createPatientOptions, setCreatePatientOptions] = useState<PatientResponse[]>([]);
  const [createPatientLoading, setCreatePatientLoading] = useState(false);
  const [selectedPatient, setSelectedPatient] = useState<PatientResponse | null>(null);
  const [createServiceName, setCreateServiceName] = useState('');
  const [createRoom, setCreateRoom] = useState('');
  const [createBed, setCreateBed] = useState('');
  const [createReason, setCreateReason] = useState('');
  const [createBedSuggestionMessage, setCreateBedSuggestionMessage] = useState<string | null>(null);

  const fetchBedSuggestion = useCallback(async (serviceName: string) => {
    setCreateBedSuggestionMessage(null);
    if (!serviceName.trim()) {
      setCreateRoom('');
      setCreateBed('');
      return;
    }
    try {
      const { data } = await api.get<BedSuggestionResponse>('/api/v1/admissions/suggestions/bed', {
        params: { serviceName },
      });
      if (data.available && data.room != null && data.bed != null) {
        setCreateRoom(data.room);
        setCreateBed(data.bed);
        setCreateBedSuggestionMessage(null);
      } else {
        setCreateRoom('');
        setCreateBed('');
        setCreateBedSuggestionMessage(data.message ?? 'Aucun lit automatique disponible pour ce service.');
      }
    } catch {
      setCreateRoom('');
      setCreateBed('');
      setCreateBedSuggestionMessage('Impossible de proposer un lit automatiquement.');
    }
  }, []);

  useEffect(() => {
    void loadAdmissions();
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
    void api
      .get<PageHospitalServiceResponse>('/api/v1/hospital-services?activeOnly=true&page=0&size=200')
      .then((res) => setServices(res.data.content))
      .catch(() => setServices([]));
  }, [showCreateDrawer]);

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

  async function loadAdmissions() {
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
      const { data } = await api.get<PageAdmissionResponse>(`/api/v1/admissions?${params}`);
      setPage(data);
      await enrichPatientNames(data.content);
    } catch {
      setError('Impossible de charger les admissions.');
    } finally {
      setLoading(false);
    }
  }

  function onSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    setAppliedSearch(searchInput);
  }

  async function enrichPatientNames(rows: AdmissionResponse[]) {
    const missingPatientIds = Array.from(new Set(rows.map((r) => r.patientId))).filter((id) => !patientNamesById[id]);
    if (missingPatientIds.length === 0) return;
    const results = await Promise.all(
      missingPatientIds.map((id) =>
        api
          .get<PatientResponse>(`/api/v1/patients/${id}`)
          .then((res) => ({
            id,
            name: `${res.data.firstName} ${res.data.lastName}`.trim() || `Patient ${id}`,
            patient: res.data,
          }))
          .catch(() => ({ id, name: `Patient ${id}`, patient: null as PatientResponse | null })),
      ),
    );
    setPatientNamesById((prev) => {
      const next = { ...prev };
      results.forEach((r) => {
        next[r.id] = r.name;
      });
      return next;
    });
    setPatientsById((prev) => {
      const next = { ...prev };
      results.forEach((r) => {
        if (r.patient) next[r.id] = r.patient;
      });
      return next;
    });
  }

  function onToggleSort(column: AdmissionSortKey) {
    toggleTableSort(column, sortBy, setSortBy, setSortDir, defaultSortDirForColumn(column));
  }

  const sortedAdmissions = useMemo(() => {
    if (!page) return [];
    const rows = [...page.content];
    rows.sort((a, b) => {
      if (sortBy === 'id') return compareNumbers(a.id, b.id, sortDir);
      if (sortBy === 'patient') {
        const aName = patientNamesById[a.patientId] ?? String(a.patientId);
        const bName = patientNamesById[b.patientId] ?? String(b.patientId);
        return compareStrings(aName, bName, sortDir);
      }
      if (sortBy === 'serviceName') {
        return compareStrings(a.serviceName, b.serviceName, sortDir);
      }
      if (sortBy === 'admissionDateTime') {
        return compareStrings(a.admissionDateTime, b.admissionDateTime, sortDir);
      }
      return compareStrings(a.status, b.status, sortDir);
    });
    return rows;
  }, [page, sortBy, sortDir, patientNamesById]);

  const displayedAdmissions = useMemo(() => {
    const q = appliedSearch.trim().toLowerCase();
    if (!q) {
      return sortedAdmissions;
    }
    return sortedAdmissions.filter((item) => {
      if (matchingPatientIds?.has(item.patientId)) {
        return true;
      }
      const patientName = (patientNamesById[item.patientId] ?? '').toLowerCase();
      if (patientName.includes(q)) {
        return true;
      }
      if (String(item.patientId).includes(q)) {
        return true;
      }
      if (String(item.id).includes(q)) {
        return true;
      }
      if (item.serviceName.toLowerCase().includes(q)) {
        return true;
      }
      if (item.reason?.toLowerCase().includes(q)) {
        return true;
      }
      const roomBed = [item.room, item.bed].filter(Boolean).join(' ').toLowerCase();
      return roomBed.includes(q);
    });
  }, [sortedAdmissions, appliedSearch, matchingPatientIds, patientNamesById]);

  function resetCreateForm() {
    setCreatePatientQuery('');
    setCreatePatientOptions([]);
    setCreatePatientLoading(false);
    setSelectedPatient(null);
    setCreateServiceName('');
    setCreateRoom('');
    setCreateBed('');
    setCreateReason('');
    setCreateError(null);
    setCreateBedSuggestionMessage(null);
  }

  async function onCreateAdmissionSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedPatient) {
      setCreateError('Sélectionnez un patient.');
      return;
    }
    if (!createServiceName.trim()) {
      setCreateError('Le service est obligatoire.');
      return;
    }

    const payload: AdmissionCreateRequest = {
      patientId: selectedPatient.id,
      serviceName: createServiceName.trim(),
      room: createRoom.trim() || undefined,
      bed: createBed.trim() || undefined,
      reason: createReason.trim() || undefined,
    };

    setSubmittingCreate(true);
    setCreateError(null);
    try {
      await api.post<AdmissionResponse>('/api/v1/admissions', payload);
      setShowCreateDrawer(false);
      resetCreateForm();
      setReloadKey((k) => k + 1);
    } catch (err) {
      setCreateError(getApiErrorMessage(err, "Impossible de créer l'admission."));
    } finally {
      setSubmittingCreate(false);
    }
  }

  return (
    <>
      <PageHeader title="Admissions" />

      <div className="card filter-toolbar">
        <div className="filter-toolbar__inner">
          <form onSubmit={onSearchSubmit} className="filter-toolbar__form">
            <div className="field field--grow">
              <label htmlFor="admission-search">Recherche</label>
              <input
                id="admission-search"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                placeholder="Patient, n° dossier, service, n° admission…"
              />
            </div>
            <button type="submit" className="btn btn-secondary">
              Rechercher
            </button>
          </form>

          {canCreateAdmission && (
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => {
                setShowCreateDrawer(true);
                setCreateError(null);
              }}
            >
              Nouvelle admission
            </button>
          )}
        </div>
      </div>
      {error && <div className="error-banner">{error}</div>}
      {loading && <LoadingBlock label="Chargement des admissions…" />}

      {!loading && page && displayedAdmissions.length === 0 && (
        <div className="card table-wrap">
          <EmptyState
            title="Aucune admission trouvée"
            description="Modifiez les critères de recherche ou créez une nouvelle admission."
          />
        </div>
      )}

      {!loading && page && displayedAdmissions.length > 0 && (
        <div className="card table-wrap">
          <ScrollTableRegion>
            <table className="data-table">
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
                      label="Service"
                      sortable
                      sortActive={sortBy === 'serviceName'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('serviceName')}
                    />
                  </th>
                  <th>Chambre / lit</th>
                  <th>Motif</th>
                  <th>
                    <DataTableColumnHeader
                      label="Admission"
                      sortable
                      sortActive={sortBy === 'admissionDateTime'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('admissionDateTime')}
                    />
                  </th>
                  <th>Sortie</th>
                  <th>
                    <DataTableColumnHeader
                      label="Statut séjour"
                      sortable
                      sortActive={sortBy === 'status'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('status')}
                    />
                  </th>
                  <th>État patient</th>
                  <th>Fiche</th>
                  <th>Dossier</th>
                </tr>
              </thead>
              <tbody>
                {displayedAdmissions.map((item) => (
                  <tr key={item.id}>
                    <td className="data-table-col--id">{item.id}</td>
                    <td>{patientNamesById[item.patientId] ?? item.patientId}</td>
                    <td>{item.serviceName}</td>
                    <td>{[item.room, item.bed].filter(Boolean).join(' / ') || '-'}</td>
                    <td>{item.reason}</td>
                    <td>{new Date(item.admissionDateTime).toLocaleString('fr-FR')}</td>
                    <td>{item.dischargeDateTime ? new Date(item.dischargeDateTime).toLocaleString('fr-FR') : '-'}</td>
                    <td>{statusLabels[item.status]}</td>
                    <td>
                      {(() => {
                        const careStatus = resolvePatientCareStatus(
                          item.patientId,
                          patientsById[item.patientId],
                          patientCareIndex,
                        );
                        return (
                          <span className={patientCareStatusBadgeClass(careStatus)} title="État actuel du patient">
                            {patientCareStatusLabel(careStatus)}
                          </span>
                        );
                      })()}
                    </td>
                    <td>
                      <Link to={`/admissions/${item.id}`}>Fiche</Link>
                    </td>
                    <td>
                      {canAccessMedicalRecord ? (
                        <Link to={`/medical-records/${item.patientId}`}>Dossier</Link>
                      ) : (
                        '—'
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </ScrollTableRegion>
          <TableResultFooter
            totalElements={appliedSearch.trim() ? displayedAdmissions.length : page.totalElements}
            displayedCount={displayedAdmissions.length}
            itemLabelPlural="admission(s)"
          />
        </div>
      )}

      <Drawer
        open={showCreateDrawer}
        onClose={() => {
          setShowCreateDrawer(false);
          resetCreateForm();
        }}
        title="Créer une admission"
        footer={
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            <button type="submit" form="admission-create-form" className="btn btn-primary" disabled={submittingCreate}>
              {submittingCreate ? 'Création…' : "Créer l'admission"}
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
        <form id="admission-create-form" onSubmit={onCreateAdmissionSubmit} className="card card--flat" style={{ display: 'grid', gap: '0.75rem' }}>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="drawer-admission-patient-search">Patient *</label>
            <input
              id="drawer-admission-patient-search"
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
                      setCreatePatientQuery(
                        `${patient.firstName} ${patient.lastName} (${patient.dossierNumber})`
                      );
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
            <label htmlFor="drawer-admission-service-name">Service *</label>
            <select
              id="drawer-admission-service-name"
              value={createServiceName}
              onChange={(e) => {
                const v = e.target.value;
                setCreateServiceName(v);
                void fetchBedSuggestion(v);
              }}
              required
            >
              <option value="">Sélectionner un service</option>
              {services.map((s) => (
                <option key={s.id} value={s.name}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>
          {createBedSuggestionMessage && (
            <div
              role="status"
              style={{
                padding: '0.5rem 0.65rem',
                borderRadius: '0.45rem',
                border: '1px solid rgba(214, 158, 46, 0.55)',
                background: 'rgba(214, 158, 46, 0.12)',
                color: 'var(--text)',
                fontSize: '0.88rem',
                lineHeight: 1.35,
              }}
            >
              {createBedSuggestionMessage}
            </div>
          )}
          <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="drawer-admission-room">Chambre</label>
              <input id="drawer-admission-room" value={createRoom} onChange={(e) => setCreateRoom(e.target.value)} placeholder="Ex. A1" />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="drawer-admission-bed">Lit</label>
              <input id="drawer-admission-bed" value={createBed} onChange={(e) => setCreateBed(e.target.value)} placeholder="Ex. 01" />
            </div>
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="drawer-admission-reason">Motif</label>
            <textarea
              id="drawer-admission-reason"
              rows={4}
              value={createReason}
              onChange={(e) => setCreateReason(e.target.value)}
              placeholder="Motif d'hospitalisation"
            />
          </div>
        </form>
      </Drawer>
    </>
  );
}
