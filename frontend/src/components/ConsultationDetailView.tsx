import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type {
  ClinicalPrescriptionCreateRequest,
  ClinicalPrescriptionResponse,
  ConsultationEventResponse,
  ConsultationResponse,
  DiseaseCatalogResponse,
  EventCreateRequest,
} from '../api/types';
import { formatClinicalPrescriptionDetails } from '../utils/prescriptionDisplay';
import {
  compareNumbers,
  compareStrings,
  defaultSortDirForColumn,
  toggleTableSort,
} from '../utils/tableSort';
import {
  DataTableColumnHeader,
  DataTableFilterCell,
  DataTableFilterHint,
  DataTableFilterSelectCell,
  type TableSortDir,
} from './DataTableColumnHeader';
import { ScrollTableRegion, TableResultFooter } from './ScrollTableRegion';
import { Drawer } from './ui/Drawer';
import { Toast } from './ui/Toast';

const EVENT_TYPE_LABELS: Record<string, string> = {
  OBSERVATION: 'Observation',
  DIAGNOSTIC: 'Diagnostic',
  EXAM_ORDER: 'Demande examen',
};

const EVENT_TYPE_FILTER_OPTIONS = Object.entries(EVENT_TYPE_LABELS).map(([value, label]) => ({
  value,
  label,
}));

type EventEntryKind = keyof typeof EVENT_TYPE_LABELS;

const EVENT_ENTRY_OPTIONS: { value: EventEntryKind; label: string }[] = [
  { value: 'OBSERVATION', label: 'Observation' },
  { value: 'DIAGNOSTIC', label: 'Diagnostic' },
  { value: 'EXAM_ORDER', label: 'Demande examen' },
];

const EVENT_ENTRY_CONFIG: Record<
  EventEntryKind,
  { pathSuffix: string; successMessage: string; placeholder: string; submitLabel: string }
> = {
  OBSERVATION: {
    pathSuffix: 'observations',
    successMessage: 'Observation ajoutée.',
    placeholder: "Décrire l'observation clinique…",
    submitLabel: 'Ajouter observation',
  },
  DIAGNOSTIC: {
    pathSuffix: 'diagnostics',
    successMessage: 'Diagnostic ajouté.',
    placeholder: 'Saisir le diagnostic…',
    submitLabel: 'Ajouter diagnostic',
  },
  EXAM_ORDER: {
    pathSuffix: 'orders/exams',
    successMessage: 'Demande ajoutée.',
    placeholder: "Préciser l'examen demandé…",
    submitLabel: 'Ajouter demande',
  },
};

const DISEASE_TYPE_OPTIONS = [
  'Infectieuse',
  'Chronique',
  'Aiguë',
  'Dégénérative',
  'Congénitale',
  'Auto-immune',
  'Métabolique',
  'Tumorale',
  'Traumatique',
  'Autre',
] as const;

function timelineDiseaseTypeLabel(ev: ConsultationEventResponse): string {
  if (ev.type !== 'DIAGNOSTIC') {
    return '—';
  }
  return ev.diseaseType?.trim() || '—';
}

function timelineDiseaseLabel(ev: ConsultationEventResponse): string {
  if (ev.type !== 'DIAGNOSTIC') {
    return '—';
  }
  return ev.diseaseName?.trim() || '—';
}

function timelineContentLabel(ev: ConsultationEventResponse): string {
  if (ev.type === 'DIAGNOSTIC') {
    return ev.content?.trim() || '—';
  }
  return ev.content?.trim() || '—';
}

type TimelineSortKey = 'createdAt' | 'type';
type TimelineFilterColumn = 'date' | 'type';
type PrescriptionSortKey = 'drugName' | 'details' | 'status' | 'createdAt';

type TimelineFilters = {
  date: string;
  type: string;
};

const emptyTimelineFilters: TimelineFilters = { date: '', type: '' };

function eventTypeLabel(type: string): string {
  return EVENT_TYPE_LABELS[type] ?? type;
}

function matchesTimelineFilters(ev: ConsultationEventResponse, filters: TimelineFilters): boolean {
  const dateQ = filters.date.trim().toLowerCase();
  if (dateQ) {
    const formatted = new Date(ev.createdAt).toLocaleString('fr-FR').toLowerCase();
    if (!formatted.includes(dateQ) && !ev.createdAt.toLowerCase().includes(dateQ)) return false;
  }

  const typeQ = filters.type.trim();
  if (typeQ && ev.type !== typeQ) return false;

  return true;
}

function hasActiveTimelineFilters(filters: TimelineFilters): boolean {
  return Object.values(filters).some((v) => v.trim() !== '');
}

function prescriptionDetailsText(p: ClinicalPrescriptionResponse): string {
  return formatClinicalPrescriptionDetails(p.dosage, p.frequency);
}

export type ConsultationDetailViewProps = {
  consultationId: number;
  backTo: string;
  backLabel: string;
  /** Lorsque la fiche est ouverte depuis un séjour hospitalier. */
  admissionContextId?: number;
};

export function ConsultationDetailView({
  consultationId,
  backTo,
  backLabel,
  admissionContextId,
}: ConsultationDetailViewProps) {
  const [consultation, setConsultation] = useState<ConsultationResponse | null>(null);
  const [timeline, setTimeline] = useState<ConsultationEventResponse[]>([]);
  const [eventKind, setEventKind] = useState<EventEntryKind>('OBSERVATION');
  const [eventDrafts, setEventDrafts] = useState<Record<EventEntryKind, string>>({
    OBSERVATION: '',
    DIAGNOSTIC: '',
    EXAM_ORDER: '',
  });
  const [diagnosticDiseaseType, setDiagnosticDiseaseType] = useState('');
  const [diagnosticDiseaseName, setDiagnosticDiseaseName] = useState('');
  const [selectableDiseases, setSelectableDiseases] = useState<DiseaseCatalogResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [timelineWarning, setTimelineWarning] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [timelineSortBy, setTimelineSortBy] = useState<TimelineSortKey>('createdAt');
  const [timelineSortDir, setTimelineSortDir] = useState<TableSortDir>('desc');
  const [openTimelineFilterCol, setOpenTimelineFilterCol] = useState<TimelineFilterColumn | null>(null);
  const [timelineFilterDraft, setTimelineFilterDraft] = useState<TimelineFilters>({ ...emptyTimelineFilters });
  const [appliedTimelineFilters, setAppliedTimelineFilters] = useState<TimelineFilters>({
    ...emptyTimelineFilters,
  });
  const [selectedTimelineEvent, setSelectedTimelineEvent] = useState<ConsultationEventResponse | null>(
    null,
  );
  const [prescriptions, setPrescriptions] = useState<ClinicalPrescriptionResponse[]>([]);
  const [prescriptionSortBy, setPrescriptionSortBy] = useState<PrescriptionSortKey>('createdAt');
  const [prescriptionSortDir, setPrescriptionSortDir] = useState<TableSortDir>('desc');
  const [rxDrug, setRxDrug] = useState('');
  const [rxDetails, setRxDetails] = useState('');

  const displayedPrescriptions = useMemo(() => {
    const sorted = [...prescriptions];
    sorted.sort((a, b) => {
      if (prescriptionSortBy === 'createdAt') {
        return compareNumbers(
          new Date(a.createdAt).getTime(),
          new Date(b.createdAt).getTime(),
          prescriptionSortDir,
        );
      }
      if (prescriptionSortBy === 'drugName') {
        return compareStrings(a.drugName, b.drugName, prescriptionSortDir);
      }
      if (prescriptionSortBy === 'status') {
        return compareStrings(a.status, b.status, prescriptionSortDir);
      }
      return compareStrings(prescriptionDetailsText(a), prescriptionDetailsText(b), prescriptionSortDir);
    });
    return sorted;
  }, [prescriptions, prescriptionSortBy, prescriptionSortDir]);

  const displayedTimeline = useMemo(() => {
    const filtered = timeline.filter((ev) => matchesTimelineFilters(ev, appliedTimelineFilters));
    const sorted = [...filtered];
    sorted.sort((a, b) => {
      let cmp = 0;
      if (timelineSortBy === 'createdAt') {
        cmp = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
      } else {
        cmp = eventTypeLabel(a.type).localeCompare(eventTypeLabel(b.type), 'fr');
      }
      return timelineSortDir === 'asc' ? cmp : -cmp;
    });
    return sorted;
  }, [timeline, appliedTimelineFilters, timelineSortBy, timelineSortDir]);

  const timelineFilterHint = useMemo(() => {
    if (!hasActiveTimelineFilters(appliedTimelineFilters)) return null;
    const parts: string[] = [];
    if (appliedTimelineFilters.date.trim()) parts.push(`Date « ${appliedTimelineFilters.date.trim()} »`);
    if (appliedTimelineFilters.type.trim()) {
      parts.push(`Type « ${eventTypeLabel(appliedTimelineFilters.type)} »`);
    }
    return parts.join(' · ');
  }, [appliedTimelineFilters]);

  const showTimelineFilterRow = openTimelineFilterCol !== null;

  function toggleTimelineSort(column: TimelineSortKey) {
    if (timelineSortBy === column) {
      setTimelineSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setTimelineSortBy(column);
      setTimelineSortDir(column === 'createdAt' ? 'desc' : 'asc');
    }
  }

  function onTogglePrescriptionSort(column: PrescriptionSortKey) {
    toggleTableSort(
      column,
      prescriptionSortBy,
      setPrescriptionSortBy,
      setPrescriptionSortDir,
      defaultSortDirForColumn(column),
    );
  }

  function toggleTimelineFilterColumn(column: TimelineFilterColumn) {
    setOpenTimelineFilterCol((current) => (current === column ? null : column));
    setTimelineFilterDraft({ ...appliedTimelineFilters });
  }

  function applyTimelineFilter(column: TimelineFilterColumn) {
    const next = { ...appliedTimelineFilters, [column]: timelineFilterDraft[column] };
    setAppliedTimelineFilters(next);
    setOpenTimelineFilterCol(null);
  }

  function clearTimelineFilter(column: TimelineFilterColumn) {
    const next = { ...appliedTimelineFilters, [column]: '' };
    setAppliedTimelineFilters(next);
    setTimelineFilterDraft((d) => ({ ...d, [column]: '' }));
    setOpenTimelineFilterCol(null);
  }

  function clearAllTimelineFilters() {
    setAppliedTimelineFilters({ ...emptyTimelineFilters });
    setTimelineFilterDraft({ ...emptyTimelineFilters });
    setOpenTimelineFilterCol(null);
  }

  useEffect(() => {
    if (!Number.isFinite(consultationId)) {
      setError('ID consultation invalide.');
      setLoading(false);
      return;
    }
    void loadData();
  }, [consultationId]);

  useEffect(() => {
    if (eventKind !== 'DIAGNOSTIC' || !diagnosticDiseaseType.trim()) {
      setSelectableDiseases([]);
      return;
    }
    let cancelled = false;
    void api
      .get<DiseaseCatalogResponse[]>(
        `/api/v1/disease-catalog?diseaseType=${encodeURIComponent(diagnosticDiseaseType.trim())}`,
      )
      .then((res) => {
        if (!cancelled) setSelectableDiseases(res.data);
      })
      .catch(() => {
        if (!cancelled) setSelectableDiseases([]);
      });
    return () => {
      cancelled = true;
    };
  }, [eventKind, diagnosticDiseaseType]);

  function applyTimelineResult(eventsResult: PromiseSettledResult<{ data: ConsultationEventResponse[] }>) {
    if (eventsResult.status === 'fulfilled') {
      setTimeline(eventsResult.value.data);
      setTimelineWarning(null);
    } else {
      setTimeline([]);
      setTimelineWarning(
        getApiErrorMessage(
          eventsResult.reason,
          'Chronologie indisponible. Redémarrez le BFF (8080) et clinical-record (8086) après mise à jour.',
        ),
      );
    }
  }

  async function loadData() {
    setLoading(true);
    setError(null);
    setTimelineWarning(null);
    setTimeline([]);
    try {
      const { data } = await api.get<ConsultationResponse>(`/api/v1/consultations/${consultationId}`);
      setConsultation(data);

      const [eventsResult, prescriptionsResult] = await Promise.allSettled([
        api.get<ConsultationEventResponse[]>(`/api/v1/consultations/${consultationId}/events`),
        api.get<ClinicalPrescriptionResponse[]>(`/api/v1/patients/${data.patientId}/prescriptions`),
      ]);

      applyTimelineResult(eventsResult);

      if (prescriptionsResult.status === 'fulfilled') {
        setPrescriptions(prescriptionsResult.value.data);
      } else {
        setPrescriptions([]);
      }
    } catch (err) {
      setConsultation(null);
      setError(getApiErrorMessage(err, 'Impossible de charger le dossier consultation.'));
    } finally {
      setLoading(false);
    }
  }

  async function refreshTimeline() {
    try {
      const { data } = await api.get<ConsultationEventResponse[]>(
        `/api/v1/consultations/${consultationId}/events`,
      );
      setTimeline(data);
      setTimelineWarning(null);
    } catch (err) {
      setTimelineWarning(
        getApiErrorMessage(
          err,
          'Chronologie indisponible. Redémarrez le BFF (8080) et clinical-record (8086) après mise à jour.',
        ),
      );
    }
  }

  async function postEvent(
    path: string,
    content: string,
    clear: () => void,
    successMessage: string,
    diseaseType?: string | null,
    diseaseName?: string | null,
  ) {
    if (!content.trim()) return setError('Le contenu est obligatoire.');
    const payload: EventCreateRequest = {
      content: content.trim(),
      diseaseType: diseaseType?.trim() ? diseaseType.trim() : null,
      diseaseName: diseaseName?.trim() ? diseaseName.trim() : null,
    };
    setPending(true);
    setError(null);
    setMessage(null);
    try {
      await api.post<ConsultationEventResponse>(path, payload);
      clear();
      setMessage(successMessage);
      await refreshTimeline();
      if (diseaseType?.trim()) {
        try {
          const { data } = await api.get<DiseaseCatalogResponse[]>(
            `/api/v1/disease-catalog?diseaseType=${encodeURIComponent(diseaseType.trim())}`,
          );
          setSelectableDiseases(data);
        } catch {
          /* liste optionnelle */
        }
      }
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'ajouter cet evenement."));
    } finally {
      setPending(false);
    }
  }

  function onAddTimelineEvent() {
    const config = EVENT_ENTRY_CONFIG[eventKind];
    const rawContent = eventDrafts[eventKind];
    if (eventKind === 'DIAGNOSTIC') {
      if (!diagnosticDiseaseType.trim()) {
        setError('Le type de maladie est obligatoire.');
        return;
      }
      if (!diagnosticDiseaseName.trim()) {
        setError('Le nom de la maladie est obligatoire.');
        return;
      }
    }
    const content =
      eventKind === 'DIAGNOSTIC' && !rawContent.trim()
        ? diagnosticDiseaseName.trim()
        : rawContent;
    void postEvent(
      `/api/v1/consultations/${consultationId}/${config.pathSuffix}`,
      content,
      () => {
        setEventDrafts((prev) => ({ ...prev, [eventKind]: '' }));
        if (eventKind === 'DIAGNOSTIC') {
          setDiagnosticDiseaseName('');
        }
      },
      config.successMessage,
      eventKind === 'DIAGNOSTIC' ? diagnosticDiseaseType : null,
      eventKind === 'DIAGNOSTIC' ? diagnosticDiseaseName : null,
    );
  }

  async function loadPrescriptions(patientId: number) {
    const { data } = await api.get<ClinicalPrescriptionResponse[]>(
      `/api/v1/patients/${patientId}/prescriptions`,
    );
    setPrescriptions(data);
  }

  async function onAddPrescription(e: React.FormEvent) {
    e.preventDefault();
    if (!consultation) return;
    const drugName = rxDrug.trim();
    const prescriptionDetails = rxDetails.trim();
    if (!drugName || !prescriptionDetails) {
      setError('Médicament et détails de la prescription sont obligatoires.');
      return;
    }
    const payload: ClinicalPrescriptionCreateRequest = { drugName, prescriptionDetails };
    setPending(true);
    setError(null);
    setMessage(null);
    try {
      await api.post(`/api/v1/patients/${consultation.patientId}/prescriptions`, payload);
      setRxDrug('');
      setRxDetails('');
      setMessage('Prescription enregistrée.');
      await loadPrescriptions(consultation.patientId);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'ajouter la prescription."));
    } finally {
      setPending(false);
    }
  }

  const stayConsultationsPath =
    admissionContextId != null
      ? `/admissions/${admissionContextId}/consultations`
      : consultation
        ? `/admissions/${consultation.admissionId}/consultations`
        : null;

  return (
    <div className="consultation-detail-page">
      <p className="page-breadcrumb">
        <Link to={backTo}>← {backLabel}</Link>
        {!loading && consultation ? (
          <>
            {' · '}
            <Link to={`/patients/${consultation.patientId}`}>Fiche patient</Link>
            {' · '}
            <Link to={`/medical-records/${consultation.patientId}`}>Dossier médical</Link>
            {admissionContextId == null ? (
              <>
                {' · '}
                <Link to={`/admissions/${consultation.admissionId}`}>Séjour</Link>
              </>
            ) : stayConsultationsPath ? (
              <>
                {' · '}
                <Link to={`/admissions/${consultation.admissionId}`}>Fiche séjour</Link>
              </>
            ) : null}
          </>
        ) : null}
      </p>

      <Toast message={message} onDismiss={() => setMessage(null)} />
      {error && <div className="error-banner">{error}</div>}
      {timelineWarning && !error && (
        <p className="loading-block" style={{ color: 'var(--muted)' }}>
          {timelineWarning}
        </p>
      )}
      {loading && <p className="loading-block">Chargement de la chronologie…</p>}

      {!loading && consultation && (
        <>
          <div className="clinical-layout consultation-detail-body">
            <div className="clinical-layout__actions">
              <div className="consultation-detail-cards-row">
                <div className="card consultation-detail-form">
                  <h2 className="clinical-section__title" style={{ marginTop: 0 }}>
                    Ajouter à la chronologie
                  </h2>
                  <div className="field">
                    <label htmlFor="event-kind">Type</label>
                    <select
                      id="event-kind"
                      value={eventKind}
                      onChange={(e) => setEventKind(e.target.value as EventEntryKind)}
                      disabled={pending}
                    >
                      {EVENT_ENTRY_OPTIONS.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label}
                        </option>
                      ))}
                    </select>
                  </div>
                  {eventKind === 'DIAGNOSTIC' ? (
                    <>
                      <div className="field">
                        <label htmlFor="event-disease-type">Type de maladie *</label>
                        <select
                          id="event-disease-type"
                          value={diagnosticDiseaseType}
                          onChange={(e) => {
                            setDiagnosticDiseaseType(e.target.value);
                            setDiagnosticDiseaseName('');
                          }}
                          disabled={pending}
                        >
                          <option value="">Sélectionner</option>
                          {DISEASE_TYPE_OPTIONS.map((item) => (
                            <option key={item} value={item}>
                              {item}
                            </option>
                          ))}
                        </select>
                      </div>
                      <div className="field">
                        <label htmlFor="event-disease-name">Maladie *</label>
                        <input
                          id="event-disease-name"
                          list="event-disease-name-options"
                          value={diagnosticDiseaseName}
                          onChange={(e) => setDiagnosticDiseaseName(e.target.value)}
                          placeholder="Saisir ou choisir une maladie"
                          disabled={pending || !diagnosticDiseaseType.trim()}
                        />
                        <datalist id="event-disease-name-options">
                          {selectableDiseases.map((item) => (
                            <option key={item.id} value={item.label} />
                          ))}
                        </datalist>
                        <p style={{ margin: '0.35rem 0 0', fontSize: '0.82rem', color: 'var(--muted)' }}>
                          {selectableDiseases.length > 0
                            ? 'Maladies fréquentes proposées à la sélection (5 saisies ou plus).'
                            : 'Saisie libre : après 5 utilisations identiques, la maladie sera proposée à la sélection.'}
                        </p>
                      </div>
                      <div className="field">
                        <label htmlFor="event-content">Détails du diagnostic</label>
                        <textarea
                          id="event-content"
                          rows={5}
                          value={eventDrafts[eventKind]}
                          onChange={(e) =>
                            setEventDrafts((prev) => ({ ...prev, [eventKind]: e.target.value }))
                          }
                          placeholder="Observations, évolution, recommandations…"
                          disabled={pending}
                        />
                      </div>
                    </>
                  ) : (
                    <div className="field">
                      <label htmlFor="event-content">Contenu</label>
                      <textarea
                        id="event-content"
                        key={eventKind}
                        rows={8}
                        value={eventDrafts[eventKind]}
                        onChange={(e) =>
                          setEventDrafts((prev) => ({ ...prev, [eventKind]: e.target.value }))
                        }
                        placeholder={EVENT_ENTRY_CONFIG[eventKind].placeholder}
                        disabled={pending}
                      />
                    </div>
                  )}
                  <div className="consultation-detail-form__actions">
                    <button
                      type="button"
                      className="btn btn-primary"
                      disabled={pending}
                      onClick={() => onAddTimelineEvent()}
                    >
                      {pending ? 'Traitement...' : EVENT_ENTRY_CONFIG[eventKind].submitLabel}
                    </button>
                  </div>
                </div>

                <div className="card consultation-detail-form">
                  <h2 className="clinical-section__title" style={{ marginTop: 0 }}>
                    Prescription
                  </h2>
                  <form onSubmit={(e) => void onAddPrescription(e)} style={{ display: 'grid', gap: '0.75rem' }}>
                    <div className="field" style={{ marginBottom: 0 }}>
                      <label htmlFor="consult-rx-drug">Médicament *</label>
                      <input
                        id="consult-rx-drug"
                        value={rxDrug}
                        onChange={(e) => setRxDrug(e.target.value)}
                        placeholder="Ex. Paracétamol"
                        disabled={pending}
                        required
                      />
                    </div>
                    <div className="field" style={{ marginBottom: 0 }}>
                      <label htmlFor="consult-rx-details">Détails de la prescription *</label>
                      <textarea
                        id="consult-rx-details"
                        rows={4}
                        value={rxDetails}
                        onChange={(e) => setRxDetails(e.target.value)}
                        placeholder="Ex. 500 mg, 3 fois par jour pendant 5 jours"
                        disabled={pending}
                        required
                      />
                    </div>
                    <div className="consultation-detail-form__actions">
                      <button type="submit" className="btn btn-primary" disabled={pending}>
                        {pending ? 'Enregistrement…' : 'Ajouter la prescription'}
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            </div>

            <div className="consultation-detail-main-column">
            <div className="card table-wrap consultation-detail-timeline">
              <h2 className="clinical-section__title" style={{ marginTop: 0 }}>
                Chronologie
              </h2>
              {timelineFilterHint ? (
                <DataTableFilterHint>
                  Filtres : {timelineFilterHint}
                  {' — '}
                  <button
                    type="button"
                    className="btn btn-ghost"
                    style={{ padding: 0, verticalAlign: 'baseline' }}
                    onClick={clearAllTimelineFilters}
                  >
                    Tout effacer
                  </button>
                </DataTableFilterHint>
              ) : null}
              <ScrollTableRegion>
                <table className="data-table consultation-timeline-table">
                  <colgroup>
                    <col className="consultation-timeline-col-date" />
                    <col className="consultation-timeline-col-type" />
                    <col className="consultation-timeline-col-disease-type" />
                    <col className="consultation-timeline-col-disease" />
                    <col className="consultation-timeline-col-content" />
                  </colgroup>
                  <thead>
                    <tr>
                      <th>
                        <DataTableColumnHeader
                          label="Date"
                          sortable
                          sortActive={timelineSortBy === 'createdAt'}
                          sortDir={timelineSortDir}
                          onSort={() => toggleTimelineSort('createdAt')}
                          filterable
                          filterOpen={openTimelineFilterCol === 'date'}
                          filterActive={appliedTimelineFilters.date.trim() !== ''}
                          onToggleFilter={() => toggleTimelineFilterColumn('date')}
                        />
                      </th>
                      <th>
                        <DataTableColumnHeader
                          label="Type"
                          sortable
                          sortActive={timelineSortBy === 'type'}
                          sortDir={timelineSortDir}
                          onSort={() => toggleTimelineSort('type')}
                          filterable
                          filterOpen={openTimelineFilterCol === 'type'}
                          filterActive={appliedTimelineFilters.type.trim() !== ''}
                          onToggleFilter={() => toggleTimelineFilterColumn('type')}
                        />
                      </th>
                      <th>
                        <DataTableColumnHeader label="Type de maladie" />
                      </th>
                      <th>
                        <DataTableColumnHeader label="Maladie" />
                      </th>
                      <th>
                        <DataTableColumnHeader label="Contenu" />
                      </th>
                    </tr>
                    {showTimelineFilterRow ? (
                      <tr className="table-filter-row">
                        <th>
                          <DataTableFilterCell
                            open={openTimelineFilterCol === 'date'}
                            value={timelineFilterDraft.date}
                            onChange={(v) => setTimelineFilterDraft((d) => ({ ...d, date: v }))}
                            onApply={() => applyTimelineFilter('date')}
                            onClear={() => clearTimelineFilter('date')}
                            placeholder="Date ou heure…"
                          />
                        </th>
                        <th>
                          <DataTableFilterSelectCell
                            open={openTimelineFilterCol === 'type'}
                            value={timelineFilterDraft.type}
                            onChange={(v) => setTimelineFilterDraft((d) => ({ ...d, type: v }))}
                            onApply={() => applyTimelineFilter('type')}
                            onClear={() => clearTimelineFilter('type')}
                            options={EVENT_TYPE_FILTER_OPTIONS}
                            ariaLabel="Filtrer par type d'événement"
                            allLabel="Tous les types"
                          />
                        </th>
                        <th />
                        <th />
                        <th />
                      </tr>
                    ) : null}
                  </thead>
                  <tbody>
                    {displayedTimeline.length === 0 ? (
                      <tr>
                        <td colSpan={5} style={{ color: 'var(--muted)' }}>
                          {timeline.length === 0
                            ? 'Aucun événement.'
                            : hasActiveTimelineFilters(appliedTimelineFilters)
                              ? 'Aucun événement pour ces filtres.'
                              : 'Aucun événement.'}
                        </td>
                      </tr>
                    ) : (
                      displayedTimeline.map((ev) => (
                        <tr
                          key={ev.id}
                          className="consultation-timeline-row"
                          tabIndex={0}
                          role="button"
                          aria-label={`Afficher le détail : ${eventTypeLabel(ev.type)}`}
                          onClick={() => setSelectedTimelineEvent(ev)}
                          onKeyDown={(e) => {
                            if (e.key === 'Enter' || e.key === ' ') {
                              e.preventDefault();
                              setSelectedTimelineEvent(ev);
                            }
                          }}
                        >
                          <td className="consultation-timeline-table__date">
                            {new Date(ev.createdAt).toLocaleString('fr-FR')}
                          </td>
                          <td className="consultation-timeline-table__type">{eventTypeLabel(ev.type)}</td>
                          <td className="consultation-timeline-table__disease-type">
                            <span
                              className="consultation-timeline-table__content-preview"
                              title={timelineDiseaseTypeLabel(ev)}
                            >
                              {timelineDiseaseTypeLabel(ev)}
                            </span>
                          </td>
                          <td className="consultation-timeline-table__disease">
                            <span
                              className="consultation-timeline-table__content-preview"
                              title={timelineDiseaseLabel(ev)}
                            >
                              {timelineDiseaseLabel(ev)}
                            </span>
                          </td>
                          <td className="consultation-timeline-table__content">
                            <span
                              className="consultation-timeline-table__content-preview"
                              title={timelineContentLabel(ev)}
                            >
                              {timelineContentLabel(ev)}
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </ScrollTableRegion>
              <TableResultFooter
                totalElements={displayedTimeline.length}
                displayedCount={displayedTimeline.length}
                itemLabelPlural="événement(s)"
              />
            </div>

            <div className="card table-wrap consultation-detail-prescriptions clinical-compact-table">
              <div className="clinical-section__head">
                <h2 className="clinical-section__title" style={{ marginTop: 0 }}>
                  Prescriptions du patient
                </h2>
                <span className="clinical-section__count">{prescriptions.length}</span>
              </div>
              {prescriptions.length === 0 ? (
                <p className="clinical-empty">Aucune prescription pour ce patient.</p>
              ) : (
                <ScrollTableRegion>
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>
                          <DataTableColumnHeader
                            label="Date"
                            sortable
                            sortActive={prescriptionSortBy === 'createdAt'}
                            sortDir={prescriptionSortDir}
                            onSort={() => onTogglePrescriptionSort('createdAt')}
                          />
                        </th>
                        <th>
                          <DataTableColumnHeader
                            label="Médicament"
                            sortable
                            sortActive={prescriptionSortBy === 'drugName'}
                            sortDir={prescriptionSortDir}
                            onSort={() => onTogglePrescriptionSort('drugName')}
                          />
                        </th>
                        <th>
                          <DataTableColumnHeader
                            label="Détails"
                            sortable
                            sortActive={prescriptionSortBy === 'details'}
                            sortDir={prescriptionSortDir}
                            onSort={() => onTogglePrescriptionSort('details')}
                          />
                        </th>
                        <th>
                          <DataTableColumnHeader
                            label="Statut"
                            sortable
                            sortActive={prescriptionSortBy === 'status'}
                            sortDir={prescriptionSortDir}
                            onSort={() => onTogglePrescriptionSort('status')}
                          />
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {displayedPrescriptions.map((p) => (
                        <tr key={p.id}>
                          <td>{new Date(p.createdAt).toLocaleString('fr-FR')}</td>
                          <td>{p.drugName}</td>
                          <td>{formatClinicalPrescriptionDetails(p.dosage, p.frequency)}</td>
                          <td>{p.status}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </ScrollTableRegion>
              )}
            </div>
            </div>
          </div>
        </>
      )}

      <Drawer
        open={selectedTimelineEvent != null}
        onClose={() => setSelectedTimelineEvent(null)}
        title={
          selectedTimelineEvent
            ? eventTypeLabel(selectedTimelineEvent.type)
            : 'Détail de l’événement'
        }
        width="md"
      >
        {selectedTimelineEvent ? (
          <div className="consultation-timeline-event-detail">
            <p className="consultation-timeline-event-detail__meta">
              <strong>Date</strong>
              {new Date(selectedTimelineEvent.createdAt).toLocaleString('fr-FR')}
            </p>
            <p className="consultation-timeline-event-detail__meta">
              <strong>Type</strong>
              {eventTypeLabel(selectedTimelineEvent.type)}
            </p>
            <div className="consultation-timeline-event-detail__body">
              <strong>Contenu</strong>
              <p>{selectedTimelineEvent.content || '—'}</p>
            </div>
            {selectedTimelineEvent.type === 'DIAGNOSTIC' ? (
              <>
                <p className="consultation-timeline-event-detail__meta">
                  <strong>Type de maladie</strong>
                  {selectedTimelineEvent.diseaseType || '—'}
                </p>
                <p className="consultation-timeline-event-detail__meta">
                  <strong>Maladie</strong>
                  {selectedTimelineEvent.diseaseName || '—'}
                </p>
              </>
            ) : null}
          </div>
        ) : null}
      </Drawer>
    </div>
  );
}
