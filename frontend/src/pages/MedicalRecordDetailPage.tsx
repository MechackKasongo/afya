import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { ClinicalDossierGlobalPanel } from '../components/ClinicalDossierGlobalPanel';
import { PatientAdmissionEpisodes } from '../components/PatientAdmissionEpisodes';
import { MedicalRecordTabs } from '../components/ui/MedicalRecordTabs';
import { PageHeader } from '../components/ui/PageHeader';
import { Toast } from '../components/ui/Toast';
import { LoadingBlock } from '../components/ui/LoadingBlock';
import type {
  AntecedentType,
  ClinicalDocumentResponse,
  ClinicalMedicalRecordResponse,
  ClinicalNoteRequest,
  MedicalAntecedentCreateRequest,
  MedicalAntecedentResponse,
  PatientResponse,
} from '../api/types';

const ANTECEDENT_TYPE_LABELS: Record<AntecedentType, string> = {
  MEDICAL: 'Médical',
  CHIRURGICAL: 'Chirurgical',
  FAMILIAL: 'Familial',
  ALLERGIE: 'Allergie',
};

const NON_ALLERGY_TYPES: AntecedentType[] = ['MEDICAL', 'CHIRURGICAL', 'FAMILIAL'];
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

function formatEventDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
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
  const [antecedents, setAntecedents] = useState<MedicalAntecedentResponse[]>([]);
  const [newAllergyDesc, setNewAllergyDesc] = useState('');
  const [newAntType, setNewAntType] = useState<AntecedentType>('MEDICAL');
  const [newAntDesc, setNewAntDesc] = useState('');
  const [expandedAntecedentIds, setExpandedAntecedentIds] = useState<Set<number>>(new Set());
  const [patientName, setPatientName] = useState('');

  function toggleAntecedentExpanded(id: number) {
    setExpandedAntecedentIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }
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
      const [recordRes, patientRes, antecedentsRes] = await Promise.all([
        api.get<ClinicalMedicalRecordResponse>(`/api/v1/patients/${parsedPatientId}/medical-record`),
        api.get<PatientResponse>(`/api/v1/patients/${parsedPatientId}`),
        api.get<MedicalAntecedentResponse[]>(`/api/v1/patients/${parsedPatientId}/medical-antecedents`),
      ]);
      setRecord(recordRes.data);
      setPatientProfile(patientRes.data);
      setAntecedents(antecedentsRes.data);
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

  async function addAntecedent(payload: MedicalAntecedentCreateRequest, successMessage: string) {
    setPending(true);
    setError(null);
    setMessage(null);
    try {
      await api.post<MedicalAntecedentResponse>(
        `/api/v1/patients/${parsedPatientId}/medical-antecedents`,
        payload,
      );
      const { data } = await api.get<MedicalAntecedentResponse[]>(
        `/api/v1/patients/${parsedPatientId}/medical-antecedents`,
      );
      setAntecedents(data);
      setMessage(successMessage);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'ajouter l'entrée."));
    } finally {
      setPending(false);
    }
  }

  async function onAddAllergy(e: React.FormEvent) {
    e.preventDefault();
    const description = newAllergyDesc.trim();
    if (!description) {
      setError('Décrivez l’allergie à ajouter.');
      return;
    }
    await addAntecedent(
      { type: 'ALLERGIE', description, eventDate: null },
      'Allergie ajoutée.',
    );
    setNewAllergyDesc('');
  }

  async function onAddAntecedent(e: React.FormEvent) {
    e.preventDefault();
    const description = newAntDesc.trim();
    if (!description) {
      setError('Décrivez l’antécédent à ajouter.');
      return;
    }
    await addAntecedent(
      { type: newAntType, description, eventDate: null },
      'Antécédent ajouté.',
    );
    setNewAntDesc('');
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

  const allergyEntries = antecedents.filter((a) => a.type === 'ALLERGIE');
  const antecedentEntries = antecedents.filter((a) => a.type !== 'ALLERGIE');

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
      {loading && <LoadingBlock label="Chargement du dossier…" />}

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
              {allergyEntries.length > 0 ? (
                <ul className="antecedent-list">
                  {allergyEntries.map((item) => (
                    <li
                      key={item.id}
                      className={`antecedent-list__item antecedent-list__item--clickable${
                        expandedAntecedentIds.has(item.id) ? ' antecedent-list__item--expanded' : ''
                      }`}
                      role="button"
                      tabIndex={0}
                      aria-expanded={expandedAntecedentIds.has(item.id)}
                      onClick={() => toggleAntecedentExpanded(item.id)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          toggleAntecedentExpanded(item.id);
                        }
                      }}
                    >
                      <span className="antecedent-list__desc">{item.description}</span>
                      <span className="antecedent-list__date">{formatEventDate(item.createdAt)}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="hint" style={{ marginTop: 0 }}>Aucune allergie enregistrée.</p>
              )}
              <form onSubmit={onAddAllergy} className="antecedent-add-form">
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="new-allergy-desc">Ajouter une allergie</label>
                  <input
                    id="new-allergy-desc"
                    value={newAllergyDesc}
                    onChange={(e) => setNewAllergyDesc(e.target.value)}
                    placeholder="Ex. pénicilline, arachides, latex…"
                  />
                </div>
                <button type="submit" className="btn btn-primary" disabled={pending}>
                  {pending ? 'Ajout…' : 'Ajouter'}
                </button>
              </form>
            </section>
            <section className="card medical-record-resume-cards__item">
              <h2 className="clinical-section__title" style={{ marginTop: 0 }}>
                Antécédents médicaux
              </h2>
              {antecedentEntries.length > 0 ? (
                <ul className="antecedent-list">
                  {antecedentEntries.map((item) => (
                    <li
                      key={item.id}
                      className={`antecedent-list__item antecedent-list__item--clickable${
                        expandedAntecedentIds.has(item.id) ? ' antecedent-list__item--expanded' : ''
                      }`}
                      role="button"
                      tabIndex={0}
                      aria-expanded={expandedAntecedentIds.has(item.id)}
                      onClick={() => toggleAntecedentExpanded(item.id)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          toggleAntecedentExpanded(item.id);
                        }
                      }}
                    >
                      <span className="antecedent-list__type">{ANTECEDENT_TYPE_LABELS[item.type]}</span>
                      <span className="antecedent-list__desc">{item.description}</span>
                      <span className="antecedent-list__date">{formatEventDate(item.createdAt)}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="hint" style={{ marginTop: 0 }}>Aucun antécédent enregistré.</p>
              )}
              <form onSubmit={onAddAntecedent} className="antecedent-add-form">
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="new-ant-type">Type</label>
                  <select
                    id="new-ant-type"
                    value={newAntType}
                    onChange={(e) => setNewAntType(e.target.value as AntecedentType)}
                  >
                    {NON_ALLERGY_TYPES.map((t) => (
                      <option key={t} value={t}>
                        {ANTECEDENT_TYPE_LABELS[t]}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="new-ant-desc">Description</label>
                  <input
                    id="new-ant-desc"
                    value={newAntDesc}
                    onChange={(e) => setNewAntDesc(e.target.value)}
                    placeholder="Ex. hypertension, appendicectomie…"
                  />
                </div>
                <button type="submit" className="btn btn-primary" disabled={pending}>
                  {pending ? 'Ajout…' : 'Ajouter'}
                </button>
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
