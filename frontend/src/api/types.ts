export interface MeResponse {
  id: number;
  username: string;
  fullName: string;
  roles: string[];
  hospitalServiceIds: number[];
  /** Libellés des services affectés (triés), pour l’en-tête. */
  hospitalServiceNames: string[];
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  /** Profil aligné sur GET /auth/me (affectations hospitalières incluses). */
  me: MeResponse;
}

export interface PatientResponse {
  id: number;
  firstName: string;
  lastName: string;
  dossierNumber: string;
  birthDate: string;
  sex: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  postName: string | null;
  employer: string | null;
  employeeId: string | null;
  profession: string | null;
  spouseName: string | null;
  spouseProfession: string | null;
  bloodGroup: string | null;
  heightCm: number | null;
  /** Set when death was recorded (e.g. via declare-death on an admission). */
  deceasedAt: string | null;
}

export interface PatientCreateRequest {
  firstName: string;
  lastName: string;
  dossierNumber?: string;
  birthDate: string;
  sex: string;
  phone?: string;
  email?: string;
  address?: string;
  postName?: string;
  employer?: string;
  employeeId?: string;
  profession?: string;
  spouseName?: string;
  spouseProfession?: string;
  bloodGroup?: string;
  heightCm?: number;
}

/** PUT /api/v1/patients/{id} ; le n° de dossier reste non modifiable via ce corps. */
export interface PatientUpdateRequest {
  firstName: string;
  lastName: string;
  birthDate: string;
  sex: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  postName: string;
  employer: string;
  employeeId: string;
  profession: string;
  spouseName: string;
  spouseProfession: string;
  bloodGroup?: string | null;
  heightCm?: number | null;
}

export interface MedicalRecordAllergiesUpdateRequest {
  allergies: string | null;
}

export interface MedicalRecordAntecedentsUpdateRequest {
  antecedents: string | null;
}

export type AntecedentType = 'MEDICAL' | 'CHIRURGICAL' | 'FAMILIAL' | 'ALLERGIE';

export interface MedicalAntecedentResponse {
  id: number;
  patientId: number;
  type: AntecedentType;
  description: string;
  eventDate: string | null;
  createdAt: string;
}

export interface MedicalAntecedentCreateRequest {
  type: AntecedentType;
  description: string;
  eventDate?: string | null;
}

export interface PagePatientResponse {
  content: PatientResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export type AdmissionStatus = 'EN_COURS' | 'TRANSFERE' | 'SORTI' | 'DECEDE';

export interface AdmissionResponse {
  id: number;
  patientId: number;
  serviceName: string;
  room: string | null;
  bed: string | null;
  reason: string;
  admissionDateTime: string;
  dischargeDateTime: string | null;
  status: AdmissionStatus;
}

export interface AdmissionCreateRequest {
  patientId: number;
  serviceName: string;
  room?: string;
  bed?: string;
  reason?: string;
}

/** GET /api/v1/admissions/suggestions/bed?serviceName= */
export interface BedSuggestionResponse {
  available: boolean;
  room: string | null;
  bed: string | null;
  occupiedBeds: number;
  bedCapacity: number;
  message: string | null;
}

export type VitalSignSlot = 'MATIN' | 'SOIR' | 'JOURNEE';

export interface VitalSignAlertResponse {
  id: number;
  parameter: string;
  measuredValue: string;
  thresholdLabel: string;
  alertLevel: 'ATTENTION' | 'CRITIQUE';
  alertAt: string;
}

export interface VitalSignResponse {
  id: number;
  patientId?: number;
  admissionId: number;
  nurseUsername?: string;
  recordedAt: string;
  slot: VitalSignSlot | null;
  systolicBp: number | null;
  diastolicBp: number | null;
  pulseBpm: number | null;
  respiratoryRate?: number | null;
  temperatureCelsius: number | null;
  weightKg: number | null;
  spo2?: number | null;
  diuresisMl: number | null;
  stoolsNote: string | null;
  alerts?: VitalSignAlertResponse[];
}

export interface VitalSignCreateRequest {
  recordedAt?: string;
  slot?: VitalSignSlot;
  systolicBp?: number;
  diastolicBp?: number;
  pulseBpm?: number;
  respiratoryRate?: number;
  temperatureCelsius?: number;
  weightKg?: number;
  spo2?: number;
  diuresisMl?: number;
  stoolsNote?: string;
}

export interface PrescriptionLineResponse {
  id: number;
  admissionId: number;
  medicationName: string;
  dosageText: string | null;
  frequencyText: string | null;
  instructionsText: string | null;
  prescriberName: string | null;
  startDate: string;
  endDate: string | null;
  active: boolean;
  createdAt: string;
}

export interface PrescriptionLineCreateRequest {
  medicationName: string;
  dosageText?: string;
  frequencyText?: string;
  instructionsText?: string;
  prescriberName?: string;
  startDate: string;
  endDate?: string;
}

export interface PrescriptionLineUpdateRequest extends PrescriptionLineCreateRequest {
  active: boolean;
}

export interface MedicationAdministrationResponse {
  id: number;
  prescriptionLineId: number;
  administrationDate: string;
  slot: VitalSignSlot;
  administered: boolean;
}

export interface MedicationAdministrationCreateRequest {
  administrationDate: string;
  slot: VitalSignSlot;
  administered: boolean;
}

export type PrescriptionNotificationStatus = 'ENVOYEE' | 'LUE' | 'EXECUTEE';

export interface PrescriptionNotificationResponse {
  id: number;
  prescriptionLineId: number;
  patientId: number;
  drugName: string;
  nurseUsername: string | null;
  medicationAdministrationId: number | null;
  sentAt: string;
  status: PrescriptionNotificationStatus;
  readAt: string | null;
  executedAt: string | null;
}

export interface AdmissionClinicalFormResponse {
  id: number;
  admissionId: number;
  stayId?: number;
  updatedAt?: string | null;
  antecedentsText: string | null;
  anamnesisText: string | null;
  physicalExamPulmonaryText: string | null;
  physicalExamCardiacText: string | null;
  physicalExamAbdominalText: string | null;
  physicalExamNeurologicalText: string | null;
  physicalExamMiscText: string | null;
  paraclinicalText: string | null;
  conclusionText: string | null;
}

export interface AdmissionClinicalFormUpsertRequest {
  antecedentsText?: string | null;
  anamnesisText?: string | null;
  physicalExamPulmonaryText?: string | null;
  physicalExamCardiacText?: string | null;
  physicalExamAbdominalText?: string | null;
  physicalExamNeurologicalText?: string | null;
  physicalExamMiscText?: string | null;
  paraclinicalText?: string | null;
  conclusionText?: string | null;
}

export interface TransferRequest {
  toService: string;
  room?: string;
  bed?: string;
  note?: string;
}

export interface DischargeRequest {
  note?: string;
}

export interface DeathDeclarationRequest {
  note?: string;
}

export type UrgenceStatus = 'EN_ATTENTE_TRIAGE' | 'EN_COURS' | 'ORIENTE' | 'CLOTURE';

export interface UrgenceResponse {
  id: number;
  patientId: number;
  motif: string | null;
  priority: string;
  triageLevel: string | null;
  orientation: string | null;
  status: UrgenceStatus;
  createdAt: string;
  closedAt: string | null;
}

export interface UrgenceCreateRequest {
  patientId: number;
  motif?: string;
  priority: string;
}

export interface TriageRequest {
  triageLevel: string;
  details?: string;
}

export interface OrientRequest {
  orientation: string;
  details?: string;
}

export interface CloseRequest {
  details?: string;
}

export interface UrgenceTimelineEventResponse {
  id: number;
  urgenceId: number;
  type: string;
  details: string | null;
  createdAt: string;
}

export interface ConsultationResponse {
  id: number;
  patientId: number;
  admissionId: number;
  doctorName: string;
  reason: string | null;
  consultationDateTime: string;
}

export interface ConsultationCreateRequest {
  patientId: number;
  admissionId: number;
  doctorName: string;
  reason?: string;
}

export interface ConsultationEventResponse {
  id: number;
  consultationId: number;
  patientId: number;
  type: string;
  content: string;
  diseaseType?: string | null;
  diseaseName?: string | null;
  examRequestId?: number | null;
  createdAt: string;
}

export interface EventCreateRequest {
  content: string;
  diseaseType?: string | null;
  diseaseName?: string | null;
}

export interface ExamOrderCreateRequest {
  doctorId: number;
  examTypeIds: number[];
  urgency?: ExamUrgency;
  content?: string | null;
}

export interface DiseaseCatalogResponse {
  id: number;
  diseaseType: string;
  label: string;
  usageCount: number;
  selectable: boolean;
}

export interface MetricResponse {
  metric: string;
  value: unknown;
  generatedAt: string;
}

export interface ExportResponse {
  reportType: string;
  status: string;
  downloadUrl: string;
}

export interface MedicalRecordResponse {
  id: number;
  patientId: number;
  allergies: string | null;
  antecedents: string | null;
  createdAt: string;
  updatedAt: string;
  /** Mirror of patient.deceasedAt for read-only UI without an extra patient fetch. */
  patientDeceasedAt: string | null;
}

export interface MedicalRecordEntryResponse {
  id: number;
  patientId: number;
  type: string;
  content: string;
  createdAt: string;
}

export interface RecordCreateRequest {
  patientId: number;
}

export interface TextUpdateRequest {
  content: string;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string | null;
  fullName: string;
  roles: string[];
  active: boolean;
  hospitalServiceIds: number[];
  /** Present only once in the JSON response right after account creation. */
  generatedPassword?: string | null;
}

export interface PageUserResponse {
  content: UserResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PasswordPreviewRequest {
  firstName: string;
  lastName: string;
  postName?: string;
  generatedPasswordLength?: number;
  variation?: number;
}

export interface PasswordPreviewResponse {
  password: string;
}

export interface UserCreateRequest {
  username?: string;
  fullName?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  postName?: string;
  password?: string;
  generatedPasswordLength?: number;
  /** Doit correspondre à la dernière proposition affichée (régénérations). */
  passwordVariation?: number;
  role: string;
  hospitalServiceIds?: number[];
}

export interface UserUpdateRequest {
  fullName: string;
  email?: string | null;
  role: string;
  password?: string;
  /** Omettre pour ne pas modifier l'affectation aux services. */
  hospitalServiceIds?: number[];
}

export interface RoleOptionResponse {
  id: number;
  name: string;
  label: string;
}

export interface DepartmentResponse {
  id: number;
  code: string;
  name: string;
  active: boolean;
}

export interface DepartmentRequest {
  /** Optionnel : généré automatiquement à partir du nom si absent. */
  code?: string;
  name: string;
  active?: boolean;
}

export type BedAssignmentPolicy = 'ROOM_ORDER_ASC' | 'LONGEST_IDLE';

export interface HospitalServiceResponse {
  id: number;
  departmentId?: number;
  departmentCode?: string;
  departmentName?: string;
  name: string;
  bedCapacity: number;
  bedsPerRoom?: number;
  roomLetterPrefix?: string;
  roomCount?: number;
  bedCount?: number;
  bedAssignmentPolicy?: BedAssignmentPolicy;
  active: boolean;
}

export interface HospitalServiceRequest {
  departmentId: number;
  name: string;
  bedCapacity: number;
  bedsPerRoom?: number;
  roomLetterPrefix?: string;
  bedAssignmentPolicy?: BedAssignmentPolicy;
}

export interface BedResponse {
  id: number;
  hospitalServiceId: number;
  label: string;
  occupied: boolean;
}

export interface BedOccupationResponse {
  id: number;
  bedId: number;
  bedLabel: string;
  patientId: number;
  admissionId: number;
  startedAt: string;
  endedAt: string | null;
}

export interface PageAdmissionResponse {
  content: AdmissionResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PageConsultationResponse {
  content: ConsultationResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/** Liste urgences : {@code scopeRestricted} si la liste est vide car l'affectation exclut le service « Urgences ». */
export interface PageUrgenceResponse {
  scopeRestricted: boolean;
  content: UrgenceResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PageHospitalServiceResponse {
  content: HospitalServiceResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/** Dossier clinique (microservices — GET /api/v1/patients/{id}/medical-record). */
export interface ClinicalNoteResponse {
  id: number;
  authoredAt: string;
  authorUsername: string;
  narrative: string;
}

export interface ClinicalNoteRequest {
  narrative: string;
}

export interface ClinicalDiagnosisRequest {
  code?: string | null;
  label: string;
}

export interface ClinicalPrescriptionCreateRequest {
  drugName: string;
  prescriptionDetails: string;
}

export interface ClinicalDiagnosisResponse {
  id: number;
  recordedAt: string;
  label: string;
  code: string | null;
}

export interface ClinicalPrescriptionResponse {
  id: number;
  drugName: string;
  dosage: string | null;
  frequency: string | null;
  startDate: string;
  endDate: string | null;
  status: string;
  prescribedBy: string | null;
  createdAt: string;
  administered: boolean;
}

export interface ClinicalDocumentResponse {
  id: number;
  title: string;
  contentType: string;
  objectStorageKey: string;
  uploadedAt: string;
  uploadedBy: string;
}

export interface ClinicalMedicalRecordResponse {
  id: number;
  patientId: number;
  patientName: string;
  dossierNumber: string;
  openedAt: string;
  allergies: string | null;
  antecedents: string | null;
  notes: ClinicalNoteResponse[];
  diagnoses: ClinicalDiagnosisResponse[];
  prescriptions: ClinicalPrescriptionResponse[];
  nursingCare: { id: number; description: string; recordedAt: string }[];
  documents: ClinicalDocumentResponse[];
}

export interface AuditEventResponse {
  id: number;
  eventId: string;
  occurredAt: string;
  actorUsername: string;
  action: string;
  resourceType: string;
  resourceId: string | null;
  sourceService: string;
  metadataJson: string | null;
  createdAt: string;
}

export interface PageAuditEventResponse {
  content: AuditEventResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ActivityCountItem {
  key: string;
  count: number;
}

export interface ActivityReportResponse {
  from: string;
  to: string;
  totalEvents: number;
  byAction: ActivityCountItem[];
  bySourceService: ActivityCountItem[];
  topActors: ActivityCountItem[];
  byDay: ActivityCountItem[];
  degraded: boolean;
  notice: string | null;
}

export interface ServiceOccupancyStats {
  hospitalServiceId: number;
  serviceName: string;
  departmentName: string;
  totalBeds: number;
  occupiedBeds: number;
  availableBeds: number;
  ratePercent: number;
}

export interface OccupancyStatsValue {
  overallRatePercent: number;
  totalBeds: number;
  occupiedBeds: number;
  availableBeds: number;
  byService: ServiceOccupancyStats[];
}

export interface PlatformVolumesValue {
  patients: number;
  admissions: number;
  activeAdmissions: number;
  transferredAdmissions: number;
  dischargedAdmissions: number;
  deceasedAdmissions: number;
  emergencyVisits: number;
  activeEmergencyVisits: number;
  openStays: number;
  consultations: number;
  clinicalDocuments: number;
}

export interface PlatformReportOverviewResponse {
  generatedAt: string;
  occupancy: OccupancyStatsValue | null;
  occupancyAvailable: boolean;
  volumes: PlatformVolumesValue | null;
  volumesAvailable: boolean;
  activity: ActivityReportResponse;
  warnings: string[];
}

export type ExamCategory = 'BIOLOGY' | 'IMAGING' | 'BACTERIOLOGY' | 'OTHER';

export type ExamRequestStatus = 'PENDING' | 'SPECIMEN_COLLECTED' | 'RESULTS_AVAILABLE' | 'POSTPONED';

export type ExamUrgency = 'NORMAL' | 'URGENT';

export interface ExamTypeSummary {
  id: number;
  name: string;
  category: ExamCategory;
}

export interface ExamTypeResponse {
  id: number;
  name: string;
  description: string | null;
  category: ExamCategory;
  parameters: string | null;
  active: boolean;
  createdAt: string;
}

export interface ExamTypeCreateRequest {
  name: string;
  description?: string | null;
  category: ExamCategory;
  parameters?: string | null;
}

export interface ExamRequestResponse {
  id: number;
  patientId: number;
  doctorId: number;
  admissionId: number | null;
  requestedAt: string;
  urgency: ExamUrgency;
  status: ExamRequestStatus;
  comment: string | null;
  postponeReason: string | null;
  examTypes: ExamTypeSummary[];
}

export interface PageExamRequestResponse {
  content: ExamRequestResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ExamRequestCreateRequest {
  patientId: number;
  doctorId: number;
  admissionId?: number | null;
  urgency: ExamUrgency;
  comment?: string | null;
  examTypeIds: number[];
}

export interface SpecimenCollectionRequest {
  labTechnicianId: number;
  sampleType: string;
}

export interface ResultParameterRequest {
  parameterName: string;
  value: string;
  unit?: string | null;
  referenceMin?: string | null;
  referenceMax?: string | null;
  abnormal: boolean;
}

export interface ResultParameterResponse {
  id: number;
  parameterName: string;
  value: string;
  unit: string | null;
  referenceMin: string | null;
  referenceMax: string | null;
  abnormal: boolean;
}

export interface ExamResultRequest {
  labTechnicianId: number;
  annotation?: string | null;
  parameters: ResultParameterRequest[];
}

export interface ExamResultResponse {
  id: number;
  requestId: number;
  patientId: number;
  labTechnicianId: number;
  resultedAt: string;
  annotation: string | null;
  parameters: ResultParameterResponse[];
}

export interface LabStatsResponse {
  from: string;
  to: string;
  examRequests: number;
  pendingRequests: number;
  specimenCollected: number;
  resultsAvailable: number;
  abnormalParameters: number;
  degraded: boolean;
  notice: string | null;
}

export interface NursingStatsResponse {
  from: string;
  to: string;
  vitalSignReadings: number;
  vitalSignAlerts: number;
  prescriptionNotifications: number;
  executedPrescriptions: number;
  degraded: boolean;
  notice: string | null;
}

export interface OperationalStatsResponse {
  activity: ActivityReportResponse;
  lab: LabStatsResponse;
  nursing: NursingStatsResponse;
}
