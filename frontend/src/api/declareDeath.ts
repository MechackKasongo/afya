import { api } from './client';
import type { AdmissionResponse, DeathDeclarationRequest, PageAdmissionResponse } from './types';

/**
 * Déclare le décès : sur le séjour actif s'il existe, sinon sur la fiche patient (ex. passage aux urgences).
 */
export async function declarePatientDeath(
  patientId: number,
  options?: { admissionId?: number | null; note?: string },
): Promise<{ viaAdmission: boolean; admissionId?: number }> {
  const payload: DeathDeclarationRequest = { note: options?.note?.trim() || undefined };

  if (options?.admissionId != null && Number.isFinite(options.admissionId)) {
    await api.put<AdmissionResponse>(`/api/v1/admissions/${options.admissionId}/declare-death`, payload);
    return { viaAdmission: true, admissionId: options.admissionId };
  }

  const { data } = await api.get<PageAdmissionResponse>(
    `/api/v1/admissions?patientId=${patientId}&status=EN_COURS&page=0&size=1`,
  );
  const activeAdmission = data.content[0];
  if (activeAdmission) {
    await api.put<AdmissionResponse>(`/api/v1/admissions/${activeAdmission.id}/declare-death`, payload);
    return { viaAdmission: true, admissionId: activeAdmission.id };
  }

  await api.put(`/api/v1/patients/${patientId}/declare-death`, payload);
  return { viaAdmission: false };
}
