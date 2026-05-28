import axios from 'axios';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type {
  HospitalServiceResponse,
  OrientRequest,
  PageUrgenceResponse,
  PageHospitalServiceResponse,
  PatientResponse,
  TriageRequest,
  UrgenceResponse,
  UrgenceStatus,
  UrgenceTimelineEventResponse,
} from '../api/types';
import { DeclareDeathSection } from '../components/DeclareDeathSection';
import { PatientDeceasedBanner } from '../components/PatientDeceasedBanner';
import { Toast } from '../components/ui/Toast';
import { UrgenceTimelinePanel } from '../components/UrgenceTimelinePanel';
import { LoadingBlock } from '../components/ui/LoadingBlock';
import { PageHeader } from '../components/ui/PageHeader';
import { useAuth } from '../auth/AuthContext';
import { hasRole } from '../auth/roles';
import { platformFeatures } from '../config/features';
import { isPatientDeceased } from '../utils/patientStatus';

const statusLabels: Record<UrgenceStatus, string> = {
  EN_ATTENTE_TRIAGE: 'En attente triage',
  EN_COURS: 'En cours',
  ORIENTE: 'Transféré',
  CLOTURE: 'Clôturé',
};

const TRIAGE_LEVEL_OPTIONS = [
  { value: 'TRES_URGENT', label: 'Très urgent' },
  { value: 'URGENT', label: 'Urgent' },
  { value: 'MOYEN', label: 'Moyen' },
] as const;

function triageLevelLabel(value: string | null | undefined): string {
  if (!value) return '—';
  const found = TRIAGE_LEVEL_OPTIONS.find((item) => item.value === value);
  if (found) return found.label;
  return value;
}

function triageLevelBadgeClass(value: string | null | undefined): string {
  if (!value) return 'urgence-triage-badge urgence-triage-badge--unknown';
  const normalized = value.toLowerCase();
  if (normalized === 'tres_urgent') return 'urgence-triage-badge urgence-triage-badge--tres_urgent';
  if (normalized === 'urgent') return 'urgence-triage-badge urgence-triage-badge--urgent';
  if (normalized === 'moyen') return 'urgence-triage-badge urgence-triage-badge--moyen';
  return 'urgence-triage-badge urgence-triage-badge--unknown';
}

function priorityLabel(value: string | null | undefined): string {
  if (!value) return '—';
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

export function UrgenceDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const urgenceId = Number(id);
  const canDeclareDeath = hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_MEDECIN');

  const [urgence, setUrgence] = useState<UrgenceResponse | null>(null);
  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const [patientName, setPatientName] = useState<string>('');
  const [services, setServices] = useState<HospitalServiceResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const [triageLevel, setTriageLevel] = useState('URGENT');
  const [triageDetails, setTriageDetails] = useState('');
  const [transferService, setTransferService] = useState('');
  const [timeline, setTimeline] = useState<UrgenceTimelineEventResponse[]>([]);
  const [timelineLoading, setTimelineLoading] = useState(false);
  const [timelineError, setTimelineError] = useState<string | null>(null);
  const [patientUrgences, setPatientUrgences] = useState<UrgenceResponse[]>([]);

  useEffect(() => {
    if (!Number.isFinite(urgenceId)) {
      setError("ID d'urgence invalide.");
      setLoading(false);
      return;
    }
    void loadData();
  }, [urgenceId]);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [urgenceRes, servicesRes] = await Promise.all([
        api.get<UrgenceResponse>(`/api/v1/urgences/${urgenceId}`),
        api.get<PageHospitalServiceResponse>('/api/v1/hospital-services?activeOnly=true&page=0&size=200'),
      ]);
      setUrgence(urgenceRes.data);
      setServices(servicesRes.data.content);
      if (urgenceRes.data.triageLevel) {
        setTriageLevel(urgenceRes.data.triageLevel);
      }
      if (urgenceRes.data.orientation) {
        setTransferService(urgenceRes.data.orientation);
      }
      const patientRes = await api.get<PatientResponse>(`/api/v1/patients/${urgenceRes.data.patientId}`);
      setPatient(patientRes.data);
      setPatientName(`${patientRes.data.firstName} ${patientRes.data.lastName}`.trim());
      const urgencesRes = await api.get<PageUrgenceResponse>(
        '/api/v1/urgences?sortBy=createdAt&sortDir=desc&page=0&size=500',
      );
      setPatientUrgences(
        (urgencesRes.data.content ?? []).filter((item) => item.patientId === urgenceRes.data.patientId),
      );
      await loadTimeline();
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        setError(getApiErrorMessage(err, 'Passage urgences introuvable.'));
      } else {
        setError(getApiErrorMessage(err, 'Impossible de charger le passage urgences.'));
      }
    } finally {
      setLoading(false);
    }
  }

  async function loadTimeline() {
    setTimelineLoading(true);
    setTimelineError(null);
    try {
      const { data } = await api.get<UrgenceTimelineEventResponse[]>(
        `/api/v1/urgences/${urgenceId}/timeline`,
      );
      setTimeline(Array.isArray(data) ? data : []);
    } catch (err) {
      setTimeline([]);
      setTimelineError(getApiErrorMessage(err, 'Impossible de charger le fil d’événements.'));
    } finally {
      setTimelineLoading(false);
    }
  }

  async function onTriageSubmit(e: React.FormEvent) {
    e.preventDefault();
    setPending(true);
    setError(null);
    setMessage(null);
    try {
      await api.post<UrgenceResponse>(`/api/v1/urgences/${urgenceId}/triage`, {
        triageLevel: triageLevel.trim(),
        details: triageDetails.trim() || undefined,
      } satisfies TriageRequest);
      setMessage('Triage enregistré.');
      setTriageDetails('');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible d’enregistrer le triage.'));
    } finally {
      setPending(false);
    }
  }

  async function onTransferSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!transferService.trim()) {
      setError('Le service de destination est obligatoire.');
      return;
    }
    setPending(true);
    setError(null);
    setMessage(null);
    const payload: OrientRequest = { orientation: transferService.trim() };
    try {
      await api.post<UrgenceResponse>(`/api/v1/urgences/${urgenceId}/orientation`, payload);
      setMessage('Transfert enregistré.');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible d’enregistrer le transfert.'));
    } finally {
      setPending(false);
    }
  }

  async function onDischargeHome() {
    if (!window.confirm(`Confirmer la sortie à domicile du passage ${urgenceId} ?`)) return;
    setPending(true);
    setError(null);
    setMessage(null);
    try {
      const payload: OrientRequest = { orientation: 'Retour domicile' };
      await api.post<UrgenceResponse>(`/api/v1/urgences/${urgenceId}/orientation`, payload);
      setMessage('Sortie à domicile enregistrée.');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible d’enregistrer la sortie à domicile.'));
    } finally {
      setPending(false);
    }
  }

  const isClosed = urgence?.status === 'CLOTURE';
  const showTriage =
    urgence != null && (urgence.status === 'EN_ATTENTE_TRIAGE' || urgence.status === 'EN_COURS');
  const showTransfer =
    urgence != null && (urgence.status === 'EN_COURS' || urgence.status === 'ORIENTE');

  const showDeclareDeath =
    platformFeatures.admissionDeclareDeath &&
    canDeclareDeath &&
    urgence != null &&
    !isPatientDeceased(patient) &&
    !isClosed;

  async function onDeathDeclared() {
    setPending(true);
    setError(null);
    setMessage(null);
    try {
      await api.post<UrgenceResponse>(`/api/v1/urgences/${urgenceId}/close`);
      setMessage('Déclaration de décès enregistrée. Passage clôturé.');
    } catch {
      setMessage('Déclaration de décès enregistrée.');
    } finally {
      setPending(false);
    }
    await loadData();
  }

  const headerTitle = patientName
    ? `Urgences — ${patientName}`
    : loading
      ? 'Passage aux urgences'
      : `Passage urgence ${id}`;

  return (
    <>
      <PageHeader
        title={headerTitle}
        subtitle={urgence ? `Passage n° ${urgence.id} · ${statusLabels[urgence.status]}` : `Passage n° ${id}`}
      />

      <p className="page-breadcrumb">
        <Link to="/urgences">← Retour aux urgences</Link>
        {urgence ? (
          <>
            {' · '}
            <Link to={`/patients/${urgence.patientId}`}>Fiche patient</Link>
          </>
        ) : null}
      </p>

      <Toast message={message} onDismiss={() => setMessage(null)} />
      {error ? <div className="error-banner">{error}</div> : null}
      <PatientDeceasedBanner
        deceasedAt={patient?.deceasedAt}
        detail="Le décès est enregistré pour ce patient."
      />
      {loading ? <LoadingBlock label="Chargement du passage…" /> : null}

      {!loading && urgence ? (
        <div className="urgence-detail-layout">
          <aside className="urgence-detail-layout__aside" aria-label="Actions du passage">
            <div className="card urgence-detail-panel">
              {isClosed ? (
                <>
                  <p className="urgence-detail-closed-banner" role="status">
                    Passage clôturé.
                  </p>
                  <div className="urgence-closed-actions">
                    {urgence.orientation &&
                    !urgence.orientation.toLowerCase().includes('domicile') ? (
                      <p className="urgence-closed-actions__hint">
                        Service ciblé: <strong>{urgence.orientation}</strong>
                      </p>
                    ) : null}
                    <div className="urgence-closed-actions__buttons">
                      <Link className="btn btn-secondary btn-sm" to={`/medical-records/${urgence.patientId}`}>
                        Ouvrir dossier médical
                      </Link>
                      <Link className="btn btn-primary btn-sm" to="/admissions">
                        Aller aux admissions
                      </Link>
                    </div>
                  </div>
                </>
              ) : (
                <div className="urgence-detail-steps">
                  {showTriage ? (
                    <section className="urgence-detail-step" aria-labelledby="urgence-triage-heading">
                      <h3 id="urgence-triage-heading" className="urgence-detail-step__title">
                        Triage
                      </h3>
                      <form onSubmit={onTriageSubmit} className="urgence-detail-form urgence-detail-form--grid">
                        <div className="field" style={{ marginBottom: 0 }}>
                          <label htmlFor="triage-level">Niveau</label>
                          <select
                            id="triage-level"
                            value={triageLevel}
                            onChange={(e) => setTriageLevel(e.target.value)}
                            required
                            disabled={pending}
                          >
                            {TRIAGE_LEVEL_OPTIONS.map((level) => (
                              <option key={level.value} value={level.value}>
                                {level.label}
                              </option>
                            ))}
                          </select>
                        </div>
                        <div className="field" style={{ marginBottom: 0, gridColumn: '1 / -1' }}>
                          <label htmlFor="triage-details">Note</label>
                          <input
                            id="triage-details"
                            value={triageDetails}
                            onChange={(e) => setTriageDetails(e.target.value)}
                            placeholder="Optionnel"
                            disabled={pending}
                          />
                        </div>
                        <div className="urgence-detail-form__actions">
                          <button type="submit" className="btn btn-primary btn-sm" disabled={pending}>
                            {pending ? '…' : 'Enregistrer'}
                          </button>
                        </div>
                      </form>
                    </section>
                  ) : null}

                  {showTransfer ? (
                    <section className="urgence-detail-step" aria-labelledby="urgence-transfer-heading">
                      <h3 id="urgence-transfer-heading" className="urgence-detail-step__title">
                        Transfert
                      </h3>
                      <form onSubmit={onTransferSubmit} className="urgence-detail-form">
                        <div className="field" style={{ marginBottom: 0 }}>
                          <label htmlFor="urgence-transfer-service">Service</label>
                          <select
                            id="urgence-transfer-service"
                            value={transferService}
                            onChange={(e) => setTransferService(e.target.value)}
                            required
                            disabled={pending}
                          >
                            <option value="">Choisir…</option>
                            {services.map((s) => (
                              <option key={s.id} value={s.name}>
                                {s.name}
                              </option>
                            ))}
                            {transferService &&
                            !services.some((s) => s.name === transferService) ? (
                              <option value={transferService}>{transferService}</option>
                            ) : null}
                          </select>
                        </div>
                        <div className="urgence-detail-form__actions">
                          <button type="submit" className="btn btn-primary btn-sm" disabled={pending}>
                            {pending ? '…' : 'Transférer'}
                          </button>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            disabled={pending}
                            onClick={() => void onDischargeHome()}
                          >
                            Sortie domicile
                          </button>
                        </div>
                      </form>
                    </section>
                  ) : null}

                  {showDeclareDeath ? (
                    <section className="urgence-detail-step" aria-label="Fin de passage">
                      <div className="urgence-detail-fin__body">
                        {showDeclareDeath ? (
                          <div className="urgence-fin-card">
                            <DeclareDeathSection
                              bare
                              patientId={urgence.patientId}
                              contextLabel=""
                              bareButtonClassName="urgence-fin-btn"
                              disabled={pending}
                              onSuccess={() => void onDeathDeclared()}
                            />
                          </div>
                        ) : null}
                      </div>
                    </section>
                  ) : null}
                </div>
              )}
            </div>
          </aside>

          <section className="urgence-detail-layout__main" aria-label="Historique du passage">
            <div className="card urgence-detail-panel">
              <h2 className="urgence-detail-panel__title">Historique</h2>
              {timelineError ? <div className="error-banner">{timelineError}</div> : null}
              <UrgenceTimelinePanel events={timeline} loading={timelineLoading} />
            </div>

            <div className="card urgence-detail-panel">
              <h2 className="urgence-detail-panel__title">Parcours urgences du patient</h2>
              {patientUrgences.length === 0 ? (
                <p className="urgence-journey-empty">Aucun autre passage trouvé.</p>
              ) : (
                <ul className="urgence-journey-list">
                  {patientUrgences.map((item) => (
                    <li key={item.id} className="urgence-journey-item">
                      <details open={item.id === urgence.id}>
                        <summary className="urgence-journey-summary">
                          <span>Passage #{item.id}</span>
                          <span className="urgence-journey-status">{statusLabels[item.status]}</span>
                        </summary>
                        <div className="urgence-journey-details">
                          <p>
                            <strong>Date:</strong> {new Date(item.createdAt).toLocaleString('fr-FR')}
                          </p>
                          <p>
                            <strong>Priorité:</strong>{' '}
                            <span className={priorityBadgeClass(item.priority)}>{priorityLabel(item.priority)}</span>
                          </p>
                          <p>
                            <strong>Triage:</strong>{' '}
                            {item.triageLevel ? (
                              <span className={triageLevelBadgeClass(item.triageLevel)}>
                                {triageLevelLabel(item.triageLevel)}
                              </span>
                            ) : (
                              '—'
                            )}
                          </p>
                          <p>
                            <strong>Décision:</strong>{' '}
                            <span className="urgence-note-inline">{item.orientation || '—'}</span>
                          </p>
                          <p>
                            <strong>Motif:</strong> <span className="urgence-note-inline">{item.motif || '—'}</span>
                          </p>
                          <p>
                            <strong>Clôture:</strong>{' '}
                            {item.closedAt ? new Date(item.closedAt).toLocaleString('fr-FR') : 'Non clôturé'}
                          </p>
                          {patient?.deceasedAt ? (
                            <p>
                              <strong>Décès:</strong> enregistré le{' '}
                              {new Date(patient.deceasedAt).toLocaleString('fr-FR')}
                            </p>
                          ) : null}
                          {item.id !== urgence.id ? (
                            <Link className="btn btn-secondary btn-sm" to={`/urgences/${item.id}`}>
                              Ouvrir ce passage
                            </Link>
                          ) : null}
                        </div>
                      </details>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </section>
        </div>
      ) : null}
    </>
  );
}
