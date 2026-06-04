# Modèle du domaine Afya — diagramme de classes avec attributs et méthodes

Attributs et **méthodes publiques** alignés sur les entités JPA (`**/model/*.java`).  
Export complet (recommandé) : [plantuml/MODELE_DOMAINE_AFYA.puml](plantuml/MODELE_DOMAINE_AFYA.puml).

## identity-service

```mermaid
classDiagram
  class AppUser {
    <<AggregateRoot>>
    -Long id
    -String username
    -String email
    -String fullName
    -String passwordHash
    -boolean active
    -Instant createdAt
    -Set~Long~ hospitalServiceIds
  }
  class Role {
    <<Entity>>
    -Long id
    -String code
    -String label
  }
  class RefreshToken {
    <<Entity>>
    -Long id
    -Long userId
    -String tokenHash
    -Instant expiresAt
    -boolean revoked
    -Instant createdAt
  }
  class RevokedAccessJti {
    <<Entity>>
    -String jti
    -Instant expiresAt
    -Instant revokedAt
  }
  AppUser "1" --> "*" RefreshToken
  AppUser "*" --> "*" Role
```

## catalog-service

```mermaid
classDiagram
  class Department {
    <<AggregateRoot>>
    -Long id
    -String code
    -String name
    -boolean active
    -Instant createdAt
  }
  class HospitalService {
    <<AggregateRoot>>
    -Long id
    -Long departmentId
    -String name
    -int bedCapacity
    -int bedsPerRoom
    -char roomLetterPrefix
    -BedAssignmentPolicy bedAssignmentPolicy
    -boolean active
    -Instant createdAt
  }
  class Bed {
    <<Entity>>
    -Long id
    -Long hospitalServiceId
    -String label
    -boolean occupied
    -Instant lastFreedAt
  }
  Department "1" *-- "1..*" HospitalService
  HospitalService "1" *-- "0..*" Bed
```

## patient-service

```mermaid
classDiagram
  class Patient {
    <<AggregateRoot>>
    -Long id
    -String firstName
    -String lastName
    -String dossierNumber
    -LocalDate birthDate
    -Instant deceasedAt
    +getId() Long
    +getFirstName() String
    +setFirstName(String) void
    +getLastName() String
    +setLastName(String) void
    +getDossierNumber() String
    +getDeceasedAt() Instant
    +setDeceasedAt(Instant) void
  }
  class PatientDossierSequence {
    <<Entity>>
    -int sequenceYear
    -String letterBlock
    -int sequenceNumber
  }
  class Appointment {
    <<Entity>>
    -Long id
    -Long patientId
    -Instant scheduledAt
    -AppointmentStatus status
    -String reason
    -Instant createdAt
  }
  Patient "1" o-- "0..*" Appointment
```

## care-entry-service

```mermaid
classDiagram
  class Admission {
    <<AggregateRoot>>
    -Long id
    -Long patientId
    -Long hospitalServiceId
    -Instant admittedAt
    -Instant dischargedAt
    -AdmissionStatus status
    -String admissionReason
    -String dischargeReason
    -Instant createdAt
  }
  class TransferRequest {
    <<Entity>>
    -Long id
    -Long admissionId
    -Long fromServiceId
    -Long toServiceId
    -Instant requestedAt
  }
  class EmergencyVisit {
    <<AggregateRoot>>
    -Long id
    -Long patientId
    -Instant arrivedAt
    -Instant endedAt
    -EmergencyStatus status
    -String priority
    -String triageLevel
    -String orientation
    -String triageNotes
    -Instant createdAt
  }
  class EmergencyVisitTimelineEvent {
    <<Entity>>
    -Long id
    -Long emergencyVisitId
    -String eventType
    -String details
    -Instant createdAt
  }
  class VitalSignReading {
    <<Entity>>
    -Long id
    -Long admissionId
    -Instant recordedAt
    -VitalSignSlot slot
    -Integer systolicBp
    -Integer diastolicBp
    -Integer pulseBpm
    -BigDecimal temperatureCelsius
    -BigDecimal weightKg
    -Integer diuresisMl
    -String stoolsNote
  }
  Admission "1" *-- "0..*" TransferRequest
  EmergencyVisit "1" *-- "0..*" EmergencyVisitTimelineEvent
  Admission ..> VitalSignReading : admissionId
```

## stay-service

```mermaid
classDiagram
  class Stay {
    <<AggregateRoot>>
    -Long id
    -Long patientId
    -Long admissionId
    -Instant checkInAt
    -Instant checkOutAt
    -String roomLabel
    -String bedLabel
    -StayStatus status
    -Instant createdAt
  }
  class HospitalizationForm {
    <<Entity>>
    -Long stayId
    -String antecedentsText
    -String anamnesisText
    -String physicalExamPulmonaryText
    -String physicalExamCardiacText
    -String physicalExamAbdominalText
    -String physicalExamNeurologicalText
    -String physicalExamMiscText
    -String paraclinicalText
    -String conclusionText
    -Instant updatedAt
  }
  Stay "1" *-- "0..1" HospitalizationForm
```

## clinical-record-service

```mermaid
classDiagram
  class MedicalRecord {
    <<AggregateRoot>>
    -Long id
    -Long patientId
    -Instant openedAt
    -String allergies
    -String antecedents
  }
  class Consultation {
    <<AggregateRoot>>
    -Long id
    -Long patientId
    -Long admissionId
    -String doctorName
    -String reason
    -Instant consultationDateTime
  }
  class ConsultationEvent {
    <<Entity>>
    -Long id
    -Long consultationId
    -Long patientId
    -ConsultationEventType eventType
    -String content
    -String diseaseType
    -String diseaseName
    -Instant createdAt
  }
  class DiseaseCatalog {
    <<Entity>>
    -Long id
    -String diseaseType
    -String label
    -String labelNormalized
    -int usageCount
    -Instant firstUsedAt
    -Instant lastUsedAt
  }
  class Diagnosis {
    <<Entity>>
    -Long id
    -Long medicalRecordId
    -String code
    -String label
    -Instant recordedAt
    -String authorUsername
  }
  class PrescriptionLine {
    <<Entity>>
    -Long id
    -Long medicalRecordId
    -String drugName
    -String dosage
    -String frequency
    -LocalDate startDate
    -LocalDate endDate
    -PrescriptionStatus status
    -String prescribedBy
    -Instant createdAt
  }
  class MedicationAdministration {
    <<Entity>>
    -Long id
    -Long prescriptionLineId
    -Instant administeredAt
    -String doseGiven
    -String nurseUsername
    -String notes
  }
  class ClinicalNote {
    <<Entity>>
    -Long id
    -Long medicalRecordId
    -Instant authoredAt
    -String authorUsername
    -String narrative
  }
  class NursingCareRecord {
    <<Entity>>
    -Long id
    -Long medicalRecordId
    -String careType
    -Instant performedAt
    -String nurseUsername
    -String description
  }
  class ClinicalDocument {
    <<Entity>>
    -Long id
    -Long medicalRecordId
    -String title
    -String contentType
    -String objectStorageKey
    -Instant uploadedAt
    -String uploadedBy
  }
  MedicalRecord "1" *-- "*" Diagnosis
  MedicalRecord "1" *-- "*" PrescriptionLine
  MedicalRecord "1" *-- "*" ClinicalNote
  MedicalRecord "1" *-- "*" NursingCareRecord
  MedicalRecord "1" *-- "*" ClinicalDocument
  Consultation "1" *-- "*" ConsultationEvent
  PrescriptionLine "1" o-- "*" MedicationAdministration
```

## audit-service

```mermaid
classDiagram
  class AuditEvent {
    <<Entity>>
    -Long id
    -UUID eventId
    -Instant occurredAt
    -String actorUsername
    -String action
    -String resourceType
    -String resourceId
    -String sourceService
    -String metadataJson
    -Instant createdAt
  }
```

## Liens inter-contextes (références logiques)

```mermaid
classDiagram
  class Patient
  class HospitalService
  class Admission
  class Stay
  class MedicalRecord
  class Consultation
  class AppUser

  Patient ..> Admission : patientId
  Patient ..> Stay : patientId
  Patient ..> MedicalRecord : patientId
  Patient ..> Consultation : patientId
  HospitalService ..> Admission : hospitalServiceId
  Admission ..> Stay : admissionId
  Admission ..> Consultation : admissionId
  AppUser ..> HospitalService : hospitalServiceIds
```
