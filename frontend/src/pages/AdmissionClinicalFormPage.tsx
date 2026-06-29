import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import { clinicalFormHasContent } from '../components/ClinicalFormReadView';
import { Toast } from '../components/ui/Toast';
import { ADMISSION_CLOSED_MESSAGE, isAdmissionClosed } from '../utils/admissionStatus';
import {
  applyExamsToUpsertPayload,
  CLINICAL_EXAM_OPTIONS,
  examsSliceFromResponse,
  type ClinicalExamKind,
} from '../utils/clinicalFormExams';
import type {
  AdmissionClinicalFormResponse,
  AdmissionClinicalFormUpsertRequest,
  AdmissionResponse,
  ClinicalMedicalRecordResponse,
  MedicalRecordAntecedentsUpdateRequest,
} from '../api/types';

type ClinicalFormState = {
  antecedentsText: string;
  anamnesisText: string;
  examsRequestedKind: ClinicalExamKind;
  examsRequestedOther: string;
  examsDetailText: string;
  conclusionText: string;
};

const emptyForm: ClinicalFormState = {
  antecedentsText: '',
  anamnesisText: '',
  examsRequestedKind: '',
  examsRequestedOther: '',
  examsDetailText: '',
  conclusionText: '',
};

function fromResponse(data: AdmissionClinicalFormResponse): ClinicalFormState {
  const exams = examsSliceFromResponse(data);
  return {
    antecedentsText: data.antecedentsText ?? '',
    anamnesisText: data.anamnesisText ?? '',
    ...exams,
    conclusionText: data.conclusionText ?? '',
  };
}

function toPayload(form: ClinicalFormState): AdmissionClinicalFormUpsertRequest {
  const t = (value: string) => value.trim() || null;
  return applyExamsToUpsertPayload(
    {
      antecedentsText: t(form.antecedentsText),
      anamnesisText: t(form.anamnesisText),
      conclusionText: t(form.conclusionText),
    },
    {
      examsRequestedKind: form.examsRequestedKind,
      examsRequestedOther: form.examsRequestedOther,
      examsDetailText: form.examsDetailText,
    },
  );
}

export function AdmissionClinicalFormPage() {
  const { user } = useAuth();
  const { id } = useParams<{ id: string }>();
  const admissionId = Number(id);
  const connectedCaregiverName = user?.fullName?.trim() || user?.username?.trim() || 'Utilisateur connecté';

  const [form, setForm] = useState<ClinicalFormState>(emptyForm);
  const [admission, setAdmission] = useState<AdmissionResponse | null>(null);
  const [loadOk, setLoadOk] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [patientId, setPatientId] = useState<number | null>(null);
  const [syncAntecedentsToDossier, setSyncAntecedentsToDossier] = useState(true);
  const [antecedentsPrefilledFromDossier, setAntecedentsPrefilledFromDossier] = useState(false);
  const [savedFormSnapshot, setSavedFormSnapshot] = useState<ClinicalFormState | null>(null);

  const admissionClosed = isAdmissionClosed(admission?.status);
  const clinicalFormLocked = admissionClosed;

  useEffect(() => {
    if (!Number.isFinite(admissionId)) {
      setError("ID d'admission invalide.");
      setLoadOk(false);
      setLoading(false);
      return;
    }
    void loadForm();
  }, [admissionId]);

  async function loadForm() {
    setLoading(true);
    setError(null);
    setMessage(null);
    setLoadOk(false);
    try {
      const [formRes, admissionRes] = await Promise.all([
        api.get<AdmissionClinicalFormResponse>(`/api/v1/admissions/${admissionId}/clinical-form`),
        api.get<AdmissionResponse>(`/api/v1/admissions/${admissionId}`),
      ]);
      const admission = admissionRes.data;
      const loadedForm = fromResponse(formRes.data);
      let prefilled = false;

      setPatientId(admission.patientId);
      if (!loadedForm.antecedentsText.trim()) {
        try {
          const recordRes = await api.get<ClinicalMedicalRecordResponse>(
            `/api/v1/patients/${admission.patientId}/medical-record`,
          );
          const dossierAntecedents = recordRes.data.antecedents?.trim();
          if (dossierAntecedents) {
            loadedForm.antecedentsText = dossierAntecedents;
            prefilled = true;
          }
        } catch {
          /* dossier indisponible : formulaire vide */
        }
      }

      setForm(loadedForm);
      setSavedFormSnapshot(loadedForm);
      setAntecedentsPrefilledFromDossier(prefilled);
      setAdmission(admission);
      setLoadOk(true);
    } catch {
      setError('Impossible de charger le formulaire clinique.');
      setAdmission(null);
    } finally {
      setLoading(false);
    }
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      if (syncAntecedentsToDossier && patientId != null && !clinicalFormLocked) {
        const antecedentsPayload: MedicalRecordAntecedentsUpdateRequest = {
          antecedents: form.antecedentsText.trim() || null,
        };
        await api.patch(`/api/v1/patients/${patientId}/medical-record/antecedents`, antecedentsPayload);
      }
      const { data } = await api.put<AdmissionClinicalFormResponse>(
        `/api/v1/admissions/${admissionId}/clinical-form`,
        toPayload(form),
      );
      const nextForm = fromResponse(data);
      setForm(nextForm);
      setSavedFormSnapshot(nextForm);
      setAntecedentsPrefilledFromDossier(false);
      setMessage('Formulaire clinique enregistré.');
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'enregistrer le formulaire clinique."));
    } finally {
      setSaving(false);
    }
  }

  function updateField<K extends keyof ClinicalFormState>(key: K, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  return (
    <div className="clinical-form-page">
      <Toast message={message} onDismiss={() => setMessage(null)} />
      {error && <div className="error-banner">{error}</div>}
      {loading && <p className="loading-block">Chargement du formulaire…</p>}

      {connectedCaregiverName ||
      (!loading && loadOk && clinicalFormHasContent(savedFormSnapshot ?? form)) ? (
        <div className="clinical-form-page__toolbar">
          {connectedCaregiverName ? (
            <span className="clinical-form-page__byline">Saisie par {connectedCaregiverName}</span>
          ) : (
            <span aria-hidden="true" />
          )}
          {!loading && loadOk && clinicalFormHasContent(savedFormSnapshot ?? form) ? (
            <Link className="clinical-form-page__sheet-link" to={`/admissions/${admissionId}/clinical-sheet`}>
              Voir la fiche clinique (lecture seule)
            </Link>
          ) : null}
        </div>
      ) : null}

      {!loading && loadOk && (
        <>
          {admissionClosed ? (
            <div
              className="card"
              style={{
                marginBottom: '1rem',
                borderColor: 'rgba(232, 93, 106, 0.45)',
                background: 'rgba(232, 93, 106, 0.06)',
              }}
            >
              <strong>Séjour clôturé</strong>
              <p style={{ margin: '0.35rem 0 0', color: 'var(--muted)' }}>{ADMISSION_CLOSED_MESSAGE}</p>
            </div>
          ) : null}

          <form onSubmit={onSubmit} className="card" style={{ display: 'grid', gap: '0.85rem' }}>
          <h4 className="clinical-section-title">Antécédents et anamnèse</h4>
          <div className="clinical-form-grid">
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="antecedentsText">Antécédents</label>
              {antecedentsPrefilledFromDossier ? (
                <p style={{ margin: '0 0 0.35rem', color: 'var(--muted)', fontSize: '0.85rem' }}>
                  Repris du dossier patient (aucun antécédent enregistré pour ce séjour).
                </p>
              ) : null}
              <textarea
                id="antecedentsText"
                rows={4}
                value={form.antecedentsText}
                onChange={(e) => updateField('antecedentsText', e.target.value)}
                disabled={clinicalFormLocked}
                readOnly={clinicalFormLocked}
              />
              {!clinicalFormLocked && patientId != null ? (
                <label
                  style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '0.5rem',
                    marginTop: '0.5rem',
                    fontSize: '0.9rem',
                    cursor: 'pointer',
                  }}
                >
                  <input
                    type="checkbox"
                    checked={syncAntecedentsToDossier}
                    onChange={(e) => setSyncAntecedentsToDossier(e.target.checked)}
                    style={{ marginTop: '0.2rem' }}
                  />
                  <span>
                    Mettre à jour les antécédents du dossier patient lors de l’enregistrement
                  </span>
                </label>
              ) : null}
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="anamnesisText">Anamnèse (histoire de la maladie)</label>
              <textarea
                id="anamnesisText"
                rows={4}
                value={form.anamnesisText}
                onChange={(e) => updateField('anamnesisText', e.target.value)}
                disabled={clinicalFormLocked}
                readOnly={clinicalFormLocked}
              />
            </div>
          </div>

          <h4 className="clinical-section-title">Examens</h4>
          <div className="clinical-form-grid">
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="examsRequestedKind">Examen à passer</label>
              <select
                id="examsRequestedKind"
                value={form.examsRequestedKind}
                onChange={(e) => updateField('examsRequestedKind', e.target.value as ClinicalExamKind)}
                disabled={clinicalFormLocked}
              >
                {CLINICAL_EXAM_OPTIONS.map((option) => (
                  <option key={option.value || 'empty'} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              {form.examsRequestedKind === 'AUTRE' ? (
                <input
                  id="examsRequestedOther"
                  type="text"
                  value={form.examsRequestedOther}
                  onChange={(e) => updateField('examsRequestedOther', e.target.value)}
                  placeholder="Préciser l'examen"
                  disabled={clinicalFormLocked}
                  readOnly={clinicalFormLocked}
                  style={{ marginTop: '0.5rem' }}
                />
              ) : null}
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="examsDetailText">Détail de l&apos;examen</label>
              <textarea
                id="examsDetailText"
                rows={5}
                value={form.examsDetailText}
                onChange={(e) => updateField('examsDetailText', e.target.value)}
                placeholder="Constatations, résultats, observations…"
                disabled={clinicalFormLocked}
                readOnly={clinicalFormLocked}
              />
            </div>
          </div>

          <h4 className="clinical-section-title">Conclusion</h4>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="conclusionText">Conclusion</label>
            <textarea
              id="conclusionText"
              rows={5}
              value={form.conclusionText}
              onChange={(e) => updateField('conclusionText', e.target.value)}
              disabled={clinicalFormLocked}
              readOnly={clinicalFormLocked}
            />
          </div>
          <div>
            <button type="submit" className="btn btn-primary" disabled={saving || clinicalFormLocked}>
              {saving ? 'Enregistrement…' : 'Enregistrer le formulaire'}
            </button>
          </div>
        </form>
        </>
      )}
    </div>
  );
}
