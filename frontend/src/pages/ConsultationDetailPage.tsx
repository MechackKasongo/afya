import { useParams, useSearchParams } from 'react-router-dom';
import { ConsultationDetailView } from '../components/ConsultationDetailView';

export function ConsultationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const consultationId = Number(id);
  const fromAdmissionRaw = searchParams.get('fromAdmission');
  const fromAdmissionId =
    fromAdmissionRaw != null && Number.isFinite(Number(fromAdmissionRaw)) ? Number(fromAdmissionRaw) : undefined;

  if (!Number.isFinite(consultationId)) {
    return <div className="error-banner">ID consultation invalide.</div>;
  }

  const backTo =
    fromAdmissionId != null
      ? `/admissions/${fromAdmissionId}`
      : '/consultations';

  return (
    <ConsultationDetailView
      consultationId={consultationId}
      backTo={backTo}
      backLabel={fromAdmissionId != null ? 'Retour au séjour' : 'Consultations'}
      admissionContextId={fromAdmissionId}
    />
  );
}
