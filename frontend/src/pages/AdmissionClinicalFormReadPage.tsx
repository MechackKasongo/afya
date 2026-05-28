import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import { hasRole } from '../auth/roles';
import { ClinicalFormReadView, clinicalFormHasContent } from '../components/ClinicalFormReadView';
import type { AdmissionClinicalFormResponse, AdmissionResponse } from '../api/types';
import { admissionStatusLabel } from '../utils/admissionStatus';

function formatAdmissionDate(iso: string): string {
  return new Date(iso).toLocaleString('fr-FR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function AdmissionClinicalFormReadPage() {
  const { user } = useAuth();
  const { id } = useParams<{ id: string }>();
  const admissionId = Number(id);
  const canEditClinicalForm = hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_MEDECIN');

  const [admission, setAdmission] = useState<AdmissionResponse | null>(null);
  const [form, setForm] = useState<AdmissionClinicalFormResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (!Number.isFinite(admissionId)) {
      setError("ID d'admission invalide.");
      setLoading(false);
      return;
    }
    void loadSheet();
  }, [admissionId]);

  async function loadSheet() {
    setLoading(true);
    setError(null);
    try {
      const [formRes, admissionRes] = await Promise.all([
        api.get<AdmissionClinicalFormResponse>(`/api/v1/admissions/${admissionId}/clinical-form`),
        api.get<AdmissionResponse>(`/api/v1/admissions/${admissionId}`),
      ]);
      setForm(formRes.data);
      setAdmission(admissionRes.data);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger la fiche clinique du séjour.'));
      setForm(null);
      setAdmission(null);
    } finally {
      setLoading(false);
    }
  }

  const roomBed =
    admission != null ? [admission.room, admission.bed].filter(Boolean).join(' / ') || '—' : '—';

  return (
    <div className="clinical-form-read-page">
      <p style={{ margin: '0 0 0.75rem', color: 'var(--muted)', fontSize: '0.9rem' }}>
        Lecture seule — synthèse du formulaire clinique enregistré pour cette hospitalisation.
      </p>

      {error && <div className="error-banner">{error}</div>}
      {loading && <p className="loading-block">Chargement de la fiche…</p>}

      {!loading && !error && admission && (
        <div className="card clinical-form-read-page__card">
          <header className="clinical-form-read-page__header">
            <h2 className="clinical-form-read-page__title">Fiche clinique du séjour</h2>
            <dl className="clinical-form-read-page__meta-list">
              <div>
                <dt>Admission</dt>
                <dd>{admission.id}</dd>
              </div>
              <div>
                <dt>Service</dt>
                <dd>{admission.serviceName}</dd>
              </div>
              <div>
                <dt>Chambre / lit</dt>
                <dd>{roomBed}</dd>
              </div>
              <div>
                <dt>Entrée</dt>
                <dd>{formatAdmissionDate(admission.admissionDateTime)}</dd>
              </div>
              {admission.dischargeDateTime ? (
                <div>
                  <dt>Sortie</dt>
                  <dd>{formatAdmissionDate(admission.dischargeDateTime)}</dd>
                </div>
              ) : null}
              <div>
                <dt>Statut</dt>
                <dd>{admissionStatusLabel(admission.status)}</dd>
              </div>
            </dl>
            {admission.reason?.trim() ? (
              <p className="clinical-form-read-page__reason">
                <span className="clinical-form-read-page__reason-label">Motif :</span> {admission.reason}
              </p>
            ) : null}
          </header>

          <ClinicalFormReadView
            admissionId={admissionId}
            form={form}
            variant="full"
            showActions
            editAllowed={canEditClinicalForm}
          />

          {!clinicalFormHasContent(form) && canEditClinicalForm ? (
            <p style={{ margin: '1rem 0 0', fontSize: '0.9rem' }}>
              <Link className="btn btn-primary btn-sm" to={`/admissions/${admissionId}/clinical-form`}>
                Remplir le formulaire clinique
              </Link>
            </p>
          ) : null}

          <p style={{ margin: '1rem 0 0', fontSize: '0.9rem' }}>
            <Link to={`/medical-records/${admission.patientId}?tab=stays`}>
              Retour aux séjours du dossier médical
            </Link>
          </p>
        </div>
      )}
    </div>
  );
}
