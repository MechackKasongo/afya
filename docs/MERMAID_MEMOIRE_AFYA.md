# Diagrammes Mermaid — mémoire Afya (8 cas d'utilisation)

Complément de [DIAGRAMMES_UML.md](DIAGRAMMES_UML.md) et des fichiers [plantuml/](plantuml/).  
Rendu : GitHub, GitLab, VS Code / Cursor (extension Mermaid), ou [mermaid.live](https://mermaid.live).

## Sommaire

1. [Modèle du domaine](#4-modèle-du-domaine-mermaid)
2. [Classes participantes](#5-classes-participantes-mermaid) (CU 1 à 8)
3. [Diagrammes d'activité](#6-diagrammes-dactivité-mermaid) (CU 1 à 8)
4. [Diagrammes de conception](#7-diagrammes-de-conception-mermaid)

---

## 4. Modèle du domaine (Mermaid)

Vue **conceptuelle** par contexte délimité (équivalent [MODELE_DOMAINE_AFYA.puml](plantuml/MODELE_DOMAINE_AFYA.puml)).

```mermaid
flowchart TB
  subgraph identity["identity-service"]
    AppUser
    Role
    RefreshToken
    AppUser --- Role
    AppUser --- RefreshToken
  end

  subgraph catalog["catalog-service"]
    Department
    HospitalService
    Bed
    Department --> HospitalService
    HospitalService --> Bed
  end

  subgraph patient["patient-service"]
    Patient
  end

  subgraph care["care-entry-service"]
    Admission
    TransferRequest
    EmergencyVisit
    Admission --> TransferRequest
  end

  subgraph stay["stay-service"]
    Stay
    HospitalizationForm
    Stay --> HospitalizationForm
  end

  subgraph clinical["clinical-record-service"]
    MedicalRecord
    Consultation
    ConsultationEvent
    DiseaseCatalog
    PrescriptionLine
    NursingCareRecord
    MedicalRecord --> PrescriptionLine
    MedicalRecord --> NursingCareRecord
    Consultation --> ConsultationEvent
  end

  subgraph audit["audit-service"]
    AuditEvent
  end

  Patient -.->|patientId| Admission
  Patient -.->|patientId| Stay
  Patient -.->|patientId| MedicalRecord
  Patient -.->|patientId| Consultation
  HospitalService -.->|hospitalServiceId| Admission
  Admission -.->|admissionId| Stay
  Admission -.->|admissionId| Consultation
```

MCD détaillé (tables) : [DIAGRAMMES_UML.md §5](DIAGRAMMES_UML.md#5-modèle-du-domaine-mcd--erdiagram).

---

## 5. Classes participantes (Mermaid)

Stéréotypes : **boundary** = UI / API ; **control** = service applicatif ; **entity** = persistance JPA.

### 5.1 CU 1 — S'authentifier

```mermaid
classDiagram
  direction LR
  class Utilisateur {
    <<actor>>
  }
  class LoginPage {
    <<boundary>>
  }
  class AuthBffController {
    <<boundary>>
  }
  class AuthController {
    <<boundary>>
  }
  class AuthService {
    <<control>>
  }
  class JwtService {
    <<control>>
  }
  class AppUser {
    <<entity>>
  }
  class RefreshToken {
    <<entity>>
  }

  Utilisateur --> LoginPage : saisit identifiants
  LoginPage --> AuthBffController : POST /auth/login
  AuthBffController --> AuthController
  AuthController --> AuthService
  AuthService --> AppUser : vérifie compte
  AuthService --> JwtService : émet JWT
  AuthService --> RefreshToken : session
```

### 5.2 CU 2 — Gérer les utilisateurs

```mermaid
classDiagram
  direction LR
  class Administrateur {
    <<actor>>
  }
  class UsersAdminPage {
    <<boundary>>
  }
  class UserBffController {
    <<boundary>>
  }
  class UserController {
    <<boundary>>
  }
  class UserAdminService {
    <<control>>
  }
  class AppUser {
    <<entity>>
  }
  class Role {
    <<entity>>
  }

  Administrateur --> UsersAdminPage
  UsersAdminPage --> UserBffController : CRUD /users
  UserBffController --> UserController
  UserController --> UserAdminService
  UserAdminService --> AppUser
  UserAdminService --> Role
```

### 5.3 CU 3 — Gérer les services hospitaliers

```mermaid
classDiagram
  direction LR
  class Administrateur {
    <<actor>>
  }
  class HospitalServicesPage {
    <<boundary>>
  }
  class HospitalServiceBffController {
    <<boundary>>
  }
  class DepartmentController {
    <<boundary>>
  }
  class HospitalServiceController {
    <<boundary>>
  }
  class DepartmentService {
    <<control>>
  }
  class HospitalServiceCatalogService {
    <<control>>
  }
  class Department {
    <<entity>>
  }
  class HospitalService {
    <<entity>>
  }
  class Bed {
    <<entity>>
  }

  Administrateur --> HospitalServicesPage
  HospitalServicesPage --> HospitalServiceBffController
  HospitalServiceBffController --> DepartmentController
  HospitalServiceBffController --> HospitalServiceController
  DepartmentController --> DepartmentService --> Department
  HospitalServiceController --> HospitalServiceCatalogService
  HospitalServiceCatalogService --> HospitalService
  HospitalServiceCatalogService --> Bed
  HospitalService --> Department
```

### 5.4 CU 4 — Gérer les activités du système

```mermaid
classDiagram
  direction LR
  class Administrateur {
    <<actor>>
  }
  class ReportingPage {
    <<boundary>>
  }
  class AuditBffController {
    <<boundary>>
  }
  class StatsBffController {
    <<boundary>>
  }
  class AuditEventController {
    <<boundary>>
  }
  class AuditEventService {
    <<control>>
  }
  class AuditEvent {
    <<entity>>
  }

  Administrateur --> ReportingPage
  ReportingPage --> AuditBffController : GET /audit/events
  ReportingPage --> StatsBffController : rapports / volumes
  AuditBffController --> AuditEventController
  AuditEventController --> AuditEventService
  AuditEventService --> AuditEvent
```

### 5.5 CU 5 — Enregistrer un patient

```mermaid
classDiagram
  direction LR
  class Receptionniste {
    <<actor>>
  }
  class PatientsPage {
    <<boundary>>
  }
  class PatientBffController {
    <<boundary>>
  }
  class PatientController {
    <<boundary>>
  }
  class PatientRegistryService {
    <<control>>
  }
  class Patient {
    <<entity>>
  }
  class PatientDossierSequence {
    <<entity>>
  }

  Receptionniste --> PatientsPage
  PatientsPage --> PatientBffController : search / create
  PatientBffController --> PatientController
  PatientController --> PatientRegistryService
  PatientRegistryService --> Patient
  PatientRegistryService --> PatientDossierSequence : n° dossier
```

### 5.6 CU 6 — Gérer les admissions

```mermaid
classDiagram
  direction LR
  class Receptionniste {
    <<actor>>
  }
  class AdmissionsPage {
    <<boundary>>
  }
  class AdmissionBffController {
    <<boundary>>
  }
  class AdmissionController {
    <<boundary>>
  }
  class AdmissionService {
    <<control>>
  }
  class PatientServiceClient {
    <<boundary>>
  }
  class CatalogServiceClient {
    <<boundary>>
  }
  class StayServiceClient {
    <<boundary>>
  }
  class Admission {
    <<entity>>
  }

  Receptionniste --> AdmissionsPage
  AdmissionsPage --> AdmissionBffController
  AdmissionBffController --> AdmissionController
  AdmissionController --> AdmissionService
  AdmissionService --> Admission
  AdmissionService ..> PatientServiceClient
  AdmissionService ..> CatalogServiceClient
  AdmissionService ..> StayServiceClient
```

### 5.7 CU 7 — Prise en charge médicale

```mermaid
classDiagram
  direction TB
  class Medecin {
    <<actor>>
  }
  class ConsultationDetailView {
    <<boundary>>
  }
  class ConsultationBffController {
    <<boundary>>
  }
  class DiseaseCatalogBffController {
    <<boundary>>
  }
  class PrescriptionBffController {
    <<boundary>>
  }
  class ConsultationController {
    <<boundary>>
  }
  class ConsultationService {
    <<control>>
  }
  class DiseaseCatalogService {
    <<control>>
  }
  class ClinicalRecordService {
    <<control>>
  }
  class Consultation {
    <<entity>>
  }
  class ConsultationEvent {
    <<entity>>
  }
  class DiseaseCatalog {
    <<entity>>
  }
  class PrescriptionLine {
    <<entity>>
  }

  Medecin --> ConsultationDetailView
  ConsultationDetailView --> ConsultationBffController
  ConsultationDetailView --> DiseaseCatalogBffController
  ConsultationDetailView --> PrescriptionBffController
  ConsultationBffController --> ConsultationController
  ConsultationController --> ConsultationService
  ConsultationService --> Consultation
  ConsultationService --> ConsultationEvent
  ConsultationService --> DiseaseCatalogService
  DiseaseCatalogService --> DiseaseCatalog
  PrescriptionBffController --> ClinicalRecordService
  ClinicalRecordService --> PrescriptionLine
```

### 5.8 CU 8 — Enregistrer les soins

```mermaid
classDiagram
  direction LR
  class Infirmier {
    <<actor>>
  }
  class MedicalRecordDetailPage {
    <<boundary>>
  }
  class PatientClinicalBffController {
    <<boundary>>
  }
  class MedicalRecordController {
    <<boundary>>
  }
  class ClinicalRecordService {
    <<control>>
  }
  class MedicalRecord {
    <<entity>>
  }
  class NursingCareRecord {
    <<entity>>
  }
  class MedicationAdministration {
    <<entity>>
  }

  Infirmier --> MedicalRecordDetailPage
  MedicalRecordDetailPage --> PatientClinicalBffController
  PatientClinicalBffController --> MedicalRecordController
  MedicalRecordController --> ClinicalRecordService
  ClinicalRecordService --> MedicalRecord
  ClinicalRecordService --> NursingCareRecord
  ClinicalRecordService --> MedicationAdministration
```

---

## 6. Diagrammes d'activité (Mermaid)

### 6.1 CU 1 — S'authentifier

```mermaid
flowchart TD
  A([Début]) --> B[Accéder page connexion]
  B --> C[Saisir identifiant et mot de passe]
  C --> D{Compte actif ?}
  D -->|non| E[401 compte inconnu ou désactivé]
  E --> C
  D -->|oui| F{Mot de passe OK ?}
  F -->|non| G[401 + audit LOGIN_FAILED]
  G --> C
  F -->|oui| H[Générer JWT + refresh]
  H --> I[Persister session refresh]
  I --> J[Audit LOGIN_SUCCESS]
  J --> K[Redirection espace de travail]
  K --> L([Fin])
```

### 6.2 CU 2 — Gérer les utilisateurs

```mermaid
flowchart TD
  A([Début]) --> B[Module Utilisateurs]
  B --> C{Action ?}
  C -->|Ajouter| D[Saisir infos + rôles]
  D --> E{Valide ?}
  E -->|oui| F[Créer AppUser]
  E -->|non| G[Erreur validation]
  C -->|Modifier| H[Sélectionner utilisateur]
  H --> I[Modifier + PATCH]
  C -->|Supprimer| J[Confirmation]
  J -->|oui| K[Désactiver / supprimer]
  J -->|non| L[Annuler]
  C -->|Consulter| M[Afficher détail]
  F --> N([Fin])
  I --> N
  K --> N
  M --> N
  G --> B
  L --> B
```

### 6.3 CU 3 — Gérer les services hospitaliers

```mermaid
flowchart TD
  A([Début]) --> B[Organisation / Services]
  B --> C{Action ?}
  C -->|Ajouter| D[Saisir nom, capacité lits]
  D --> E{Valide ?}
  E -->|oui| F[Créer service + lits]
  E -->|non| G[Erreur validation]
  C -->|Modifier| H[MAJ capacité / libellés]
  C -->|Supprimer| I{Encore utilisé ?}
  I -->|oui| J[Refus suppression]
  I -->|non| K[Supprimer service]
  C -->|Consulter lits| L[Grille chambres/lits]
  F --> M([Fin])
  H --> M
  K --> M
  L --> M
  G --> B
  J --> B
```

### 6.4 CU 4 — Gérer les activités du système

```mermaid
flowchart TD
  A([Début]) --> B[Reporting / Journal audit]
  B --> C[GET audit/events + rapports]
  C --> D{Données ?}
  D -->|non| E[Message aucune activité]
  D -->|oui| F[Afficher tableau]
  F --> G[Filtrer date / action / acteur]
  G --> H[Requête filtrée]
  H --> I[Consulter détail événement]
  E --> J([Fin])
  I --> J
```

### 6.5 CU 5 — Enregistrer un patient

```mermaid
flowchart TD
  A([Début]) --> B{Recherche ou création ?}
  B -->|Recherche| C[Saisir nom ou n° dossier]
  C --> D[GET /patients?search=]
  D --> E{Trouvé ?}
  E -->|oui| F[Afficher fiche]
  E -->|non| G[Patient non trouvé]
  B -->|Création| H[Saisir identité]
  H --> I{Valide ?}
  I -->|oui| J[Générer dossierNumber]
  J --> K[Persister Patient]
  K --> L[Succès + n° dossier]
  I -->|non| M[Erreur validation]
  F --> N([Fin])
  L --> N
  G --> N
  M --> A
```

### 6.6 CU 6 — Gérer les admissions

```mermaid
flowchart TD
  A([Début]) --> B[Interface admissions]
  B --> C[Rechercher patient]
  C --> D{Trouvé ?}
  D -->|non| E[Proposer enregistrement]
  D -->|oui| F[Saisir motif, service, date]
  F --> G{Données valides ?}
  G -->|non| H[Erreur validation]
  G -->|oui| I[Valider patient + service]
  I --> J[Enregistrer Admission]
  J --> K[Ouvrir Stay + lit]
  K --> L[Confirmation admission]
  H --> F
  E --> A
  L --> M([Fin])
```

### 6.7 CU 7 — Prise en charge médicale

```mermaid
flowchart TD
  A([Début]) --> B[Ouvrir consultation]
  B --> C[Charger chronologie + dossier]
  C --> D[Consulter antécédents]
  D --> E[Saisir diagnostic type + maladie]
  E --> F{Champs obligatoires ?}
  F -->|non| G[Erreur 400]
  F -->|oui| H[POST diagnostic]
  H --> I[Incrémenter DiseaseCatalog]
  I --> J{usageCount >= 5 ?}
  J -->|oui| K[Maladie au catalogue]
  J -->|non| L[Suite]
  K --> L
  L --> M[Prescription sur fiche consultation]
  M --> N{Décision ?}
  N -->|Sortie| O[POST discharge admission]
  N -->|Poursuite| P[Suivi séjour / formulaire clinique]
  O --> Q([Fin])
  P --> Q
  G --> E
```

### 6.8 CU 8 — Enregistrer les soins

```mermaid
flowchart TD
  A([Début]) --> B[Rechercher patient]
  B --> C{Trouvé ?}
  C -->|non| D[Patient introuvable]
  C -->|oui| E[Ouvrir dossier médical]
  E --> F{Dossier existe ?}
  F -->|non| G[Créer ou erreur]
  F -->|oui| H[Consulter soins / prescriptions]
  H --> I{Nouveau soin ?}
  I -->|Soin infirmier| J[Saisir type + description]
  I -->|Médicament| K[Marquer administration]
  J --> L{Valide ?}
  L -->|oui| M[Enregistrer NursingCareRecord]
  L -->|non| N[Erreur validation]
  K --> O[Enregistrer MedicationAdministration]
  M --> P([Fin])
  O --> P
  N --> J
  D --> A
```

---

## 7. Diagrammes de conception (Mermaid)

### 7.1 Architecture en couches

Voir [DIAGRAMMES_UML.md §7.1](DIAGRAMMES_UML.md#71-vue-densemble--architecture-en-couches-plateforme).

### 7.2 Séquence — authentification

Voir [DIAGRAMMES_UML.md §7.3](DIAGRAMMES_UML.md#73-séquence--authentification-login).

### 7.3 Séquence — admission

Voir [DIAGRAMMES_UML.md §7.4](DIAGRAMMES_UML.md#74-séquence--enregistrer-une-admission).

### 7.4 Séquence — prise en charge médicale

Voir [DIAGRAMMES_UML.md §7.7](DIAGRAMMES_UML.md#77-séquence--prise-en-charge-médicale-consultation).

### 7.5 Séquence — prescription et administration

Voir [DIAGRAMMES_UML.md §7.5](DIAGRAMMES_UML.md#75-séquence--prescription-et-administration).

### 7.6 Classes — consultation et catalogue

Voir [DIAGRAMMES_UML.md §7.8](DIAGRAMMES_UML.md#78-patron-consultation--clinical-record-service).

### 7.7 États — Admission

```mermaid
stateDiagram-v2
  direction LR
  [*] --> OUVERTE: admit()
  OUVERTE --> TRANSFEREE: transfer()
  TRANSFEREE --> OUVERTE: validation
  OUVERTE --> SORTIE: discharge()
  TRANSFEREE --> SORTIE: discharge()
  OUVERTE --> ANNULEE: annuler()
  SORTIE --> [*]
  ANNULEE --> [*]
```

### 7.8 États — Stay (séjour)

```mermaid
stateDiagram-v2
  direction LR
  [*] --> PLANIFIE: openStay()
  PLANIFIE --> EN_COURS: checkIn()
  EN_COURS --> SUSPENDU: suspend()
  SUSPENDU --> EN_COURS: resume()
  EN_COURS --> CLOTURE: closeStay()
  PLANIFIE --> ANNULE: cancel()
  CLOTURE --> [*]
  ANNULE --> [*]
```

---

## Export PNG

```bash
# Avec @mermaid-js/mermaid-cli (npm)
npx @mermaid-js/mermaid-cli -i docs/MERMAID_MEMOIRE_AFYA.md -o docs/mermaid/out/
```

Ou copier chaque bloc `` ```mermaid `` dans [mermaid.live](https://mermaid.live) → Export PNG/SVG.

PlantUML équivalent : [plantuml/README.md](plantuml/README.md).
