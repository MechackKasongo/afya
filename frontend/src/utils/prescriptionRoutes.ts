export function prescriptionAdministrationsPath(
  admissionId: number,
  prescriptionLineId: number,
): string {
  return `/admissions/${admissionId}/prescriptions/${prescriptionLineId}/administrations`;
}

export function canLinkPrescriptionAdministrations(
  admissionId: number | null | undefined,
): admissionId is number {
  return admissionId != null && Number.isFinite(admissionId);
}
