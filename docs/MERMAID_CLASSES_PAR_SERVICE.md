# Diagrammes de classes par service — Afya Platform

Un diagramme **par microservice** (et le BFF), avec **attributs** et **méthodes** principales.  
Équivalent PlantUML : [plantuml/CLASSES_SERVICE_*.puml](plantuml/README.md).

Rendu : [mermaid.live](https://mermaid.live) ou extension Mermaid dans l’IDE.

---

## 1. identity-service (port 8081)

```mermaid
classDiagram
  direction TB

  class AuthController {
    -authService AuthService
    +login(request) TokenResponse
    +refresh(request) TokenResponse
    +logout(auth, request, body) void
    +me(auth) MeResponse
  }

  class UserController {
    -userAdminService UserAdminService
    +list(...) Page~UserResponse~
    +listRoles() List~RoleOptionResponse~
    +create(request) UserResponse
    +update(id, request) UserResponse
    +updateStatus(id, request) UserResponse
    +delete(id) void
    +get(id) UserResponse
    +credentialsForUser(id) UserCredentialsResponse
  }

  class AuthService {
    -appUserRepository AppUserRepository
    -refreshTokenRepository RefreshTokenRepository
    -jwtService JwtService
    -passwordEncoder PasswordEncoder
    +login(username, password) TokenResponse
    +refresh(refreshTokenRaw) TokenResponse
    +logout(...) void
    +me(username) MeResponse
    -issueTokens(user) TokenResponse
  }

  class UserAdminService {
    -appUserRepository AppUserRepository
    -roleRepository RoleRepository
    +getById(id) UserResponse
    +list(...) Page
    +create(request) UserResponse
    +update(id, request) UserResponse
    +updateStatus(id, active) UserResponse
    +delete(id) void
  }

  class JwtService {
    +issueAccessToken(user) String
    +issueRefreshToken(user) String
    +parseAccessToken(raw) Claims
    +parseRefreshToken(raw) Claims
  }

  class CredentialsLogService {
    +credentialsPreview() CredentialsLogPreviewResponse
    +credentialsFile() byte[]
    +deleteCredentialsFile() void
  }

  class AppUserRepository {
    <<interface>>
    +findByUsernameIgnoreCase(u) Optional
    +save(user) AppUser
  }

  class RefreshTokenRepository {
    <<interface>>
    +findByTokenHashAndRevokedFalse(h) Optional
  }

  class RoleRepository {
    <<interface>>
    +findByCodeIn(codes) List~Role~
  }

  class AppUser {
    -id Long
    -username String
    -email String
    -fullName String
    -passwordHash String
    -roles Set~Role~
    -hospitalServiceIds Set~Long~
    -active boolean
    +isActive() boolean
  }

  class Role {
    -id Long
    -code String
    -label String
  }

  class RefreshToken {
    -id Long
    -tokenHash String
    -expiresAt Instant
    -revoked boolean
  }

  class RevokedAccessJti {
    -jti String
    -expiresAt Instant
  }

  AuthController --> AuthService
  UserController --> UserAdminService
  UserController --> CredentialsLogService
  AuthService --> AppUserRepository
  AuthService --> RefreshTokenRepository
  AuthService --> JwtService
  UserAdminService --> AppUserRepository
  UserAdminService --> RoleRepository
  AppUserRepository ..> AppUser
  RoleRepository ..> Role
  RefreshTokenRepository ..> RefreshToken
```

---

## 2. catalog-service (port 8082)

```mermaid
classDiagram
  direction TB

  class DepartmentController {
    -departmentService DepartmentService
    +list() List~DepartmentResponse~
    +create(request) DepartmentResponse
    +update(id, request) DepartmentResponse
  }

  class HospitalServiceController {
    -hospitalServiceCatalogService HospitalServiceCatalogService
    +list(activeOnly) List
    +getById(id) HospitalServiceResponse
    +create(request) HospitalServiceResponse
    +update(id, request) HospitalServiceResponse
    +updateStatus(id, request) void
    +bedSuggestion(id) BedSuggestionResponse
  }

  class BedController {
    -bedService BedService
    +listByService(serviceId) List~BedResponse~
    +generateBeds(serviceId) List~BedResponse~
    +updateOccupancy(id, request) BedResponse
  }

  class StatsController {
    -catalogStatsService CatalogStatsService
    +occupancy() CatalogOccupancyStatsResponse
  }

  class DepartmentService {
    -departmentRepository DepartmentRepository
    +list() List
    +create(request) DepartmentResponse
    +update(id, request) DepartmentResponse
  }

  class HospitalServiceCatalogService {
    -hospitalServiceRepository HospitalServiceRepository
    -bedService BedService
    +create(request) HospitalServiceResponse
    +update(id, request) HospitalServiceResponse
    +delete(id) void
    +findBedSuggestion(serviceId) BedSuggestionResponse
  }

  class BedService {
    -bedRepository BedRepository
    -bedAssignmentSelector BedAssignmentSelector
    +listByService(serviceId) List
    +generateBedsForService(serviceId) List
    +markOccupied(bedId) void
    +markFree(bedId) void
  }

  class BedAssignmentSelector {
    +selectFirstFreeBed(serviceId) Optional~Bed~
  }

  class Department {
    -id Long
    -code String
    -name String
    -active boolean
  }

  class HospitalService {
    -id Long
    -department Department
    -name String
    -bedCapacity int
    -bedsPerRoom int
    -active boolean
  }

  class Bed {
    -id Long
    -hospitalService HospitalService
    -label String
    -occupied boolean
    -lastFreedAt Instant
  }

  DepartmentController --> DepartmentService
  HospitalServiceController --> HospitalServiceCatalogService
  BedController --> BedService
  StatsController --> CatalogStatsService
  DepartmentService --> Department
  HospitalServiceCatalogService --> HospitalService
  BedService --> Bed
  HospitalService --> Department
  Bed --> HospitalService
```

---

## 3. patient-service (port 8083)

```mermaid
classDiagram
  direction TB

  class PatientController {
    -patientRegistryService PatientRegistryService
    +create(request) PatientResponse
    +getById(id) PatientResponse
    +search(query, page, size, sortBy, sortDir) Page
    +update(id, request) PatientResponse
    +updateContacts(id, request) PatientResponse
    +declareDeath(id, request) PatientResponse
  }

  class AppointmentController {
    -appointmentService AppointmentService
    +create(patientId, request) AppointmentResponse
    +listByPatient(patientId) List
    +cancel(id) AppointmentResponse
  }

  class PatientStatsController {
    +volumes() PatientVolumesResponse
  }

  class PatientRegistryService {
    -patientRepository PatientRepository
    -dossierNumberGenerator DossierNumberGenerator
    +create(request) PatientResponse
    +getById(id) PatientResponse
    +search(...) Page
    +update(id, request) PatientResponse
    +declareDeath(id, request) PatientResponse
  }

  class AppointmentService {
    -appointmentRepository AppointmentRepository
    +create(patientId, request) AppointmentResponse
    +cancel(appointmentId) AppointmentResponse
  }

  class DossierNumberGenerator {
    -sequenceRepository PatientDossierSequenceRepository
    +nextDossierNumber() String
  }

  class Patient {
    -id Long
    -firstName String
    -lastName String
    -dossierNumber String
    -birthDate LocalDate
    -sex String
    -phone String
    -email String
    -deceasedAt Instant
  }

  class Appointment {
    -id Long
    -patientId Long
    -scheduledAt Instant
    -status String
    -reason String
  }

  class PatientDossierSequence {
    -year int
    -lastValue long
  }

  PatientController --> PatientRegistryService
  AppointmentController --> AppointmentService
  PatientRegistryService --> DossierNumberGenerator
  PatientRegistryService --> Patient
  AppointmentService --> Appointment
  DossierNumberGenerator --> PatientDossierSequence
```

---

## 4. care-entry-service (port 8084)

```mermaid
classDiagram
  direction TB

  class AdmissionController {
    -admissionService AdmissionService
    +create(request) AdmissionResponse
    +getById(id) AdmissionResponse
    +list(...) Page
    +transfer(id, request) AdmissionResponse
    +discharge(id, request) AdmissionResponse
    +cancel(id) AdmissionResponse
    +declareDeath(id, request) AdmissionResponse
  }

  class EmergencyController {
    -emergencyVisitService EmergencyVisitService
    +create(request) EmergencyResponse
    +list(...) Page
    +triage(id, request) EmergencyResponse
    +orientation(id, request) EmergencyResponse
    +close(id) EmergencyResponse
  }

  class VitalSignController {
    -vitalSignService VitalSignService
    +record(admissionId, request) VitalSignResponse
    +list(admissionId) List
  }

  class CareEntryStatsController {
    +volumes() CareEntryVolumesResponse
  }

  class AdmissionService {
    -admissionRepository AdmissionRepository
    -admissionWriter AdmissionWriter
    -patientServiceClient PatientServiceClient
    -catalogServiceClient CatalogServiceClient
    -stayServiceClient StayServiceClient
    +admit(request, authHeader) AdmissionResponse
    +transfer(id, request, authHeader) AdmissionResponse
    +discharge(id, request, authHeader) AdmissionResponse
    +cancel(id, authHeader) AdmissionResponse
  }

  class AdmissionWriter {
    +saveAdmission(entity) Admission
  }

  class EmergencyVisitService {
    -emergencyVisitRepository EmergencyVisitRepository
    -timelineService EmergencyVisitTimelineService
    +create(request, authHeader) EmergencyResponse
    +applyTriage(id, request) EmergencyResponse
    +applyOrientation(id, request) EmergencyResponse
    +close(id) EmergencyResponse
  }

  class VitalSignService {
    -vitalSignReadingRepository VitalSignReadingRepository
    +record(admissionId, request) VitalSignResponse
    +listByAdmission(admissionId) List
  }

  class PatientServiceClient {
    +getPatient(patientId, authHeader) PatientSummary
  }

  class CatalogServiceClient {
    +getService(serviceId, authHeader) ServiceSummary
    +suggestBed(serviceName, authHeader) BedSuggestion
  }

  class StayServiceClient {
    +openStay(request, authHeader) StaySummary
    +closeStay(stayId, authHeader) void
  }

  class Admission {
    -id Long
    -patientId Long
    -hospitalServiceId Long
    -status AdmissionStatus
    -admittedAt Instant
    -dischargedAt Instant
    -roomLabel String
    -bedLabel String
  }

  class TransferRequest {
    -id Long
    -admissionId Long
    -fromServiceId Long
    -toServiceId Long
  }

  class EmergencyVisit {
    -id Long
    -patientId Long
    -status String
    -priority String
    -triageLevel String
  }

  class VitalSignReading {
    -id Long
    -admissionId Long
    -recordedAt Instant
    -temperatureCelsius BigDecimal
    -pulseBpm Integer
  }

  AdmissionController --> AdmissionService
  EmergencyController --> EmergencyVisitService
  VitalSignController --> VitalSignService
  AdmissionService --> Admission
  AdmissionService ..> PatientServiceClient
  AdmissionService ..> CatalogServiceClient
  AdmissionService ..> StayServiceClient
  AdmissionService --> AdmissionWriter
  EmergencyVisitService --> EmergencyVisit
  VitalSignService --> VitalSignReading
```

---

## 5. stay-service (port 8085)

```mermaid
classDiagram
  direction TB

  class StayController {
    -stayService StayService
    +open(request) StayResponse
    +getById(id) StayResponse
    +getByAdmission(admissionId) StayResponse
    +close(id, request) StayResponse
    +updateHospitalizationForm(admissionId, request) HospitalizationFormResponse
    +getHospitalizationForm(admissionId) HospitalizationFormResponse
  }

  class StayStatsController {
    +volumes() StayVolumesResponse
  }

  class StayService {
    -stayRepository StayRepository
    -hospitalizationFormRepository HospitalizationFormRepository
    -patientServiceClient PatientServiceClient
    -careEntryServiceClient CareEntryServiceClient
    +open(request, authHeader) StayResponse
    +close(stayId, request, authHeader) StayResponse
    +updateForm(admissionId, request) HospitalizationFormResponse
    +getForm(admissionId) HospitalizationFormResponse
  }

  class PatientServiceClient {
    +getPatient(patientId, authHeader) PatientSummary
  }

  class CareEntryServiceClient {
    +getAdmission(admissionId, authHeader) AdmissionSummary
  }

  class Stay {
    -id Long
    -patientId Long
    -admissionId Long
    -status StayStatus
    -roomLabel String
    -bedLabel String
    -checkInAt Instant
    -checkOutAt Instant
  }

  class HospitalizationForm {
    -stay Stay
    -chiefComplaint String
    -historyText String
    -allergies String
    -updatedAt Instant
  }

  StayController --> StayService
  StayService --> Stay
  StayService --> HospitalizationForm
  StayService ..> PatientServiceClient
  StayService ..> CareEntryServiceClient
  Stay "1" *-- "0..1" HospitalizationForm
```

---

## 6. clinical-record-service (port 8086)

```mermaid
classDiagram
  direction TB

  class MedicalRecordController {
    -clinicalRecordService ClinicalRecordService
    +getMedicalRecord(patientId, activeOnly) MedicalRecordResponse
    +updateAllergies(patientId, request) MedicalRecordResponse
    +updateAntecedents(patientId, request) MedicalRecordResponse
    +addNote(patientId, request) ClinicalNoteResponse
    +addDiagnosis(patientId, request) DiagnosisResponse
    +addNursingCare(patientId, request) NursingCareResponse
    +addMedicationAdministration(...) MedicationAdministrationResponse
    +uploadDocument(patientId, file, title) ClinicalDocumentResponse
    +downloadDocument(patientId, docId) Resource
  }

  class ConsultationController {
    -consultationService ConsultationService
    +list(...) Page~ConsultationResponse~
    +getById(id) ConsultationResponse
    +consultationEvents(id) List
    +create(request) ConsultationResponse
    +addObservation(id, request) ConsultationEventResponse
    +addDiagnostic(id, request) ConsultationEventResponse
    +addExamOrder(id, request) ConsultationEventResponse
  }

  class PrescriptionController {
    -clinicalRecordService ClinicalRecordService
    +create(patientId, request) PrescriptionResponse
    +list(patientId, activeOnly) List
  }

  class DiseaseCatalogController {
    -diseaseCatalogService DiseaseCatalogService
    +listSelectable(diseaseType) List~DiseaseCatalogResponse~
  }

  class ClinicalStatsController {
    +volumes() ClinicalVolumesResponse
  }

  class ClinicalRecordService {
    -medicalRecordRepository MedicalRecordRepository
    -prescriptionLineRepository PrescriptionLineRepository
    -nursingCareRecordRepository NursingCareRecordRepository
    -medicationAdministrationRepository MedicationAdministrationRepository
    -objectStorageService ObjectStorageService
    +getMedicalRecord(patientId, authHeader, activeOnly) MedicalRecordResponse
    +addPrescription(...) PrescriptionResponse
    +addNursingCare(...) NursingCareResponse
    +addMedicationAdministration(...) MedicationAdministrationResponse
    +attachDocument(...) ClinicalDocumentResponse
    -findOrCreateRecord(patientId) MedicalRecord
  }

  class ConsultationService {
    -consultationRepository ConsultationRepository
    -consultationEventRepository ConsultationEventRepository
    -diseaseCatalogService DiseaseCatalogService
    -patientServiceClient PatientServiceClient
    -careEntryServiceClient CareEntryServiceClient
    +create(request, username, authHeader) ConsultationResponse
    +addDiagnostic(id, request, username) ConsultationEventResponse
    +consultationEvents(consultationId) List
    +patientTimeline(patientId) List
  }

  class DiseaseCatalogService {
    -diseaseCatalogRepository DiseaseCatalogRepository
    +recordUsage(diseaseType, diseaseName) void
    +listSelectable(diseaseType) List
    +normalizeLabel(label) String
  }

  class ObjectStorageService {
    <<interface>>
    +putObject(key, bytes, contentType) void
    +getObject(key) InputStream
  }

  class FilesystemObjectStorageService {
    +putObject(...) void
    +getObject(key) InputStream
  }

  class MinioObjectStorageService {
    +putObject(...) void
    +getObject(key) InputStream
  }

  class MedicalRecord {
    -id Long
    -patientId Long
    -allergies String
    -antecedents String
  }

  class Consultation {
    -id Long
    -patientId Long
    -admissionId Long
    -doctorName String
    -reason String
  }

  class ConsultationEvent {
    -eventType ConsultationEventType
    -content String
    -diseaseType String
    -diseaseName String
  }

  class DiseaseCatalog {
    -diseaseType String
    -label String
    -usageCount int
    +isSelectable() boolean
  }

  class PrescriptionLine {
    -drugName String
    -dosage String
    -frequency String
    -status String
  }

  class NursingCareRecord {
    -careType String
    -description String
    -performedAt Instant
  }

  class MedicationAdministration {
    -administeredAt Instant
    -doseGiven String
  }

  class Diagnosis {
    -code String
    -label String
  }

  class ClinicalNote {
    -narrative String
    -authorUsername String
  }

  class ClinicalDocument {
    -title String
    -objectStorageKey String
    -contentType String
  }

  MedicalRecordController --> ClinicalRecordService
  ConsultationController --> ConsultationService
  PrescriptionController --> ClinicalRecordService
  DiseaseCatalogController --> DiseaseCatalogService
  ClinicalRecordService --> MedicalRecord
  ClinicalRecordService --> PrescriptionLine
  ClinicalRecordService --> ObjectStorageService
  FilesystemObjectStorageService ..|> ObjectStorageService
  MinioObjectStorageService ..|> ObjectStorageService
  ConsultationService --> Consultation
  ConsultationService --> ConsultationEvent
  ConsultationService --> DiseaseCatalogService
  DiseaseCatalogService --> DiseaseCatalog
  MedicalRecord "1" *-- "*" PrescriptionLine
  MedicalRecord "1" *-- "*" NursingCareRecord
  Consultation "1" *-- "*" ConsultationEvent
```

---

## 7. audit-service (port 8087)

```mermaid
classDiagram
  direction TB

  class AuditEventController {
    -auditEventService AuditEventService
    +list(page, size, actor, action, from, to) Page~AuditEventResponse~
    +ingest(request) AuditEventResponse
  }

  class ActivityReportController {
    -auditEventService AuditEventService
    +activityReport(from, to) ActivityReportResponse
  }

  class AuditEventService {
    -auditEventRepository AuditEventRepository
    +list(...) Page~AuditEventResponse~
    +ingest(request) AuditEventResponse
    +buildActivityReport(from, to) ActivityReportResponse
  }

  class IngestionKeyAuthenticationFilter {
    +doFilterInternal(...) void
  }

  class AuditEventRepository {
    <<interface>>
    +save(event) AuditEvent
    +search(...) Page
  }

  class AuditEvent {
    -id Long
    -eventId UUID
    -occurredAt Instant
    -actorUsername String
    -action String
    -resourceType String
    -resourceId String
    -sourceService String
    -metadataJson String
  }

  AuditEventController --> AuditEventService
  ActivityReportController --> AuditEventService
  AuditEventService --> AuditEventRepository
  AuditEventRepository ..> AuditEvent
```

---

## 8. afya-bff (port 8080)

```mermaid
classDiagram
  direction TB

  class AuthBffController {
    -identityClient IdentityClient
    +login(request) TokenResponse
    +refresh(request) TokenResponse
    +logout(request) void
    +me(authHeader) MeResponse
  }

  class UserBffController {
    -identityClient IdentityClient
    +listUsers(...) Page
    +createUser(request) UserResponse
    +updateUser(id, request) UserResponse
  }

  class PatientBffController {
    -patientClient PatientClient
    +search(...) Page
    +create(request) PatientResponse
    +getById(id) PatientResponse
  }

  class AdmissionBffController {
    -careEntryClient CareEntryClient
    +createAdmission(request) AdmissionResponse
    +transfer(id, request) AdmissionResponse
    +discharge(id, request) AdmissionResponse
  }

  class EmergencyBffController {
    -careEntryClient CareEntryClient
    +listEmergencies(...) Page
    +createEmergency(request) EmergencyResponse
  }

  class StayBffController {
    -stayClient StayClient
    +getStayByAdmission(admissionId) StayResponse
    +updateClinicalForm(admissionId, request) HospitalizationFormResponse
  }

  class PatientClinicalBffController {
    -clinicalRecordClient ClinicalRecordClient
    +getMedicalRecord(patientId) MedicalRecordResponse
    +addNursingCare(...) NursingCareResponse
  }

  class ConsultationBffController {
    -clinicalRecordClient ClinicalRecordClient
    +listConsultations(...) Page
    +consultationEvents(id) List
    +createConsultation(request) ConsultationResponse
  }

  class PrescriptionBffController {
    -clinicalRecordClient ClinicalRecordClient
    +createPrescription(patientId, request) PrescriptionResponse
  }

  class DiseaseCatalogBffController {
    -clinicalRecordClient ClinicalRecordClient
    +listSelectable(diseaseType) List
  }

  class HospitalServiceBffController {
    -catalogClient CatalogClient
    +listServices() List
    +createService(request) HospitalServiceResponse
  }

  class DepartmentBffController {
    -catalogClient CatalogClient
    +listDepartments() List
  }

  class AuditBffController {
    -auditClient AuditClient
    +listAuditEvents(...) Page
  }

  class StatsBffController {
    -auditClient AuditClient
    -careEntryClient CareEntryClient
    -clinicalRecordClient ClinicalRecordClient
    +activityReport() ActivityReportResponse
    +volumes() PlatformReportOverviewResponse
    +occupancy() CatalogOccupancyStatsResponse
  }

  class IdentityClient {
    +login(request) TokenResponse
    +listUsers(...) Page
    +createUser(request) UserResponse
  }

  class PatientClient {
    +search(...) Page
    +getById(id) PatientResponse
    +create(request) PatientResponse
  }

  class CareEntryClient {
    +createAdmission(request) AdmissionResponse
    +listAdmissions(...) Page
    +createEmergency(request) EmergencyResponse
  }

  class StayClient {
    +openStay(request) StayResponse
    +getForm(admissionId) HospitalizationFormResponse
  }

  class ClinicalRecordClient {
    +getMedicalRecord(patientId) MedicalRecordResponse
    +consultationEvents(id) List
    +addDiagnostic(...) ConsultationEventResponse
    +createPrescription(...) PrescriptionResponse
    +listSelectableDiseases(type) List
  }

  class CatalogClient {
    +listHospitalServices() List
    +occupancyStats() CatalogOccupancyStatsResponse
  }

  class AuditClient {
    +listEvents(...) Page
    +activityReport() ActivityReportResponse
  }

  AuthBffController --> IdentityClient
  UserBffController --> IdentityClient
  PatientBffController --> PatientClient
  AdmissionBffController --> CareEntryClient
  EmergencyBffController --> CareEntryClient
  StayBffController --> StayClient
  PatientClinicalBffController --> ClinicalRecordClient
  ConsultationBffController --> ClinicalRecordClient
  PrescriptionBffController --> ClinicalRecordClient
  DiseaseCatalogBffController --> ClinicalRecordClient
  HospitalServiceBffController --> CatalogClient
  DepartmentBffController --> CatalogClient
  AuditBffController --> AuditClient
  StatsBffController --> AuditClient
  StatsBffController --> CareEntryClient
  StatsBffController --> ClinicalRecordClient
```

---

## Export

| Format | Fichiers |
|--------|----------|
| Mermaid | ce document |
| PlantUML | `docs/plantuml/CLASSES_SERVICE_*.puml` |

Index : [DIAGRAMMES_UML.md](DIAGRAMMES_UML.md) · [MERMAID_MEMOIRE_AFYA.md](MERMAID_MEMOIRE_AFYA.md)
