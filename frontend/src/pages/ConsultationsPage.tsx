import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import { hasRole } from '../auth/roles';
import type {
  AdmissionResponse,
  ConsultationCreateRequest,
  ConsultationResponse,
  PageAdmissionResponse,
  PageConsultationResponse,
  PagePatientResponse,
  PatientResponse,
} from '../api/types';
import {
  DataTableColumnHeader,
  DataTableFilterCell,
  DataTableFilterHint,
  type TableSortDir,
} from '../components/DataTableColumnHeader';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { LoadingBlock } from '../components/ui/LoadingBlock';
import { PageHeader } from '../components/ui/PageHeader';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';
import {
  compareNumbers,
  compareStrings,
  defaultSortDirForColumn,
  toggleTableSort,
} from '../utils/tableSort';

type ConsultationSortKey = 'id' | 'consultationDateTime' | 'patientId' | 'doctorName';
type FilterColumn = 'id' | 'patient' | 'doctor' | 'motif';

type ColumnFilters = {
  id: string;
  patient: string;
  doctor: string;
  motif: string;
};

const emptyFilters: ColumnFilters = { id: '', patient: '', doctor: '', motif: '' };

function pickDefaultAdmission(admissions: AdmissionResponse[]): AdmissionResponse | null {
  const active = admissions.filter((a) => a.status === 'EN_COURS');
  if (active.length > 0) return active[0];
  if (admissions.length === 1) return admissions[0];
  return null;
}

function matchesColumnFilters(
  item: ConsultationResponse,
  filters: ColumnFilters,
  patientNamesById: Record<number, string>,
): boolean {
  const idQ = filters.id.trim();
  if (idQ && !String(item.id).includes(idQ)) return false;

  const patientQ = filters.patient.trim().toLowerCase();
  if (patientQ) {
    const name = (patientNamesById[item.patientId] ?? '').toLowerCase();
    if (!String(item.patientId).includes(patientQ) && !name.includes(patientQ)) return false;
  }

  const doctorQ = filters.doctor.trim().toLowerCase();
  if (doctorQ && !item.doctorName.toLowerCase().includes(doctorQ)) return false;

  const motifQ = filters.motif.trim().toLowerCase();
  if (motifQ && !(item.reason ?? '').toLowerCase().includes(motifQ)) return false;

  return true;
}

function hasActiveFilters(filters: ColumnFilters): boolean {
  return Object.values(filters).some((v) => v.trim() !== '');
}

export function ConsultationsPage() {
  const { user } = useAuth();
  /** Créer une consultation : médecin / admin uniquement (backend medical-service). */
  const canCreateConsultation = hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_MEDECIN');
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const urlPatientId = searchParams.get('patientId')?.trim() ?? '';
  const urlAdmissionId = searchParams.get('admissionId')?.trim() ?? '';
  const shouldOpenConsultation = searchParams.get('open') === '1';
  const urlContextBootstrapped = useRef(false);
  const autoOpenHandled = useRef(false);

  const connectedDoctorName = user?.fullName?.trim() || user?.username?.trim() || 'Utilisateur connecté';
  const [page, setPage] = useState<PageConsultationResponse | null>(null);
  const [sortBy, setSortBy] = useState<ConsultationSortKey>('consultationDateTime');
  const [sortDir, setSortDir] = useState<TableSortDir>('desc');
  const [openFilterCol, setOpenFilterCol] = useState<FilterColumn | null>(null);
  const [filterDraft, setFilterDraft] = useState<ColumnFilters>({ ...emptyFilters });
  const [appliedFilters, setAppliedFilters] = useState<ColumnFilters>({ ...emptyFilters });
  const [patientQuery, setPatientQuery] = useState('');
  const [patientOptions, setPatientOptions] = useState<PatientResponse[]>([]);
  const [patientLoading, setPatientLoading] = useState(false);
  const [selectedPatient, setSelectedPatient] = useState<PatientResponse | null>(null);
  const [admissionOptions, setAdmissionOptions] = useState<AdmissionResponse[]>([]);
  const [admissionLoading, setAdmissionLoading] = useState(false);
  const [admissionId, setAdmissionId] = useState('');
  const [noActiveStay, setNoActiveStay] = useState(false);
  const [showStayPicker, setShowStayPicker] = useState(false);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [patientNamesById, setPatientNamesById] = useState<Record<number, string>>({});

  const serverPatientId = useMemo(() => {
    if (/^\d+$/.test(urlPatientId)) {
      return urlPatientId;
    }
    const q = appliedFilters.patient.trim();
    return /^\d+$/.test(q) ? q : '';
  }, [appliedFilters.patient, urlPatientId]);

  const serverAdmissionId = useMemo(() => {
    return /^\d+$/.test(urlAdmissionId) ? urlAdmissionId : '';
  }, [urlAdmissionId]);

  useEffect(() => {
    if (!urlPatientId || urlContextBootstrapped.current) {
      return;
    }
    urlContextBootstrapped.current = true;
    setAppliedFilters((prev) => ({ ...prev, patient: urlPatientId }));
    setFilterDraft((prev) => ({ ...prev, patient: urlPatientId }));

    if (/^\d+$/.test(urlPatientId)) {
      void api
        .get<PatientResponse>(`/api/v1/patients/${urlPatientId}`)
        .then((res) => {
          const patient = res.data;
          setSelectedPatient(patient);
          setPatientQuery(`${patient.firstName} ${patient.lastName} (${patient.dossierNumber})`);
        })
        .catch(() => {
          /* liste seulement */
        });
    }

    if (/^\d+$/.test(urlAdmissionId)) {
      setAdmissionId(urlAdmissionId);
      setNoActiveStay(false);
      setShowStayPicker(false);
    }
  }, [urlPatientId, urlAdmissionId]);

  useEffect(() => {
    void loadConsultations();
  }, [sortBy, sortDir, serverPatientId, serverAdmissionId]);

  useEffect(() => {
    const query = patientQuery.trim();
    if (query.length < 2) {
      setPatientOptions([]);
      return;
    }
    setPatientLoading(true);
    const timer = window.setTimeout(() => {
      const params = new URLSearchParams({ page: '0', size: '8', sortBy: 'id', sortDir: 'desc', query });
      void api
        .get<PagePatientResponse>(`/api/v1/patients?${params}`)
        .then((res) => setPatientOptions(res.data.content))
        .catch(() => setPatientOptions([]))
        .finally(() => setPatientLoading(false));
    }, 250);
    return () => {
      window.clearTimeout(timer);
      setPatientLoading(false);
    };
  }, [patientQuery]);

  useEffect(() => {
    if (!selectedPatient) {
      setAdmissionOptions([]);
      setAdmissionId('');
      setNoActiveStay(false);
      setShowStayPicker(false);
      return;
    }
    setAdmissionLoading(true);
    setNoActiveStay(false);
    setShowStayPicker(false);
    const params = new URLSearchParams({
      patientId: String(selectedPatient.id),
      status: 'EN_COURS',
      page: '0',
      size: '20',
    });
    void api
      .get<PageAdmissionResponse>(`/api/v1/admissions?${params}`)
      .then((res) => {
        const activeStays = [...res.data.content].sort(
          (a, b) => new Date(b.admissionDateTime).getTime() - new Date(a.admissionDateTime).getTime(),
        );
        setAdmissionOptions(activeStays);
        const chosen = pickDefaultAdmission(activeStays);
        if (chosen) {
          setAdmissionId(String(chosen.id));
          setNoActiveStay(false);
          setShowStayPicker(false);
        } else {
          setAdmissionId('');
          setNoActiveStay(true);
          setShowStayPicker(false);
        }
      })
      .catch(() => {
        setAdmissionOptions([]);
        setAdmissionId('');
        setNoActiveStay(true);
      })
      .finally(() => setAdmissionLoading(false));
  }, [selectedPatient]);

  const selectedAdmission =
    admissionId && admissionOptions.length > 0
      ? admissionOptions.find((a) => String(a.id) === admissionId) ?? null
      : null;

  const displayedRows = useMemo(() => {
    if (!page) return [];
    const filtered = page.content.filter((item) =>
      matchesColumnFilters(item, appliedFilters, patientNamesById),
    );
    const rows = [...filtered];
    rows.sort((a, b) => {
      if (sortBy === 'id') return compareNumbers(a.id, b.id, sortDir);
      if (sortBy === 'patientId') {
        const aName = patientNamesById[a.patientId] ?? String(a.patientId);
        const bName = patientNamesById[b.patientId] ?? String(b.patientId);
        return compareStrings(aName, bName, sortDir);
      }
      if (sortBy === 'doctorName') {
        return compareStrings(a.doctorName, b.doctorName, sortDir);
      }
      return compareStrings(a.consultationDateTime, b.consultationDateTime, sortDir);
    });
    return rows;
  }, [page, appliedFilters, patientNamesById, sortBy, sortDir]);

  const filterHint = useMemo(() => {
    if (!hasActiveFilters(appliedFilters)) return null;
    const parts: string[] = [];
    if (appliedFilters.id.trim()) parts.push(`ID « ${appliedFilters.id.trim()} »`);
    if (appliedFilters.patient.trim()) parts.push(`Patient « ${appliedFilters.patient.trim()} »`);
    if (appliedFilters.doctor.trim()) parts.push(`Médecin « ${appliedFilters.doctor.trim()} »`);
    if (appliedFilters.motif.trim()) parts.push(`Motif « ${appliedFilters.motif.trim()} »`);
    return parts.join(' · ');
  }, [appliedFilters]);

  async function loadConsultations() {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({
        sortBy,
        sortDir,
        page: '0',
        size: String(LIST_FETCH_PAGE_SIZE),
      });
      if (serverPatientId) params.set('patientId', serverPatientId);
      if (serverAdmissionId) params.set('admissionId', serverAdmissionId);
      const { data } = await api.get<PageConsultationResponse>(`/api/v1/consultations?${params}`);
      setPage(data);
      await enrichPatientNames(data.content);

      if (shouldOpenConsultation && !autoOpenHandled.current) {
        autoOpenHandled.current = true;
        const candidates = data.content;
        if (candidates.length > 0) {
          const target = candidates[0];
          const fromAdmissionQuery = serverAdmissionId ? `?fromAdmission=${serverAdmissionId}` : '';
          navigate(`/consultations/${target.id}${fromAdmissionQuery}`, { replace: true });
        }
      }
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les consultations.'));
    } finally {
      setLoading(false);
    }
  }

  async function enrichPatientNames(consultations: ConsultationResponse[]) {
    const missingPatientIds = Array.from(new Set(consultations.map((c) => c.patientId))).filter(
      (id) => !patientNamesById[id],
    );
    if (missingPatientIds.length === 0) return;
    const results = await Promise.all(
      missingPatientIds.map((id) =>
        api
          .get<PatientResponse>(`/api/v1/patients/${id}`)
          .then((res) => ({
            id,
            name: `${res.data.firstName} ${res.data.lastName}`.trim() || `Patient ${id}`,
          }))
          .catch(() => ({ id, name: `Patient ${id}` })),
      ),
    );
    setPatientNamesById((prev) => {
      const next = { ...prev };
      results.forEach((r) => {
        next[r.id] = r.name;
      });
      return next;
    });
  }

  function onToggleSort(column: ConsultationSortKey) {
    toggleTableSort(column, sortBy, setSortBy, setSortDir, defaultSortDirForColumn(column));
  }

  function toggleFilterColumn(column: FilterColumn) {
    setOpenFilterCol((current) => (current === column ? null : column));
    setFilterDraft({ ...appliedFilters });
  }

  function applyFilter(column: FilterColumn) {
    const next = { ...appliedFilters, [column]: filterDraft[column] };
    setAppliedFilters(next);
    setOpenFilterCol(null);
  }

  function clearFilter(column: FilterColumn) {
    const next = { ...appliedFilters, [column]: '' };
    setAppliedFilters(next);
    setFilterDraft((d) => ({ ...d, [column]: '' }));
    setOpenFilterCol(null);
  }

  function clearAllFilters() {
    setAppliedFilters({ ...emptyFilters });
    setFilterDraft({ ...emptyFilters });
    setOpenFilterCol(null);
  }

  async function onCreateSubmit(e: React.FormEvent) {
    e.preventDefault();
    const parsedAdmissionId = Number.parseInt(admissionId, 10);
    if (!selectedPatient) return setError('Sélectionnez un patient.');
    if (!Number.isFinite(parsedAdmissionId) || parsedAdmissionId <= 0) {
      return setError(
        noActiveStay
          ? 'Ce patient n\'est admis dans aucun service. Créez une admission avant la consultation.'
          : 'Service d\'admission introuvable.',
      );
    }

    const payload: ConsultationCreateRequest = {
      patientId: selectedPatient.id,
      admissionId: parsedAdmissionId,
      doctorName: connectedDoctorName,
    };

    setSubmitting(true);
    setError(null);
    try {
      const { data: created } = await api.post<ConsultationResponse>('/api/v1/consultations', payload);
      const fromAdmissionQuery =
        Number.isFinite(parsedAdmissionId) && parsedAdmissionId > 0
          ? `?fromAdmission=${parsedAdmissionId}`
          : '';
      navigate(`/consultations/${created.id}${fromAdmissionQuery}`);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de créer la consultation.'));
    } finally {
      setSubmitting(false);
    }
  }

  const showFilterRow = openFilterCol !== null;

  return (
    <div className="page-stack">
      <PageHeader title="Consultations" subtitle="Actes médicaux : observations, diagnostics et demandes d'examens" />

      {canCreateConsultation && (
      <div className="card" style={{ marginBottom: '1rem' }}>
        <h3 style={{ marginTop: 0 }}>Nouvelle consultation</h3>
        <form onSubmit={onCreateSubmit} style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div className="field" style={{ flex: '2 1 280px', marginBottom: 0 }}>
            <label htmlFor="consult-patient-search">Patient *</label>
            <input
              id="consult-patient-search"
              value={patientQuery}
              onChange={(e) => {
                setPatientQuery(e.target.value);
                setSelectedPatient(null);
                setError(null);
              }}
              placeholder="Rechercher par nom ou dossier (min 2 caractères)"
              required
            />
            {selectedPatient ? (
              <small style={{ color: 'var(--muted)' }}>
                Sélection : {selectedPatient.firstName} {selectedPatient.lastName} — {selectedPatient.dossierNumber}
              </small>
            ) : patientLoading ? (
              <small style={{ color: 'var(--muted)' }}>Recherche en cours…</small>
            ) : null}
            {!selectedPatient && patientOptions.length > 0 && (
              <div style={{ marginTop: '0.4rem', border: '1px solid var(--border)', borderRadius: '0.5rem', maxHeight: 220, overflowY: 'auto' }}>
                {patientOptions.map((patient) => (
                  <button
                    key={patient.id}
                    type="button"
                    className="btn btn-ghost"
                    style={{ width: '100%', justifyContent: 'flex-start', borderRadius: 0 }}
                    onClick={() => {
                      setSelectedPatient(patient);
                      setPatientQuery(`${patient.firstName} ${patient.lastName} (${patient.dossierNumber})`);
                      setPatientOptions([]);
                      setError(null);
                    }}
                  >
                    {patient.firstName} {patient.lastName} — {patient.dossierNumber}
                  </button>
                ))}
              </div>
            )}
          </div>
          <div className="field" style={{ flex: '1 1 220px', marginBottom: 0 }}>
            <label htmlFor="consult-service">Service</label>
            {!selectedPatient ? (
              <input id="consult-service" readOnly disabled placeholder="—" />
            ) : admissionLoading ? (
              <input id="consult-service" readOnly disabled value="Chargement…" />
            ) : noActiveStay ? (
              <>
                <input id="consult-service" readOnly disabled value="Non admis" />
                <small style={{ color: 'var(--muted)' }}>
                  <Link to="/admissions">Admettre le patient</Link> avant la consultation.
                </small>
              </>
            ) : selectedAdmission && !showStayPicker ? (
              <>
                <input id="consult-service" readOnly value={selectedAdmission.serviceName} />
                {admissionOptions.length > 1 ? (
                  <button
                    type="button"
                    className="btn btn-ghost"
                    style={{ marginTop: '0.35rem', padding: '0.2rem 0' }}
                    onClick={() => setShowStayPicker(true)}
                  >
                    Changer de service
                  </button>
                ) : null}
              </>
            ) : (
              <>
                <select
                  id="consult-admission-id"
                  value={admissionId}
                  onChange={(e) => setAdmissionId(e.target.value)}
                  required
                >
                  {admissionOptions.map((adm) => (
                    <option key={adm.id} value={adm.id}>
                      {adm.serviceName}
                    </option>
                  ))}
                </select>
                {admissionOptions.length > 1 && selectedAdmission ? (
                  <button
                    type="button"
                    className="btn btn-ghost"
                    style={{ marginTop: '0.35rem', padding: '0.2rem 0' }}
                    onClick={() => setShowStayPicker(false)}
                  >
                    Service le plus récent
                  </button>
                ) : null}
              </>
            )}
          </div>
          <div className="field" style={{ flex: '2 1 220px', marginBottom: 0 }}>
            <label htmlFor="consult-doctor">Médecin</label>
            <input id="consult-doctor" value={connectedDoctorName} readOnly />
          </div>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={submitting || !selectedPatient || admissionLoading || noActiveStay || !admissionId}
          >
            {submitting ? 'Création…' : 'Créer'}
          </button>
        </form>
      </div>
      )}

      {error && <div className="error-banner">{error}</div>}
      {filterHint ? (
        <DataTableFilterHint>
          Filtres : {filterHint}
          {' — '}
          <button type="button" className="btn btn-ghost" style={{ padding: 0, verticalAlign: 'baseline' }} onClick={clearAllFilters}>
            Tout effacer
          </button>
        </DataTableFilterHint>
      ) : null}
      {loading && <LoadingBlock label="Chargement des consultations…" />}

      {!loading && page && (
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
                      filterable
                      filterOpen={openFilterCol === 'id'}
                      filterActive={appliedFilters.id.trim() !== ''}
                      onToggleFilter={() => toggleFilterColumn('id')}
                    />
                  </th>
                  <th>
                    <DataTableColumnHeader
                      label="Patient"
                      sortable
                      sortActive={sortBy === 'patientId'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('patientId')}
                      filterable
                      filterOpen={openFilterCol === 'patient'}
                      filterActive={appliedFilters.patient.trim() !== ''}
                      onToggleFilter={() => toggleFilterColumn('patient')}
                    />
                  </th>
                  <th>
                    <DataTableColumnHeader
                      label="Médecin"
                      sortable
                      sortActive={sortBy === 'doctorName'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('doctorName')}
                      filterable
                      filterOpen={openFilterCol === 'doctor'}
                      filterActive={appliedFilters.doctor.trim() !== ''}
                      onToggleFilter={() => toggleFilterColumn('doctor')}
                    />
                  </th>
                  <th>
                    <DataTableColumnHeader
                      label="Motif"
                      filterable
                      filterOpen={openFilterCol === 'motif'}
                      filterActive={appliedFilters.motif.trim() !== ''}
                      onToggleFilter={() => toggleFilterColumn('motif')}
                    />
                  </th>
                  <th>
                    <DataTableColumnHeader
                      label="Date"
                      sortable
                      sortActive={sortBy === 'consultationDateTime'}
                      sortDir={sortDir}
                      onSort={() => onToggleSort('consultationDateTime')}
                    />
                  </th>
                  <th />
                </tr>
                {showFilterRow ? (
                  <tr className="table-filter-row">
                    <th>
                      <DataTableFilterCell
                        open={openFilterCol === 'id'}
                        value={filterDraft.id}
                        onChange={(v) => setFilterDraft((d) => ({ ...d, id: v }))}
                        onApply={() => applyFilter('id')}
                        onClear={() => clearFilter('id')}
                        placeholder="ID"
                        inputMode="numeric"
                      />
                    </th>
                    <th>
                      <DataTableFilterCell
                        open={openFilterCol === 'patient'}
                        value={filterDraft.patient}
                        onChange={(v) => setFilterDraft((d) => ({ ...d, patient: v }))}
                        onApply={() => applyFilter('patient')}
                        onClear={() => clearFilter('patient')}
                        placeholder="Nom patient"
                      />
                    </th>
                    <th>
                      <DataTableFilterCell
                        open={openFilterCol === 'doctor'}
                        value={filterDraft.doctor}
                        onChange={(v) => setFilterDraft((d) => ({ ...d, doctor: v }))}
                        onApply={() => applyFilter('doctor')}
                        onClear={() => clearFilter('doctor')}
                        placeholder="Nom médecin"
                      />
                    </th>
                    <th>
                      <DataTableFilterCell
                        open={openFilterCol === 'motif'}
                        value={filterDraft.motif}
                        onChange={(v) => setFilterDraft((d) => ({ ...d, motif: v }))}
                        onApply={() => applyFilter('motif')}
                        onClear={() => clearFilter('motif')}
                        placeholder="Contient…"
                      />
                    </th>
                    <th />
                    <th />
                  </tr>
                ) : null}
              </thead>
              <tbody>
                {displayedRows.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="empty-cell">
                      {hasActiveFilters(appliedFilters) ? 'Aucune consultation pour ces filtres.' : 'Aucune consultation.'}
                    </td>
                  </tr>
                ) : (
                  displayedRows.map((item) => (
                    <tr key={item.id}>
                      <td className="data-table-col--id">{item.id}</td>
                      <td>
                        {patientNamesById[item.patientId] ?? item.patientId}
                      </td>
                      <td>{item.doctorName}</td>
                      <td>{item.reason || '-'}</td>
                      <td>{new Date(item.consultationDateTime).toLocaleString('fr-FR')}</td>
                      <td>
                        <Link
                          to={`/consultations/${item.id}${
                            serverAdmissionId ? `?fromAdmission=${serverAdmissionId}` : ''
                          }`}
                        >
                          Fiche de consultation
                        </Link>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </ScrollTableRegion>
          <TableResultFooter
            totalElements={displayedRows.length}
            displayedCount={displayedRows.length}
            itemLabelPlural="consultation(s)"
          />
        </div>
      )}
    </div>
  );
}
