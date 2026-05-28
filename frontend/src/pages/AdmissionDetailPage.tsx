import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import { hasRole } from '../auth/roles';
import { Toast } from '../components/ui/Toast';
import { platformFeatures } from '../config/features';
import type {
  AdmissionResponse,
  BedSuggestionResponse,
  DischargeRequest,
  HospitalServiceResponse,
  PageHospitalServiceResponse,
  PatientResponse,
  TransferRequest,
} from '../api/types';
import { DeclareDeathSection } from '../components/DeclareDeathSection';
import { usePatientCareIndex } from '../hooks/usePatientCareIndex';
import {
  admissionStatusLabel,
  isAdmissionClosed,
  isAdmissionOpenForAdministrativeActions,
} from '../utils/admissionStatus';
import {
  patientCareStatusBadgeClass,
  patientCareStatusLabel,
  resolvePatientCareStatus,
} from '../utils/patientCareStatus';

function formatStayDateTime(iso: string): string {
  return new Date(iso).toLocaleString('fr-FR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function AdmissionDetailPage() {
  const { user } = useAuth();
  const canManageAdmissions = hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_RECEPTION');
  const canEditClinicalForm = hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_MEDECIN');
  const canDeclareDeath = hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_MEDECIN');
  const { id } = useParams<{ id: string }>();
  const admissionId = Number(id);

  const [admission, setAdmission] = useState<AdmissionResponse | null>(null);
  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const [patientName, setPatientName] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [savingAction, setSavingAction] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [transferService, setTransferService] = useState('');
  const [transferRoom, setTransferRoom] = useState('');
  const [transferBed, setTransferBed] = useState('');
  const [transferNote, setTransferNote] = useState('');
  const [transferBedSuggestionMessage, setTransferBedSuggestionMessage] = useState<string | null>(null);
  const [dischargeNote, setDischargeNote] = useState('');
  const [services, setServices] = useState<HospitalServiceResponse[]>([]);

  const fetchTransferBedSuggestion = useCallback(async (serviceName: string) => {
    setTransferBedSuggestionMessage(null);
    if (!platformFeatures.admissionBedSuggestion) {
      return;
    }
    if (!serviceName.trim()) {
      setTransferRoom('');
      setTransferBed('');
      return;
    }
    try {
      const { data } = await api.get<BedSuggestionResponse>('/api/v1/admissions/suggestions/bed', {
        params: { serviceName },
      });
      if (data.available && data.room != null && data.bed != null) {
        setTransferRoom(data.room);
        setTransferBed(data.bed);
        setTransferBedSuggestionMessage(null);
      } else {
        setTransferRoom('');
        setTransferBed('');
        setTransferBedSuggestionMessage(data.message ?? 'Aucun lit automatique disponible pour ce service.');
      }
    } catch {
      setTransferRoom('');
      setTransferBed('');
      setTransferBedSuggestionMessage('Impossible de proposer un lit automatiquement.');
    }
  }, []);

  useEffect(() => {
    if (!Number.isFinite(admissionId)) {
      setError("ID d'admission invalide.");
      setLoading(false);
      return;
    }
    void loadData();
  }, [admissionId]);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const { data: admissionData } = await api.get<AdmissionResponse>(`/api/v1/admissions/${admissionId}`);
      setAdmission(admissionData);
      const patientRes = await api.get<PatientResponse>(`/api/v1/patients/${admissionData.patientId}`);
      setPatient(patientRes.data);
      setPatientName(`${patientRes.data.firstName} ${patientRes.data.lastName}`.trim());
      const servicesRes = await api.get<PageHospitalServiceResponse>('/api/v1/hospital-services?activeOnly=true&page=0&size=200');
      setServices(servicesRes.data.content);

    } catch {
      setError("Impossible de charger la fiche d'admission.");
    } finally {
      setLoading(false);
    }
  }

  async function onTransferSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!transferService.trim()) {
      setError('Le service de destination est obligatoire.');
      return;
    }
    if (!window.confirm(`Confirmer le transfert de l'admission ${admissionId} ?`)) return;

    const payload: TransferRequest = {
      toService: transferService.trim(),
      room: transferRoom.trim() || undefined,
      bed: transferBed.trim() || undefined,
      note: transferNote.trim() || undefined,
    };

    setSavingAction(true);
    setError(null);
    setActionMessage(null);
    try {
      await api.put<AdmissionResponse>(`/api/v1/admissions/${admissionId}/transfer`, payload);
      setTransferService('');
      setTransferRoom('');
      setTransferBed('');
      setTransferNote('');
      setTransferBedSuggestionMessage(null);
      setActionMessage('Transfert enregistre avec succes.');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'executer le transfert."));
    } finally {
      setSavingAction(false);
    }
  }

  async function onDischarge() {
    if (!window.confirm(`Confirmer la sortie du patient pour l'admission ${admissionId} ?`)) return;
    const payload: DischargeRequest = { note: dischargeNote.trim() || undefined };
    setSavingAction(true);
    setError(null);
    setActionMessage(null);
    try {
      await api.put<AdmissionResponse>(`/api/v1/admissions/${admissionId}/discharge`, payload);
      setDischargeNote('');
      setActionMessage('Sortie enregistree avec succes.');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'enregistrer la sortie."));
    } finally {
      setSavingAction(false);
    }
  }

  const stayOpenForAdministrativeActions =
    admission != null && isAdmissionOpenForAdministrativeActions(admission.status);
  const stayClosed = admission != null && isAdmissionClosed(admission.status);
  const showDeclareDeath =
    platformFeatures.admissionDeclareDeath &&
    canDeclareDeath &&
    admission != null &&
    stayOpenForAdministrativeActions;
  const { index: patientCareIndex } = usePatientCareIndex(Boolean(admission));
  const patientCareStatus =
    admission != null
      ? resolvePatientCareStatus(admission.patientId, patient, patientCareIndex)
      : null;
  const showStayActions = !stayClosed && (canManageAdmissions || showDeclareDeath);

  return (
    <>
      <Toast message={actionMessage} onDismiss={() => setActionMessage(null)} />
      {error && <div className="error-banner">{error}</div>}
      {loading && <p style={{ color: 'var(--muted)' }}>Chargement…</p>}

      {!loading && admission && (
        <>
          <div className="admission-stay-overview">
          <div className="card admission-stay-info">
            <h3 className="admission-stay-info__title">Informations séjour</h3>
            <dl className="admission-stay-info__list">
              <div>
                <dt>Patient</dt>
                <dd>
                  <Link to={`/patients/${admission.patientId}`}>
                    {patientName || `Patient #${admission.patientId}`}
                  </Link>
                </dd>
              </div>
              <div>
                <dt>Service</dt>
                <dd>{admission.serviceName}</dd>
              </div>
              <div>
                <dt>Chambre / lit</dt>
                <dd>{[admission.room, admission.bed].filter(Boolean).join(' / ') || '—'}</dd>
              </div>
              <div>
                <dt>Entrée</dt>
                <dd>{formatStayDateTime(admission.admissionDateTime)}</dd>
              </div>
              {admission.dischargeDateTime ? (
                <div>
                  <dt>Sortie</dt>
                  <dd>{formatStayDateTime(admission.dischargeDateTime)}</dd>
                </div>
              ) : null}
              <div>
                <dt>Statut séjour</dt>
                <dd>{admissionStatusLabel(admission.status)}</dd>
              </div>
              {patientCareStatus ? (
                <div>
                  <dt>État patient</dt>
                  <dd>
                    <span className={patientCareStatusBadgeClass(patientCareStatus)}>
                      {patientCareStatusLabel(patientCareStatus)}
                    </span>
                  </dd>
                </div>
              ) : null}
            </dl>
            {admission.reason?.trim() ? (
              <p className="admission-stay-info__reason">
                <span className="admission-stay-info__reason-label">Motif :</span> {admission.reason}
              </p>
            ) : null}
          </div>

          {platformFeatures.admissionClinicalForm ? (
            <div className="card clinical-form-summary-panel">
              <h3 className="clinical-form-summary-panel__title">Formulaire clinique du séjour</h3>
              <p className="clinical-form-summary-panel__hint">
                Consultez la fiche de lecture ou saisissez les éléments cliniques de cette hospitalisation.
              </p>
              <div className="clinical-form-summary-panel__actions">
                <Link className="btn btn-secondary btn-sm" to={`/admissions/${admissionId}/clinical-sheet`}>
                  Voir la fiche
                </Link>
                {canEditClinicalForm ? (
                  <Link className="btn btn-primary btn-sm" to={`/admissions/${admissionId}/clinical-form`}>
                    Saisie clinique
                  </Link>
                ) : null}
              </div>
            </div>
          ) : null}
          </div>

          {showStayActions ? (
            <div className="card" style={{ marginTop: '1rem' }}>
              <h3 style={{ marginTop: 0 }}>Actions de sejour</h3>

              <div className="admission-stay-actions">
                {canManageAdmissions ? (
                <div className="card admission-stay-actions__panel" style={{ margin: 0 }}>
                  <h4 style={{ marginTop: 0, marginBottom: '0.75rem' }}>Transferer</h4>
                  <form
                    onSubmit={onTransferSubmit}
                    className="admission-stay-actions__form"
                  >
                    <div className="field" style={{ marginBottom: 0 }}>
                      <label htmlFor="transfer-service">Service destination *</label>
                      <select
                        id="transfer-service"
                        value={transferService}
                        onChange={(e) => {
                          const v = e.target.value;
                          setTransferService(v);
                          void fetchTransferBedSuggestion(v);
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
                    {transferBedSuggestionMessage && (
                      <div
                        role="status"
                        style={{
                          gridColumn: '1 / -1',
                          padding: '0.5rem 0.65rem',
                          borderRadius: '0.45rem',
                          border: '1px solid rgba(214, 158, 46, 0.55)',
                          background: 'rgba(214, 158, 46, 0.12)',
                          color: 'var(--text)',
                          fontSize: '0.88rem',
                          lineHeight: 1.35,
                        }}
                      >
                        {transferBedSuggestionMessage}
                      </div>
                    )}
                    <div className="field" style={{ marginBottom: 0 }}>
                      <label htmlFor="transfer-room">Chambre</label>
                      <input
                        id="transfer-room"
                        value={transferRoom}
                        onChange={(e) => setTransferRoom(e.target.value)}
                        placeholder="Ex. 202"
                      />
                    </div>
                    <div className="field" style={{ marginBottom: 0 }}>
                      <label htmlFor="transfer-bed">Lit</label>
                      <input
                        id="transfer-bed"
                        value={transferBed}
                        onChange={(e) => setTransferBed(e.target.value)}
                        placeholder="Ex. B"
                      />
                    </div>
                    <div className="field" style={{ marginBottom: 0 }}>
                      <label htmlFor="transfer-note">Note transfert</label>
                      <input
                        id="transfer-note"
                        value={transferNote}
                        onChange={(e) => setTransferNote(e.target.value)}
                        placeholder="Motif du transfert"
                      />
                    </div>
                    <div style={{ gridColumn: '1 / -1' }}>
                      <button
                        type="submit"
                        className="btn btn-primary"
                        disabled={savingAction || !stayOpenForAdministrativeActions}
                      >
                        {savingAction ? 'Traitement...' : 'Transferer'}
                      </button>
                    </div>
                  </form>
                </div>
                ) : null}

                {canManageAdmissions ? (
                  <div className="card admission-stay-actions__panel" style={{ margin: 0 }}>
                    <h4 style={{ marginTop: 0, marginBottom: '0.75rem' }}>Sortie</h4>
                    <div className="field" style={{ marginBottom: '0.75rem' }}>
                      <label htmlFor="discharge-note">Note de sortie</label>
                      <input
                        id="discharge-note"
                        value={dischargeNote}
                        onChange={(e) => setDischargeNote(e.target.value)}
                        placeholder="Optionnel"
                      />
                    </div>
                    <button
                      type="button"
                      className="btn btn-ghost"
                      onClick={() => void onDischarge()}
                      disabled={savingAction || !stayOpenForAdministrativeActions}
                    >
                      {savingAction ? 'Traitement...' : 'Enregistrer sortie'}
                    </button>
                  </div>
                ) : null}

                {showDeclareDeath ? (
                  <DeclareDeathSection
                    embedded
                    patientId={admission.patientId}
                    admissionId={admission.id}
                    contextLabel="Clôture ce séjour et enregistre le décès."
                    disabled={savingAction}
                    onSuccess={() => {
                      setActionMessage('Déclaration de décès enregistrée.');
                      void loadData();
                    }}
                  />
                ) : null}
              </div>
            </div>
          ) : null}
        </>
      )}
    </>
  );
}
