import { Link } from 'react-router-dom';
import type { AdmissionClinicalFormResponse } from '../api/types';
import { clinicalFormHasExamContent, displayExamsDetail, displayExamsRequested } from '../utils/clinicalFormExams';

function displayText(text: string | null | undefined, max?: number): string | null {
  if (text == null || !text.trim()) {
    return null;
  }
  const t = text.trim();
  if (max == null || t.length <= max) {
    return t;
  }
  return `${t.slice(0, max)}…`;
}

type ClinicalFormContentSource =
  | AdmissionClinicalFormResponse
  | {
      antecedentsText?: string | null;
      anamnesisText?: string | null;
      conclusionText?: string | null;
      examsRequestedKind?: string;
      examsRequestedOther?: string;
      examsDetailText?: string;
    };

export function clinicalFormHasContent(form: ClinicalFormContentSource | null): boolean {
  if (!form) {
    return false;
  }
  if ('examsDetailText' in form) {
    return Boolean(
      form.antecedentsText?.trim() ||
        form.anamnesisText?.trim() ||
        form.examsDetailText?.trim() ||
        form.examsRequestedKind?.trim() ||
        form.examsRequestedOther?.trim() ||
        form.conclusionText?.trim(),
    );
  }
  return Boolean(
    form.antecedentsText?.trim() ||
      form.anamnesisText?.trim() ||
      clinicalFormHasExamContent(form as AdmissionClinicalFormResponse) ||
      form.conclusionText?.trim(),
  );
}

function ReadBlock({ label, text }: { label: string; text: string | null }) {
  if (!text) {
    return null;
  }
  return (
    <div className="clinical-summary-block">
      <span className="clinical-summary-block__label">{label}</span>
      <p className="clinical-summary-block__text">{text}</p>
    </div>
  );
}

type ClinicalFormReadViewProps = {
  admissionId: number;
  form: AdmissionClinicalFormResponse | null;
  variant?: 'preview' | 'full';
  showActions?: boolean;
  editAllowed?: boolean;
};

export function ClinicalFormReadView({
  admissionId,
  form,
  variant = 'preview',
  showActions = true,
  editAllowed = true,
}: ClinicalFormReadViewProps) {
  if (!form || !clinicalFormHasContent(form)) {
    return (
      <p style={{ color: 'var(--muted)', margin: 0, fontSize: '0.9rem' }}>
        Aucune donnée enregistrée pour ce séjour.{' '}
        {editAllowed ? (
          <Link to={`/admissions/${admissionId}/clinical-form`}>Remplir le formulaire clinique</Link>
        ) : null}
      </p>
    );
  }

  const truncate = variant === 'preview' ? 280 : undefined;
  const sheetPath = `/admissions/${admissionId}/clinical-sheet`;

  if (variant === 'full') {
    return (
      <article className="clinical-form-read">
        {form.updatedAt ? (
          <p className="clinical-form-read__meta">
            Dernière mise à jour : {new Date(form.updatedAt).toLocaleString('fr-FR')}
          </p>
        ) : null}

        <section className="clinical-form-read__section">
          <h3 className="clinical-form-read__section-title">Antécédents et anamnèse</h3>
          <div className="clinical-form-grid">
            <ReadBlock label="Antécédents" text={displayText(form.antecedentsText)} />
            <ReadBlock label="Anamnèse (histoire de la maladie)" text={displayText(form.anamnesisText)} />
          </div>
        </section>

        <section className="clinical-form-read__section">
          <h3 className="clinical-form-read__section-title">Examens</h3>
          <ReadBlock label="Examen à passer" text={displayText(displayExamsRequested(form))} />
          <ReadBlock label="Détail de l'examen" text={displayText(displayExamsDetail(form))} />
        </section>

        <section className="clinical-form-read__section">
          <h3 className="clinical-form-read__section-title">Conclusion</h3>
          <ReadBlock label="Conclusion" text={displayText(form.conclusionText)} />
        </section>

        {showActions ? (
          <div className="clinical-form-read__actions">
            {editAllowed ? (
              <Link className="btn btn-primary btn-sm" to={`/admissions/${admissionId}/clinical-form`}>
                Modifier le formulaire
              </Link>
            ) : null}
          </div>
        ) : null}
      </article>
    );
  }

  const examsRequested = displayText(displayExamsRequested(form), truncate);
  const examsDetail = displayText(displayExamsDetail(form), truncate);

  return (
    <div>
      {form.updatedAt ? (
        <p style={{ margin: '0 0 0.65rem', color: 'var(--muted)', fontSize: '0.85rem' }}>
          Dernière mise à jour : {new Date(form.updatedAt).toLocaleString('fr-FR')}
        </p>
      ) : null}
      <div className="clinical-form-grid">
        <ReadBlock label="Antécédents" text={displayText(form.antecedentsText, truncate)} />
        <ReadBlock label="Anamnèse (histoire de la maladie)" text={displayText(form.anamnesisText, truncate)} />
      </div>
      <ReadBlock label="Examen à passer" text={examsRequested} />
      <ReadBlock label="Détail de l'examen" text={examsDetail} />
      <ReadBlock label="Conclusion" text={displayText(form.conclusionText, truncate)} />
      {showActions ? (
        <p className="clinical-form-read__preview-actions">
          <Link to={sheetPath}>Voir la fiche complète</Link>
          {editAllowed ? (
            <>
              {' · '}
              <Link to={`/admissions/${admissionId}/clinical-form`}>Modifier le formulaire</Link>
            </>
          ) : null}
        </p>
      ) : null}
    </div>
  );
}
