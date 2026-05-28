import type {
  ClinicalDocumentResponse,
  ClinicalMedicalRecordResponse,
  ClinicalNoteResponse,
} from '../api/types';
import { platformFeatures } from '../config/features';
import { ScrollTableRegion } from './ScrollTableRegion';

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('fr-FR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function ClinicalNotesList({ notes }: { notes: ClinicalNoteResponse[] }) {
  if (notes.length === 0) {
    return <p className="clinical-empty">Aucune note au niveau du dossier.</p>;
  }
  return (
    <div className="clinical-entry-list">
      {notes.map((n) => (
        <article key={n.id} className="clinical-entry">
          <div className="clinical-entry__meta">
            <span>{formatDateTime(n.authoredAt)}</span>
            <span>{n.authorUsername}</span>
          </div>
          <p className="clinical-entry__body">{n.narrative}</p>
        </article>
      ))}
    </div>
  );
}

export type ClinicalDossierGlobalPanelProps = {
  record: ClinicalMedicalRecordResponse;
  pending: boolean;
  noteText: string;
  onNoteTextChange: (value: string) => void;
  onAddNote: (e: React.FormEvent) => void;
  uploadTitle: string;
  uploadFile: File | null;
  onUploadTitleChange: (value: string) => void;
  onUploadFileChange: (file: File | null) => void;
  onUploadDocument: (e: React.FormEvent) => void;
  onDownloadDocument: (doc: ClinicalDocumentResponse) => void;
};

export function ClinicalDossierGlobalPanel(props: ClinicalDossierGlobalPanelProps) {
  const { record, pending } = props;
  const docCount = record.documents?.length ?? 0;

  return (
    <>
      <p className="clinical-global-hint">
        Éléments enregistrés au niveau du <strong>dossier patient</strong> (tout le parcours), distincts du
        formulaire de chaque hospitalisation. Les observations, diagnostics et{' '}
        <strong>prescriptions</strong> se saisissent dans les <strong>consultations</strong> ; la{' '}
        <strong>conclusion</strong> relève de chaque séjour.
      </p>
      <div className="clinical-layout">
        <aside className="clinical-layout__actions">
          <div className="card">
            <h3 style={{ marginTop: 0, fontSize: '1rem' }}>Nouvelle note</h3>
            <form onSubmit={props.onAddNote}>
              <div className="field">
                <label htmlFor="clinical-note">Narratif</label>
                <textarea
                  id="clinical-note"
                  rows={4}
                  value={props.noteText}
                  onChange={(e) => props.onNoteTextChange(e.target.value)}
                  placeholder="Observation générale…"
                />
              </div>
              <button type="submit" className="btn btn-primary" disabled={pending}>
                {pending ? 'Enregistrement…' : 'Ajouter'}
              </button>
            </form>
          </div>

          {platformFeatures.clinicalDocumentsUpload && (
            <div className="card">
              <h3 style={{ marginTop: 0, fontSize: '1rem' }}>Document</h3>
              <form onSubmit={props.onUploadDocument}>
                <div className="field">
                  <label htmlFor="doc-title">Titre</label>
                  <input
                    id="doc-title"
                    value={props.uploadTitle}
                    onChange={(e) => props.onUploadTitleChange(e.target.value)}
                  />
                </div>
                <div className="field">
                  <label htmlFor="doc-file">Fichier</label>
                  <input
                    id="doc-file"
                    type="file"
                    onChange={(e) => props.onUploadFileChange(e.target.files?.[0] ?? null)}
                  />
                </div>
                <button type="submit" className="btn btn-secondary" disabled={pending}>
                  Déposer
                </button>
              </form>
            </div>
          )}
        </aside>

        <div className="clinical-layout__history">
          <section className="card clinical-section">
            <div className="clinical-section__head">
              <h2 className="clinical-section__title">Notes</h2>
              <span className="clinical-section__count">{record.notes.length}</span>
            </div>
            <ClinicalNotesList notes={record.notes} />
          </section>

          {platformFeatures.clinicalDocumentsUpload && (
            <section className="card clinical-section table-wrap clinical-compact-table">
              <div className="clinical-section__head">
                <h2 className="clinical-section__title">Documents</h2>
                <span className="clinical-section__count">{docCount}</span>
              </div>
              {docCount === 0 ? (
                <p className="clinical-empty">Aucun document.</p>
              ) : (
                <ScrollTableRegion>
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Date</th>
                        <th>Titre</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {(record.documents ?? []).map((doc) => (
                        <tr key={doc.id}>
                          <td>{formatDateTime(doc.uploadedAt)}</td>
                          <td>{doc.title}</td>
                          <td>
                            <button
                              type="button"
                              className="btn btn-secondary btn-sm"
                              onClick={() => props.onDownloadDocument(doc)}
                            >
                              Télécharger
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </ScrollTableRegion>
              )}
            </section>
          )}
        </div>
      </div>
    </>
  );
}
