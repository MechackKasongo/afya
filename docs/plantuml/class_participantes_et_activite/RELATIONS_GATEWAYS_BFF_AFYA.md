# Relations UML — gateways / BFF Afya

Inventaire des relations du gateway `afya-bff` (controllers REST vers clients HTTP), basé sur `docs/plantuml/CLASSES_SERVICE_BFF.puml`.

---

## 1. Types de relations utilisés

| Symbole | Type UML | Usage ici |
|---------|----------|-----------|
| `-->` | Association / dépendance d'appel | Un contrôleur BFF appelle un client HTTP |

Le diagramme BFF actuel utilise principalement `-->`.

---

## 2. Relations controllers BFF → clients HTTP

| # | Source (gateway) | Relation | Cible (client) |
|---|------------------|----------|----------------|
| G01 | `AuthBffController` | `-->` | `IdentityClient` |
| G02 | `UserBffController` | `-->` | `IdentityClient` |
| G03 | `PatientBffController` | `-->` | `PatientClient` |
| G04 | `AdmissionBffController` | `-->` | `CareEntryClient` |
| G05 | `EmergencyBffController` | `-->` | `CareEntryClient` |
| G06 | `StayBffController` | `-->` | `StayClient` |
| G07 | `PatientClinicalBffController` | `-->` | `ClinicalRecordClient` |
| G08 | `ConsultationBffController` | `-->` | `ClinicalRecordClient` |
| G09 | `PrescriptionBffController` | `-->` | `ClinicalRecordClient` |
| G10 | `DiseaseCatalogBffController` | `-->` | `ClinicalRecordClient` |
| G11 | `HospitalServiceBffController` | `-->` | `CatalogClient` |
| G12 | `AuditBffController` | `-->` | `AuditClient` |
| G13 | `StatsBffController` | `-->` | `AuditClient` |
| G14 | `StatsBffController` | `-->` | `CareEntryClient` |
| G15 | `StatsBffController` | `-->` | `ClinicalRecordClient` |

---

## 3. Couverture par client HTTP

| Client | Contrôleurs BFF consommateurs |
|--------|-------------------------------|
| `IdentityClient` | `AuthBffController`, `UserBffController` |
| `PatientClient` | `PatientBffController` |
| `CareEntryClient` | `AdmissionBffController`, `EmergencyBffController`, `StatsBffController` |
| `StayClient` | `StayBffController` |
| `ClinicalRecordClient` | `PatientClinicalBffController`, `ConsultationBffController`, `PrescriptionBffController`, `DiseaseCatalogBffController`, `StatsBffController` |
| `CatalogClient` | `HospitalServiceBffController` |
| `AuditClient` | `AuditBffController`, `StatsBffController` |

---

## 4. Mapping client → microservice cible

| Client BFF | Microservice cible |
|------------|--------------------|
| `IdentityClient` | `identity-service` |
| `PatientClient` | `patient-service` |
| `CareEntryClient` | `care-entry-service` |
| `StayClient` | `stay-service` |
| `ClinicalRecordClient` | `clinical-record-service` |
| `CatalogClient` | `catalog-service` |
| `AuditClient` | `audit-service` |

---

## 5. Fichiers liés

- Source diagramme BFF : `docs/plantuml/CLASSES_SERVICE_BFF.puml`
- Vue relation-only : `RELATIONS_GATEWAYS_BFF.puml`
- Inventaire global : `TOUTES_LES_RELATIONS_UML_AFYA.md`

