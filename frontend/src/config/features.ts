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
  admissionPrescriptionsByAdmission: true,
  urgenceTriageTimeline: true,
  /** Upload / téléchargement documents via MinIO ou stockage fichier (medical-service). */
  clinicalDocumentsUpload: true,
  /** Demandes d'examens laboratoire (lab-service via BFF). */
  labModule: true,
  /** Catalogue admin types d'examens (POST /api/v1/lab/exam-types). */
  labExamTypesAdmin: true,
} as const;
