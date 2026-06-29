# Architecture — Afya Platform (9 services)

> **Référence mémoire (9 microservices)** : [MODELE_DOMAINE_MEMOIRE_9_SERVICES.md](MODELE_DOMAINE_MEMOIRE_9_SERVICES.md)  
> **Mapping mémoire → implémentation** : [MAPPING_MODELE_ANALYSE_AFYA.md](MAPPING_MODELE_ANALYSE_AFYA.md)

## 1. Vue d'ensemble

Architecture **microservices** : chaque service est **déployable séparément**, possède **sa propre base PostgreSQL**, expose une **API REST versionnée** (`/api/v1/…`), et communique avec les autres par **HTTP** synchrone (avec circuit breaker) et **événements asynchrones** (audit-service).

```
                    [ Navigateur web responsive (SPA React) ]
                                      │
                                      ▼
                    [ API Gateway (infra/gateway — port 8090) ]
                                      │
                                      ▼
                           [ BFF (afya-bff — port 8080) ]
                                      │
        ┌──────┬───────┬──────┬───────┼───────┬──────┬──────┬──────┐
        ▼      ▼       ▼      ▼       ▼       ▼      ▼      ▼      ▼
      AUTH    USER    HOSP   PAT     ADM     MED    LAB    NUR    RPT
       │       │       │      │       │       │      │      │      │
       └───────┴───────┴──────┴───────┴───────┴──────┴──────┴──────┘
                          événements audit (AUDIT-SERVICE)
```

## 2. Les 9 services métier

### 2.1 auth-service (MD-01)

| Élément | Détail |
|---------|--------|
| **Port** | 8081 |
| **Responsabilité** | Authentification, JWT stateless, refresh tokens, journal connexions, traçabilité tokens émis. |
| **Base** | `afya_auth` |
| **Entités** | `Credential`, `RefreshToken`, `RevokedAccessJti`, `IssuedToken` (MD-01 TokenJWT), `LoginJournalEntry` (MD-01 JournalConnexion) |
| **API** | `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/me`, `GET /api/v1/auth/login-journal` |
| **Acteurs** | Tous (s'authentifier) |

### 2.2 user-service (MD-02)

| Élément | Détail |
|---------|--------|
| **Port** | 8082 |
| **Responsabilité** | Comptes utilisateurs, rôles, affectations datées aux services hospitaliers. |
| **Base** | `afya_user` |
| **Entités** | `AppUser`, `Role`, `UserAssignment` (MD-02 Affectation — dates début/fin) |
| **API** | `GET/POST/PUT/DELETE /api/v1/users`, `GET /api/v1/users/{id}/assignments` |
| **Acteurs** | Admin |

### 2.3 hospital-service (MD-03)

| Élément | Détail |
|---------|--------|
| **Port** | 8083 |
| **Responsabilité** | Référentiel hospitalier : départements, services hospitaliers, lits, historique occupation. |
| **Base** | `afya_hospital` |
| **Entités** | `Department`, `HospitalService`, `Bed`, `BedOccupation` (MD-03 OccupationLit) |
| **API** | `GET/POST/PUT /api/v1/departments`, `GET/POST/PUT /api/v1/hospital-services`, `PATCH /api/v1/hospital-services/{id}/beds/occupancy` |
| **Acteurs** | Admin (CRUD), Réception (lecture) |

### 2.4 patient-service (MD-04)

| Élément | Détail |
|---------|--------|
| **Port** | 8084 |
| **Responsabilité** | Identité patient, antécédents médicaux, contacts d'urgence. |
| **Base** | `afya_patient` |
| **Entités** | `Patient`, `MedicalAntecedent` (MD-04 AntecedentMedical), `EmergencyContact` (MD-04 ContactUrgence) |
| **API** | `GET/POST/PUT /api/v1/patients`, `GET/POST /api/v1/patients/{id}/antecedents`, `GET/POST /api/v1/patients/{id}/emergency-contacts` |
| **Acteurs** | Réception (création), Médecin, Infirmier (lecture) |

### 2.5 admission-service (MD-05)

| Élément | Détail |
|---------|--------|
| **Port** | 8085 |
| **Responsabilité** | Admissions (normale/urgence), passages urgences, transferts, sorties, notifications. |
| **Base** | `afya_admission` |
| **Entités** | `Admission` (+`admissionNumber`, `admissionType`), `EmergencyVisit`, `TransferRequest`, `DischargeRecord`, `AdmissionNotification` |
| **API** | `GET/POST /api/v1/admissions`, `GET/POST /api/v1/urgences`, `POST /api/v1/admissions/{id}/discharge`, `POST /api/v1/admissions/{id}/transfer` |
| **Acteurs** | Réception, Médecin |

### 2.6 medical-service (MD-06)

| Élément | Détail |
|---------|--------|
| **Port** | 8086 |
| **Responsabilité** | Dossier médical, consultations, prescriptions, diagnostics, documents cliniques, catalogue maladies. |
| **Base** | `afya_medical` |
| **Entités** | `MedicalRecord`, `Consultation`, `ConsultationEvent`, `Diagnosis`, `PrescriptionLine`, `ClinicalNote`, `ClinicalDocument`, `DiseaseCatalog` |
| **API** | `GET/POST /api/v1/medical-records`, `GET/POST /api/v1/consultations`, `GET/POST /api/v1/admissions/{id}/prescription-lines` |
| **Acteurs** | Médecin |

### 2.7 lab-service (MD-07) ✅ 100 %

| Élément | Détail |
|---------|--------|
| **Port** | 8087 |
| **Responsabilité** | Examens de laboratoire : types, demandes, prélèvements, résultats, paramètres. |
| **Base** | `afya_lab` |
| **Entités** | `ExamType`, `ExamRequest`, `ExamRequestLine`, `SpecimenCollection`, `ExamResult`, `ResultParameter` |
| **API** | `GET/POST /api/v1/lab/exam-types`, `GET/POST /api/v1/lab/exam-requests`, `POST /api/v1/lab/exam-requests/{id}/specimen`, `POST /api/v1/lab/exam-requests/{id}/results` |
| **Acteurs** | Médecin (demande), **Laborantin** (prélèvement + résultats), Admin (catalogue) |

### 2.8 nursing-service (MD-08)

| Élément | Détail |
|---------|--------|
| **Port** | 8088 |
| **Responsabilité** | Soins infirmiers, constantes vitales, alertes, notifications prescriptions, administrations médicaments. |
| **Base** | `afya_nursing` |
| **Entités** | `NursingCareRecord` (lien `prescriptionLineId`), `VitalSignReading`, `VitalSignAlert`, `PrescriptionNotification`, `MedicationAdministration` |
| **API** | `GET/POST /api/v1/nursing/care-records`, `GET/POST /api/v1/nursing/vital-signs`, `GET /api/v1/nursing/alerts`, `GET/POST /api/v1/admissions/{id}/medication-administrations` |
| **Acteurs** | Infirmier |

### 2.9 report-service + audit-service (MD-09)

| Élément | Détail |
|---------|--------|
| **Ports** | report: 8089, audit: 8090 |
| **Responsabilité** | Rapports PDF/Excel, statistiques pré-agrégées admissions/médicales, journal d'activité. |
| **Bases** | `afya_report`, `afya_audit` |
| **Entités** | `GeneratedReport`, `AdmissionStats` (MD-09 StatistiqueAdmission), `MedicalStats` (MD-09 StatistiqueMedical), `AuditEvent` |
| **API** | `GET /api/v1/reports/activity`, `GET /api/v1/reports/operational-stats`, `GET /api/v1/reports/generated` |
| **Acteurs** | Admin |

## 3. Composants transversaux

| Composant | Rôle |
|-----------|------|
| `afya-bff` (port 8080) | Agrégation API pour le SPA React, KPI dashboard, rapports combinés |
| `infra/gateway` (port 8090) | TLS, rate-limit, routage `/api` vers BFF |
| `frontend/` | SPA React (Vite + TypeScript) |
| `afya-shared` | Corrélation ID, résilience HTTP (circuit breaker + retry), audit publisher, JWT |

## 4. Règles transverses

- **Pas de JOIN cross-service** en SQL : références par identifiants (`patientId`, `admissionId`, …)
- **Sécurité** : JWT émis par `auth-service` ; chaque service valide le token et les rôles via `afya-shared`
- **Audit** : chaque service publie un événement vers `audit-service` (HTTP asynchrone)
- **Résilience** : `RestClients` (afya-shared) — retry + circuit breaker sur tous les appels inter-services
- **Rendez-vous** : **hors périmètre** Afya Platform

## 5. Couverture mémoire (post-améliorations)

| Microservice mémoire | Service Afya | Couverture |
|----------------------|--------------|------------|
| MD-01 Auth | `auth-service` | **~95 %** (LoginJournalEntry ✅, IssuedToken ✅) |
| MD-02 User | `user-service` | **~90 %** (UserAssignment daté ✅) |
| MD-03 Hospital | `hospital-service` | **~85 %** |
| MD-04 Patient | `patient-service` | **~85 %** |
| MD-05 Admission | `admission-service` | **~95 %** (admissionNumber ✅, AdmissionType ✅) |
| MD-06 Medical | `medical-service` | **~85 %** |
| MD-07 Lab | `lab-service` | **100 %** ✅ |
| MD-08 Nursing | `nursing-service` | **~90 %** (prescriptionLineId ✅) |
| MD-09 Report | `report-service` + `audit-service` | **~85 %** (AdmissionStats ✅, MedicalStats ✅) |
| **Total** | **9 services** | **~90 %** |

---

*Document mis à jour avec le dépôt Afya — plateforme HGR Jason Sendwe.*
