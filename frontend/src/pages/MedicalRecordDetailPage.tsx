import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { ClinicalDossierGlobalPanel } from '../components/ClinicalDossierGlobalPanel';
import { PatientAdmissionEpisodes } from '../components/PatientAdmissionEpisodes';
import { MedicalRecordTabs } from '../components/ui/MedicalRecordTabs';
import { PageHeader } from '../components/ui/PageHeader';
import { Toast } from '../components/ui/Toast';
import type {
  ClinicalDocumentResponse,
  ClinicalMedicalRecordResponse,
  ClinicalNoteRequest,
  MedicalRecordAllergiesUpdateRequest,
  MedicalRecordAntecedentsUpdateRequest,
  PatientResponse,
} from '../api/types';
import { platformFeatures } from '../config/features';

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('fr-FR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function globalCountsLabel(record: ClinicalMedicalRecordResponse): string {
  const docCount = record.documents?.length ?? 0;
  const parts = [`${record.notes.length} note(s)`];
  if (platformFeatures.clinicalDocumentsUpload) {
    parts.push(`${docCount} document(s)`);
  }
  return parts.join(' · ');
}

export function MedicalRecordDetailPage() {
  const { patientId } = useParams<{ patientId: string }>();
  const parsedPatientId = Number(patientId);
  const [searchParams] = useSearchParams();

  const highlightAdmissionRaw = searchParams.get('admission');
  const highlightAdmissionId =
    highlightAdmissionRaw != null && Number.isFinite(Number(highlightAdmissionRaw))
      ? Number(highlightAdmissionRaw)
      : null;

  const recordTab = searchParams.get('tab') ?? (searchParams.get('global') === '1' ? 'global' : 'resume');

  const [record, setRecord] = useState<ClinicalMedicalRecordResponse | null>(null);
  const [patientProfile, setPatientProfile] = useState<PatientResponse | null>(null);
  const [allergiesText, setAllergiesText] = useState('');
  const [antecedentsText, setAntecedentsText] = useState('');
  const [patientName, setPatientName] = useState('');
  const [globalOpen, setGlobalOpen] = useState(recordTab === 'global');
  const [noteText, setNoteText] = useState('');
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [uploadTitle, setUploadTitle] = useState('');
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  useEffect(() => {
    if (!Number.isFinite(parsedPatientId)) {
      setError('ID patient invalide.');
      setLoading(false);
      return;
    }
    void loadData();
  }, [parsedPatientId]);

  useEffect(() => {
    setGlobalOpen(recordTab === 'global');
  }, [recordTab]);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [recordRes, patientRes] = await Promise.all([
        api.get<ClinicalMedicalRecordResponse>(`/api/v1/patients/${parsedPatientId}/medical-record`),
        api.get<PatientResponse>(`/api/v1/patients/${parsedPatientId}`),
      ]);
      setRecord(recordRes.data);
      setPatientProfile(patientRes.data);
      setAllergiesText(recordRes.data.allergies ?? '');
      setAntecedentsText(recordRes.data.antecedents ?? '');
      setPatientName(
        recordRes.data.patientName ||
          `${patientRes.data.firstName} ${patientRes.data.lastName}`.trim() ||
          `Patient ${parsedPatientId}`,
      );
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger le dossier clinique.'));
    } finally {
      setLoading(false);
    }
  }

  async function onSaveAllergies(e: React.FormEvent) {
    e.preventDefault();
    setPending(true);
    setError(null);
    setMessage(null);
    try {
      const payload: MedicalRecordAllergiesUpdateRequest = {
        allergies: allergiesText.trim() || null,
      };
      const { data } = await api.patch<ClinicalMedicalRecordResponse>(
        `/api/v1/patients/${parsedPatientId}/medical-record/allergies`,
        payload,
      );
      setRecord(data);
      setAllergiesText(data.allergies ?? '');
      setMessage('Allergies enregistrées.');
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible d’enregistrer les allergies.'));
    } finally {
      setPending(false);
    }
  }

  async function onSaveAntecedents(e: React.FormEvent) {
    e.preventDefault();
    setPending(true);
    setError(null);
    setMessage(null);
    try {
      const payload: MedicalRecordAntecedentsUpdateRequest = {
        antecedents: antecedentsText.trim() || null,
      };
      const { data } = await api.patch<ClinicalMedicalRecordResponse>(
        `/api/v1/patients/${parsedPatientId}/medical-record/antecedents`,
        payload,
      );
      setRecord(data);
      setAntecedentsText(data.antecedents ?? '');
      setMessage('Antécédents enregistrés.');
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible d’enregistrer les antécédents.'));
    } finally {
      setPending(false);
    }
  }

  async function uploadDocument(e: React.FormEvent) {
    e.preventDefault();
    if (!platformFeatures.clinicalDocumentsUpload) return;
    if (!uploadFile) {
      setError('Choisissez un fichier à déposer.');
      return;
    }
    setPending(true);
    setError(null);
    setMessage(null);
    try {
      const form = new FormData();
      form.append('file', uploadFile);
      if (uploadTitle.trim()) {
        form.append('title', uploadTitle.trim());
      }
      await api.post<ClinicalDocumentResponse>(
        `/api/v1/patients/${parsedPatientId}/documents/upload`,
        form,
        { headers: { 'Content-Type': 'multipart/form-data' } },
      );
      setUploadFile(null);
      setUploadTitle('');
      setMessage('Document enregistré.');
      setGlobalOpen(true);
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de déposer le document.'));
    } finally {
      setPending(false);
    }
  }

  function downloadDocument(doc: ClinicalDocumentResponse) {
    const url = `${api.defaults.baseURL ?? ''}/api/v1/patients/${parsedPatientId}/documents/${doc.id}/download`;
    void api
      .get(url, { responseType: 'blob' })
      .then((res) => {
        const blobUrl = URL.createObjectURL(res.data);
        const a = document.createElement('a');
        a.href = blobUrl;
        a.download = doc.title || `document-${doc.id}`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(blobUrl);
      })
      .catch(() => setError('Téléchargement impossible.'));
  }

  async function addNote(e: React.FormEvent) {
    e.preventDefault();
    const payload: ClinicalNoteRequest = { narrative: noteText.trim() };
    if (!payload.narrative) {
      setError('Le texte de la note est obligatoire.');
      return;
    }
    setPending(true);
    setError(null);
    setMessage(null);
    try {
      await api.post(`/api/v1/patients/${parsedPatientId}/medical-record/notes`, payload);
      setNoteText('');
      setMessage('Note clinique ajoutée.');
      setGlobalOpen(true);
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'ajouter la note."));
    } finally {
      setPending(false);
    }
  }

  return (
    <>
      <PageHeader title={`Dossier clinique — ${patientName || `Patient ${patientId}`}`} />

      <p className="page-breadcrumb">
        <Link to="/medical-records">← Dossiers médicaux</Link>
        <Link to="/patients" style={{ marginLeft: '0.75rem' }}>
          Registre patients
        </Link>
      </p>

      <Toast message={message} onDismiss={() => setMessage(null)} />
      {error && <div className="error-banner">{error}</div>}
      {loading && <p className="loading-block">Chargement du dossier…</p>}

      {!loading && record && (
        <>
          <MedicalRecordTabs />

          {(recordTab === 'resume' || recordTab === 'stays' || recordTab === 'global') && (
            <>
              <div className="clinical-meta-bar" role="group" aria-label="Informations du dossier">
                <span className="clinical-meta-bar__item">
                  <span className="clinical-meta-bar__label">N° dossier</span>
                  <span className="clinical-meta-bar__value patient-dossier-number">
                    {record.dossierNumber || '—'}
                  </span>
                </span>
                <span className="clinical-meta-bar__sep" aria-hidden />
                <span className="clinical-meta-bar__item">
                  <span className="clinical-meta-bar__label">Ouvert le</span>
                  <span className="clinical-meta-bar__value">
                    {record.openedAt ? formatDateTime(record.openedAt) : '—'}
                  </span>
                </span>
                <span className="clinical-meta-bar__sep" aria-hidden />
                <span className="clinical-meta-bar__item">
                  <span className="clinical-meta-bar__label">Groupe sanguin</span>
                  <span className="clinical-meta-bar__value medical-record-blood-group">
                    {patientProfile?.bloodGroup?.trim() || '—'}
                  </span>
                </span>
                <span className="clinical-meta-bar__sep" aria-hidden />
                <span className="clinical-meta-bar__item">
                  <span className="clinical-meta-bar__label">Taille</span>
                  <span className="clinical-meta-bar__value">
                    {patientProfile?.heightCm != null ? `${patientProfile.heightCm} cm` : '—'}
                  </span>
                </span>
              </div>
              {recordTab === 'resume' ? (
                <p style={{ margin: '0 0 1rem', fontSize: '0.9rem' }}>
                  <Link to={`/patients/${parsedPatientId}`}>Modifier le groupe sanguin et la taille</Link> sur la
                  fiche patient.
                </p>
              ) : null}
            </>
          )}

          {recordTab === 'resume' && (
          <>
          <div className="medical-record-resume-cards">
            <section className="card medical-record-resume-cards__item">
              <h2 className="clinical-section__title" style={{ marginTop: 0 }}>
                Allergies et intolérances
              </h2>
              <form onSubmit={onSaveAllergies} style={{ display: 'grid', gap: '0.75rem' }}>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="allergies-text">Allergies connues</label>
                  <textarea
                    id="allergies-text"
                    rows={5}
                    value={allergiesText}
                    onChange={(e) => setAllergiesText(e.target.value)}
                    placeholder="Ex. pénicilline, arachides, latex…"
                  />
                </div>
                <div>
                  <button type="submit" className="btn btn-primary" disabled={pending}>
                    {pending ? 'Enregistrement…' : 'Enregistrer les allergies'}
                  </button>
                </div>
              </form>
            </section>
            <section className="card medical-record-resume-cards__item">
              <h2 className="clinical-section__title" style={{ marginTop: 0 }}>
                Antécédents médicaux
              </h2>
              <form onSubmit={onSaveAntecedents} style={{ display: 'grid', gap: '0.75rem' }}>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="antecedents-text">Antécédents</label>
                  <textarea
                    id="antecedents-text"
                    rows={5}
                    value={antecedentsText}
                    onChange={(e) => setAntecedentsText(e.target.value)}
                    placeholder="Antécédents personnels, familiaux, chirurgicaux, traitements habituels…"
                  />
                </div>
                <div>
                  <button type="submit" className="btn btn-primary" disabled={pending}>
                    {pending ? 'Enregistrement…' : 'Enregistrer les antécédents'}
                  </button>
                </div>
              </form>
            </section>
          </div>
          </>
          )}

          {recordTab === 'stays' && (
          <section className="card" style={{ marginBottom: '1rem' }}>
            <h2 className="clinical-section__title" style={{ marginTop: 0 }}>
              Hospitalisations
            </h2>
            <p style={{ color: 'var(--muted)', marginTop: 0, marginBottom: '1rem', fontSize: '0.9rem' }}>
              Dépliez un séjour pour un aperçu, ou ouvrez la fiche complète en lecture seule. La fiche admission
              sert au transfert et à la sortie.
            </p>
            <PatientAdmissionEpisodes
              patientId={parsedPatientId}
              highlightAdmissionId={highlightAdmissionId}
            />
          </section>
          )}

          {recordTab === 'global' && (
          <section id="dossier-global" className="dossier-global-section">
            <button
              type="button"
              className={`dossier-global-section__toggle${globalOpen ? ' dossier-global-section__toggle--open' : ''}`}
              onClick={() => setGlobalOpen((v) => !v)}
              aria-expanded={globalOpen}
            >
              <span className="dossier-global-section__chevron" aria-hidden>
                {globalOpen ? '▼' : '▶'}
              </span>
              <span className="dossier-global-section__label">
                <strong>Dossier global</strong>
                <span className="dossier-global-section__counts">{globalCountsLabel(record)}</span>
              </span>
              <span className="dossier-global-section__action">
                {globalOpen ? 'Replier' : 'Afficher / ajouter'}
              </span>
            </button>

            {globalOpen && (
              <div className="dossier-global-section__body">
                <ClinicalDossierGlobalPanel
                  record={record}
                  pending={pending}
                  noteText={noteText}
                  onNoteTextChange={setNoteText}
                  onAddNote={(e) => void addNote(e)}
                  uploadTitle={uploadTitle}
                  uploadFile={uploadFile}
                  onUploadTitleChange={setUploadTitle}
                  onUploadFileChange={setUploadFile}
                  onUploadDocument={(e) => void uploadDocument(e)}
                  onDownloadDocument={downloadDocument}
                />
              </div>
            )}
          </section>
          )}
        </>
      )}
    </>
  );
}
