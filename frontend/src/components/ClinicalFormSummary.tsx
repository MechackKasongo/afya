import { ClinicalFormReadView } from './ClinicalFormReadView';
import type { AdmissionClinicalFormResponse } from '../api/types';

type ClinicalFormSummaryProps = {
  admissionId: number;
  form: AdmissionClinicalFormResponse | null;
  loading?: boolean;
  loadError?: boolean;
  editAllowed?: boolean;
};

export function ClinicalFormSummary({
  admissionId,
  form,
  loading,
  loadError,
  editAllowed = true,
}: ClinicalFormSummaryProps) {
  if (loading) {
    return <p style={{ color: 'var(--muted)', margin: 0, fontSize: '0.9rem' }}>Chargement du formulaire…</p>;
  }
  if (loadError) {
    return (
      <p style={{ color: 'var(--muted)', margin: 0, fontSize: '0.9rem' }}>
        Formulaire non disponible (séjour peut-être absent).
      </p>
    );
  }

  return (
    <ClinicalFormReadView
      admissionId={admissionId}
      form={form}
      variant="preview"
      showActions
      editAllowed={editAllowed}
    />
  );
}
