/** Fonctionnalités non encore exposées par le BFF / microservices. */
export const platformFeatures = {
  consultations: true,
  usersAdmin: true,
  statsDashboard: true,
  admissionBedSuggestion: true,
  admissionDeclareDeath: true,
  admissionVitalSigns: true,
  /** Formulaire hospi : BFF `/api/v1/admissions/{id}/clinical-form` (admission-service / séjour). */
  admissionClinicalForm: true,
  admissionPrescriptionsByAdmission: false,
  urgenceTriageTimeline: true,
  /** Upload / téléchargement documents via MinIO ou stockage fichier (medical-service). */
  clinicalDocumentsUpload: true,
} as const;
