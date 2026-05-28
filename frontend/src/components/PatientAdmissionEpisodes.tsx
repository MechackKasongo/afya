import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { hasRole } from '../auth/roles';
import { ClinicalFormSummary } from './ClinicalFormSummary';
import { platformFeatures } from '../config/features';
import type { AdmissionClinicalFormResponse, AdmissionResponse, PageAdmissionResponse } from '../api/types';
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

type EpisodeFormState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'ok'; form: AdmissionClinicalFormResponse }
  | { status: 'error' }
  | { status: 'skipped' };

type PatientAdmissionEpisodesProps = {
  patientId: number;
  /** Ouvre automatiquement cette admission (ex. lien depuis la fiche admission). */
  highlightAdmissionId?: number | null;
};

export function PatientAdmissionEpisodes({ patientId, highlightAdmissionId }: PatientAdmissionEpisodesProps) {
  const { user } = useAuth();
  const canEditClinicalForm = hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_MEDECIN');

  const [admissions, setAdmissions] = useState<AdmissionResponse[]>([]);
  const [loadingList, setLoadingList] = useState(true);
  const [listError, setListError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(highlightAdmissionId ?? null);
  const [formsByAdmission, setFormsByAdmission] = useState<Record<number, EpisodeFormState>>({});

  useEffect(() => {
    void loadAdmissions();
  }, [patientId]);

  useEffect(() => {
    if (highlightAdmissionId != null) {
      setExpandedId(highlightAdmissionId);
    }
  }, [highlightAdmissionId]);

  useEffect(() => {
    if (expandedId != null && platformFeatures.admissionClinicalForm) {
      void loadFormForAdmission(expandedId);
    }
  }, [expandedId]);

  async function loadAdmissions() {
    setLoadingList(true);
    setListError(null);
    try {
      const params = new URLSearchParams({
        patientId: String(patientId),
        page: '0',
        size: '100',
      });
      const { data } = await api.get<PageAdmissionResponse>(`/api/v1/admissions?${params}`);
      setAdmissions(data.content);
      if (highlightAdmissionId == null && data.content.length === 1) {
        setExpandedId(data.content[0].id);
      }
    } catch {
      setListError('Impossible de charger les hospitalisations de ce patient.');
    } finally {
      setLoadingList(false);
    }
  }

  async function loadFormForAdmission(admissionId: number) {
    const existing = formsByAdmission[admissionId];
    if (existing?.status === 'loading' || existing?.status === 'ok') {
      return;
    }
    setFormsByAdmission((prev) => ({ ...prev, [admissionId]: { status: 'loading' } }));
    try {
      const { data } = await api.get<AdmissionClinicalFormResponse>(
        `/api/v1/admissions/${admissionId}/clinical-form`,
      );
      setFormsByAdmission((prev) => ({ ...prev, [admissionId]: { status: 'ok', form: data } }));
    } catch {
      setFormsByAdmission((prev) => ({ ...prev, [admissionId]: { status: 'error' } }));
    }
  }

  function toggle(admissionId: number) {
    setExpandedId((current) => (current === admissionId ? null : admissionId));
  }

  if (loadingList) {
    return <p className="loading-block">Chargement des hospitalisations…</p>;
  }
  if (listError) {
    return <p className="clinical-empty">{listError}</p>;
  }
  if (admissions.length === 0) {
    return (
      <p className="clinical-empty">
        Aucune hospitalisation enregistrée. Les admissions apparaîtront ici avec leur formulaire clinique de
        séjour.
      </p>
    );
  }

  return (
    <div className="admission-episodes">
      {admissions.map((adm) => {
        const isOpen = expandedId === adm.id;
        const formState = formsByAdmission[adm.id];
        const roomBed = [adm.room, adm.bed].filter(Boolean).join(' / ') || '—';

        return (
          <article
            key={adm.id}
            className={`admission-episode${isOpen ? ' admission-episode--open' : ''}${
              highlightAdmissionId === adm.id ? ' admission-episode--highlight' : ''
            }`}
            id={`admission-${adm.id}`}
          >
            <button
              type="button"
              className="admission-episode__header"
              onClick={() => toggle(adm.id)}
              aria-expanded={isOpen}
            >
              <span className="admission-episode__chevron" aria-hidden>
                {isOpen ? '▼' : '▶'}
              </span>
              <span className="admission-episode__main">
                <strong className="admission-episode__title">
                  Admission {adm.id} — {adm.serviceName}
                </strong>
                <span className="admission-episode__meta">
                  {formatAdmissionDate(adm.admissionDateTime)}
                  {adm.dischargeDateTime ? ` → ${formatAdmissionDate(adm.dischargeDateTime)}` : ''}
                  {' · '}
                  {roomBed}
                  {' · '}
                  <span className={`admission-episode__status admission-episode__status--${adm.status.toLowerCase()}`}>
                    {admissionStatusLabel(adm.status)}
                  </span>
                </span>
              </span>
            </button>

            {isOpen && (
              <div className="admission-episode__body">
                {adm.reason?.trim() && (
                  <p className="admission-episode__reason">
                    <span className="admission-episode__reason-label">Motif :</span> {adm.reason}
                  </p>
                )}

                {platformFeatures.admissionClinicalForm ? (
                  <div className="admission-episode__form-block">
                    <h4 className="admission-episode__form-title">Formulaire clinique du séjour</h4>
                    {(!formState || formState.status === 'loading') && (
                      <p style={{ color: 'var(--muted)', margin: 0, fontSize: '0.9rem' }}>
                        Chargement du formulaire…
                      </p>
                    )}
                    {formState?.status === 'error' && (
                      <p style={{ color: 'var(--muted)', margin: 0, fontSize: '0.9rem' }}>
                        Formulaire non disponible pour cette admission.
                      </p>
                    )}
                    {formState?.status === 'ok' && (
                      <ClinicalFormSummary
                        admissionId={adm.id}
                        form={formState.form}
                        editAllowed={canEditClinicalForm}
                      />
                    )}
                  </div>
                ) : null}

                <div className="admission-episode__actions">
                  <Link className="btn btn-secondary btn-sm" to={`/admissions/${adm.id}`}>
                    Fiche admission
                  </Link>
                  {platformFeatures.admissionClinicalForm && (
                    <>
                      <Link className="btn btn-secondary btn-sm" to={`/admissions/${adm.id}/clinical-sheet`}>
                        Voir la fiche
                      </Link>
                      {canEditClinicalForm ? (
                        <Link className="btn btn-primary btn-sm" to={`/admissions/${adm.id}/clinical-form`}>
                          Modifier le formulaire
                        </Link>
                      ) : null}
                    </>
                  )}
                </div>
              </div>
            )}
          </article>
        );
      })}
    </div>
  );
}
