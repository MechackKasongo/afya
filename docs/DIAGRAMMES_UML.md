# Diagrammes UML — plateforme Afya (multi-services)

Les figures ci-dessous sont en **Mermaid** (syntaxe proche UML, rendu dans GitHub, GitLab, VS Code / Cursor avec extension Mermaid, ou [mermaid.live](https://mermaid.live)). Elles correspondent à la cartographie décrite dans [CARTOGRAPHIE_EXIGENCES.md](CARTOGRAPHIE_EXIGENCES.md) et [ARCHITECTURE_SERVICES.md](ARCHITECTURE_SERVICES.md).

> **Mémoire (8 cas d'utilisation)** — version Mermaid complète (domaine, classes participantes, activités, conception) : [MERMAID_MEMOIRE_AFYA.md](MERMAID_MEMOIRE_AFYA.md). Équivalent PlantUML : [plantuml/README.md](plantuml/README.md).

> **Classes par microservice** (8 diagrammes, attributs + méthodes) : [MERMAID_CLASSES_PAR_SERVICE.md](MERMAID_CLASSES_PAR_SERVICE.md) · [plantuml/CLASSES_SERVICE_*.puml](plantuml/README.md#diagrammes-de-classes-par-service-conception).

> **Dictionnaire des données (section II.2.2)** : voir [DICTIONNAIRE_DES_DONNEES.md](./DICTIONNAIRE_DES_DONNEES.md) (Afya Platform + Afya Health System).

---

## 1. Diagramme de cas d’utilisation (vue globale)

Vue **système** : le rectangle représente le **SI hospitalier** (ensemble des services + BFF + front). Les cas d’utilisation sont regroupés par domaine fonctionnel.

```mermaid
flowchart TB
  subgraph acteurs["Acteurs"]
    A[Admin]
    R[Réceptionniste]
    M[Médecin]
    I[Infirmier·ère]
  end

  subgraph sys["Système Afya (multi-services)"]
    direction TB
    subgraph id["Identité & sécurité"]
      UC1((Authentification))
    end
    subgraph adm["Administration SI"]
      UC2((Gérer comptes utilisateurs))
      UC3((Gérer départements et services hospitaliers))
      UC4((Consulter rapports et statistiques))
      UC5((Consulter journal d’activité))
    end
    subgraph pat["Patient"]
      UC6((Enregistrer patient))
      UC7((Rechercher patient))
    end
    subgraph hosp["Hospitalisation & parcours"]
      UC9((Enregistrer admission))
      UC10((Affecter au service))
      UC11((Gérer transferts administratifs))
      UC12((Consulter historique admissions))
      UC13((Décider transfert ou sortie))
    end
    subgraph clin["Dossier & soins"]
      UC14((Consulter dossier médical))
      UC15((Diagnostics et observations))
      UC16((Prescriptions))
      UC17((Soins infirmiers))
      UC18((Marquer exécution))
      UC19((Historique interventions))
    end
  end

  A --> UC1
  R --> UC1
  M --> UC1
  I --> UC1

  A --> UC2
  A --> UC3
  A --> UC4
  A --> UC5

  R --> UC6
  R --> UC7
  R --> UC9
  R --> UC10
  R --> UC11
  R --> UC12

  M --> UC14
  M --> UC15
  M --> UC16
  M --> UC13

  I --> UC7
  I --> UC14
  I --> UC16
  I --> UC17
  I --> UC18
  I --> UC19
```

> **PlantUML (rapport / export PNG-PDF)** : voir l'index complet dans [plantuml/README.md](plantuml/README.md) (cas d'utilisation, composants, déploiement, activités, conception, domaine).

---

## 2. Diagramme de cas d’utilisation (notation `usecaseDiagram`)

Variante proche **UML use case** (Mermaid 10.9+ avec `usecaseDiagram`). Si le rendu échoue dans votre outil, utilisez la **vue globale** (section 1) ou [mermaid.live](https://mermaid.live) avec une version récente.

```mermaid
usecaseDiagram
  actor Admin
  actor "Réceptionniste" as Rec
  actor "Médecin" as Med
  actor "Infirmier" as Inf

  usecase "Authentification" as UC_Auth
  usecase "Gérer utilisateurs" as UC_Users
  usecase "Gérer services hospitaliers" as UC_Cat
  usecase "Rapports et statistiques" as UC_Rep
  usecase "Consulter journal d audit" as UC_AuditView
  usecase "Enregistrer patient" as UC_RegP
  usecase "Rechercher patient" as UC_SearchP
  usecase "Admission et affectation" as UC_Adm
  usecase "Transferts administratifs" as UC_Trf
  usecase "Historique admissions" as UC_HistAdm
  usecase "Transfert ou sortie" as UC_Discharge
  usecase "Dossier médical" as UC_Dmr
  usecase "Diagnostics et observations" as UC_Dx
  usecase "Prescriptions" as UC_Rx
  usecase "Soins infirmiers" as UC_Soin
  usecase "Marquer exécution" as UC_Exec
  usecase "Historique interventions" as UC_HistSoin

  Admin --> UC_Auth
  Rec --> UC_Auth
  Med --> UC_Auth
  Inf --> UC_Auth

  Admin --> UC_Users
  Admin --> UC_Cat
  Admin --> UC_Rep
  Admin --> UC_AuditView

  Rec --> UC_RegP
  Rec --> UC_SearchP
  Rec --> UC_Adm
  Rec --> UC_Trf
  Rec --> UC_HistAdm

  Med --> UC_Dmr
  Med --> UC_Dx
  Med --> UC_Rx
  Med --> UC_Discharge

  Inf --> UC_SearchP
  Inf --> UC_Dmr
  Inf --> UC_Rx
  Inf --> UC_Soin
  Inf --> UC_Exec
  Inf --> UC_HistSoin

  UC_Adm ..> UC_RegP : include
  UC_Dmr ..> UC_SearchP : include
```

Les relations `..> : include` traduisent une **dépendance métier** (patient connu avant admission ; patient identifié avant dossier), pas nécessairement un `<<include>>` UML implémenté tel quel dans le code.

---

## 3. Diagramme de composants (architecture logicielle)

Vue **implantation** : composants déployables et dépendances principales (REST). Le **BFF** orchestre le front ; l’**audit** consomme des **événements** émis par les autres services.

```mermaid
flowchart LR
  subgraph client["Poste client"]
    WEB[Application web\nresponsive]
  end

  subgraph edge["Couche exposition"]
    BFF[API Gateway / BFF]
  end

  subgraph services["Services métier"]
    ID[identity-service]
    CAT[catalog-service]
    PAT[patient-service]
    CE[care-entry-service]
    ST[stay-service]
    CR[clinical-record-service]
    AUD[audit-service]
  end

  subgraph data["Persistance"]
    D_ID[(DB identity)]
    D_CAT[(DB catalog)]
    D_PAT[(DB patient)]
    D_CE[(DB care-entry)]
    D_ST[(DB stay)]
    D_CR[(DB clinical)]
    OBJ[(Stockage objet\nimages / PDF)]
    D_AUD[(DB audit)]
  end

  WEB -->|HTTPS| BFF
  BFF --> ID
  BFF --> CAT
  BFF --> PAT
  BFF --> CE
  BFF --> ST
  BFF --> CR

  CE -.->|IDs patient / service| PAT
  CE -.->|IDs service| CAT
  CE -.->|séjour lié| ST
  CR -.->|patientId| PAT
  CR --> OBJ

  ID --> D_ID
  CAT --> D_CAT
  PAT --> D_PAT
  CE --> D_CE
  ST --> D_ST
  CR --> D_CR
  AUD --> D_AUD

  ID -.->|événements| AUD
  PAT -.->|événements| AUD
  CE -.->|événements| AUD
  ST -.->|événements| AUD
  CR -.->|événements| AUD
  CAT -.->|événements| AUD
```

**Variante 6 services** : fusionner `care-entry-service` et `stay-service` en un seul composant **encounter-stay-service** ; les flèches vers `D_CE` et `D_ST` deviennent une base unique ou deux schémas dans le même déployable.

---

## 4. Diagramme de déploiement (vue simplifiée)

```mermaid
flowchart TB
  subgraph utilisateur["Utilisateur"]
    NAV[Navigateur]
  end

  subgraph runtime["Environnement d’exécution (ex. conteneurs)"]
    NG[Reverse proxy / TLS]
    GW[Conteneur BFF]
    SVC[Conteneurs services métier]
    AUDC[Conteneur audit-service]
  end

  subgraph stockage["Données"]
    DB[(Bases par service)]
    S3[(Object store MinIO / S3)]
    BUS{{Bus événements optionnel}}
  end

  NAV -->|HTTPS| NG
  NG --> GW
  GW --> SVC
  SVC --> DB
  SVC --> S3
  SVC --> BUS
  BUS -.-> AUDC
  AUDC --> DB
```

---

## 5. Modèle du domaine (MCD — `erDiagram`)

Vue **entités–associations** par **contexte délimité** (un microservice = une base PostgreSQL). Les liens **entre contextes** passent par des identifiants (`patientId`, `hospitalServiceId`, `admissionId`) sans clé étrangère JPA inter-bases.

```mermaid
erDiagram
  %% --- identity-service ---
  APP_USER ||--o{ USER_ROLE : possede
  ROLE ||--o{ USER_ROLE : reference
  APP_USER ||--o{ REFRESH_TOKEN : session
  APP_USER {
    bigint id PK
    string username UK
    string email UK
    string full_name
    string password_hash
    boolean active
    timestamp created_at
  }
  ROLE {
    bigint id PK
    string code UK
    string label
  }
  REFRESH_TOKEN {
    bigint id PK
    bigint user_id FK
    string token_hash UK
    timestamp expires_at
    boolean revoked
  }
  REVOKED_ACCESS_JTI {
    string jti PK
    timestamp expires_at
    timestamp revoked_at
  }

  %% --- catalog-service ---
  DEPARTMENT ||--o{ HOSPITAL_SERVICE : contient
  HOSPITAL_SERVICE ||--o{ BED : equipe
  DEPARTMENT {
    bigint id PK
    string code UK
    string name
    boolean active
  }
  HOSPITAL_SERVICE {
    bigint id PK
    bigint department_id FK
    string name UK
    int bed_capacity
    boolean active
  }
  BED {
    bigint id PK
    bigint hospital_service_id FK
    string label
    boolean occupied
  }

  %% --- patient-service ---
  PATIENT ||--o{ APPOINTMENT : planifie
  PATIENT {
    bigint id PK
    string dossier_number UK
    string first_name
    string last_name
    date birth_date
    string sex
  }
  APPOINTMENT {
    bigint id PK
    bigint patient_id FK
    timestamp scheduled_at
    string status
    string reason
  }

  %% --- care-entry-service ---
  ADMISSION ||--o{ TRANSFER_REQUEST : demande
  PATIENT ||--o{ ADMISSION : "patientId (logique)"
  HOSPITAL_SERVICE ||--o{ ADMISSION : "hospitalServiceId (logique)"
  PATIENT ||--o{ EMERGENCY_VISIT : "patientId (logique)"
  ADMISSION {
    bigint id PK
    bigint patient_id
    bigint hospital_service_id
    timestamp admitted_at
    timestamp discharged_at
    string status
  }
  TRANSFER_REQUEST {
    bigint id PK
    bigint admission_id FK
    bigint from_service_id
    bigint to_service_id
    timestamp requested_at
  }
  EMERGENCY_VISIT {
    bigint id PK
    bigint patient_id
    timestamp arrived_at
    timestamp ended_at
    string status
  }

  %% --- stay-service ---
  ADMISSION ||--|| STAY : "admissionId (logique)"
  PATIENT ||--o{ STAY : "patientId (logique)"
  STAY ||--o| HOSPITALIZATION_FORM : formulaire
  STAY {
    bigint id PK
    bigint patient_id
    bigint admission_id UK
    timestamp check_in_at
    timestamp check_out_at
    string room_label
    string bed_label
    string status
  }
  HOSPITALIZATION_FORM {
    bigint stay_id PK_FK
    string chief_complaint
    string history_text
    string allergies
  }

  %% --- clinical-record-service ---
  PATIENT ||--o| MEDICAL_RECORD : "patientId (logique)"
  PATIENT ||--o{ CONSULTATION : "patientId (logique)"
  ADMISSION ||--o{ CONSULTATION : "admissionId (logique)"
  CONSULTATION ||--o{ CONSULTATION_EVENT : evenement
  MEDICAL_RECORD ||--o{ CLINICAL_NOTE : note
  MEDICAL_RECORD ||--o{ DIAGNOSIS : diagnostic dossier
  MEDICAL_RECORD ||--o{ PRESCRIPTION_LINE : prescription
  MEDICAL_RECORD ||--o{ NURSING_CARE_RECORD : soin
  MEDICAL_RECORD ||--o{ CLINICAL_DOCUMENT : document
  PRESCRIPTION_LINE ||--o{ MEDICATION_ADMINISTRATION : administre
  CONSULTATION {
    bigint id PK
    bigint patient_id
    bigint admission_id
    string doctor_name
    string reason
  }
  CONSULTATION_EVENT {
    bigint id PK
    bigint consultation_id FK
    string event_type
    string content
    string disease_type
    string disease_name
    timestamp created_at
  }
  DISEASE_CATALOG {
    bigint id PK
    string disease_type
    string label
    string label_normalized
    int usage_count
  }
  MEDICAL_RECORD {
    bigint id PK
    bigint patient_id UK
    timestamp opened_at
  }
  CLINICAL_NOTE {
    bigint id PK
    bigint medical_record_id FK
    string author_username
    string narrative
  }
  DIAGNOSIS {
    bigint id PK
    bigint medical_record_id FK
    string code
    string label
  }
  PRESCRIPTION_LINE {
    bigint id PK
    bigint medical_record_id FK
    string drug_name
    string status
  }
  MEDICATION_ADMINISTRATION {
    bigint id PK
    bigint prescription_line_id FK
    timestamp administered_at
    string nurse_username
  }
  NURSING_CARE_RECORD {
    bigint id PK
    bigint medical_record_id FK
    string care_type
    string description
  }
  CLINICAL_DOCUMENT {
    bigint id PK
    bigint medical_record_id FK
    string title
    string object_storage_key
  }

  %% --- audit-service ---
  AUDIT_EVENT {
    bigint id PK
    uuid event_id UK
    timestamp occurred_at
    string actor_username
    string action
    string resource_type
    string source_service
  }
```

**Légende métier** : `Admission.status` ∈ {`OUVERTE`, `TRANSFEREE`, `SORTIE`, `ANNULEE`} ; `Stay.status` ∈ {`PLANIFIE`, `EN_COURS`, `SUSPENDU`, `CLOTURE`, `ANNULE`} ; `EmergencyVisit.status` ∈ {`EN_COURS`, `SORTIE`, `ADMIS`}.

Variante PlantUML (export PNG/PDF) : [plantuml/MODELE_DOMAINE_AFYA.puml](plantuml/MODELE_DOMAINE_AFYA.puml).

**Tous les diagrammes Mermaid du mémoire (8 CU)** : [MERMAID_MEMOIRE_AFYA.md](MERMAID_MEMOIRE_AFYA.md) (classes participantes, activités, conception).

---

## 6. Diagrammes d'activité

### 6.0 Mermaid (8 cas d'utilisation)

Voir [MERMAID_MEMOIRE_AFYA.md § Activités](MERMAID_MEMOIRE_AFYA.md#6-diagrammes-dactivité-mermaid).

### 6.1 PlantUML

Alignés sur les **8 cas d'utilisation** du mémoire (§ II.3.2) — fichiers dans [plantuml/](plantuml/) :

| CU | Fichier PlantUML |
|----|------------------|
| 1 — S'authentifier | [ACTIVITE_AUTHENTIFICATION_AFYA.puml](plantuml/ACTIVITE_AUTHENTIFICATION_AFYA.puml) |
| 2 — Gérer les utilisateurs | [ACTIVITE_GERER_UTILISATEURS_AFYA.puml](plantuml/ACTIVITE_GERER_UTILISATEURS_AFYA.puml) |
| 3 — Gérer les services hospitaliers | [ACTIVITE_GERER_SERVICES_HOSP_AFYA.puml](plantuml/ACTIVITE_GERER_SERVICES_HOSP_AFYA.puml) |
| 4 — Gérer les activités du système | [ACTIVITE_GERER_ACTIVITES_AFYA.puml](plantuml/ACTIVITE_GERER_ACTIVITES_AFYA.puml) |
| 5 — Enregistrer un patient | [ACTIVITE_ENREGISTRER_PATIENT_AFYA.puml](plantuml/ACTIVITE_ENREGISTRER_PATIENT_AFYA.puml) |
| 6 — Gérer les admissions | [ACTIVITE_ADMISSION_PATIENT_AFYA.puml](plantuml/ACTIVITE_ADMISSION_PATIENT_AFYA.puml) |
| 7 — Prise en charge médicale | [ACTIVITE_PRISE_EN_CHARGE_MEDICALE_AFYA.puml](plantuml/ACTIVITE_PRISE_EN_CHARGE_MEDICALE_AFYA.puml) |
| 8 — Enregistrer les soins | [ACTIVITE_SOIN_INFIRMIER_AFYA.puml](plantuml/ACTIVITE_SOIN_INFIRMIER_AFYA.puml) |

Compléments : [ACTIVITE_SORTIE_TRANSFERT_AFYA.puml](plantuml/ACTIVITE_SORTIE_TRANSFERT_AFYA.puml) (sortie / transfert, branche du CU 7).

---

## 7. Diagrammes de conception

Niveau **conception** : organisation technique (couches, microservices), **patron par service**, **séquences** d’appels REST et **états** des agrégats.

### 7.1 Vue d’ensemble — architecture en couches (plateforme)

```mermaid
flowchart TB
  subgraph Presentation["Couche présentation"]
    FE[SPA React / TypeScript]
  end

  subgraph Edge["Couche exposition"]
    BFF[API Gateway / BFF\nagrégation + JWT]
  end

  subgraph API["Couche API REST (par service)"]
    CTRL[Controllers\n/api/v1/...]
  end

  subgraph Metier["Couche application / métier"]
    SVC[Services\norchestration locale]
    CLIENT[Clients HTTP\nvers autres services]
  end

  subgraph Persistance["Couche persistance"]
    REPO[Repositories JPA]
    ENT[Entités JPA]
  end

  subgraph Transversal["Transversal (afya-shared)"]
    SEC[JWT / sécurité]
    EXC[Gestion erreurs]
  end

  subgraph Services["Microservices"]
    S1[identity-service]
    S2[catalog-service]
    S3[patient-service]
    S4[care-entry-service]
    S5[stay-service]
    S6[clinical-record-service]
    S7[audit-service]
  end

  subgraph Data["Données"]
    DB[(PostgreSQL\nune base par service)]
    OBJ[(Object store\nclinical-documents)]
    FW[Flyway]
  end

  FE --> BFF
  BFF --> CTRL
  CTRL --> SVC
  SVC --> REPO
  SVC --> CLIENT
  REPO --> ENT
  ENT --> DB
  FW --> DB
  SEC --> CTRL
  EXC --> CTRL
  CTRL --> S1
  CTRL --> S2
  CTRL --> S3
  CTRL --> S4
  CTRL --> S5
  CTRL --> S6
  S6 --> OBJ
  S1 --> S7
  S3 --> S7
  S4 --> S7
  S5 --> S7
  S6 --> S7
```

### 7.2 Patron en couches — exemple `care-entry-service`

```mermaid
classDiagram
  direction TB

  class AdmissionController {
    +create(request) AdmissionResponse
    +findById(id) AdmissionResponse
    +transfer(id, request) AdmissionResponse
  }
  class AdmissionService {
    -admissionRepository
    -patientClient
    -catalogClient
    -stayClient
    +admit(request) Admission
    +transfer(id, request) Admission
    +discharge(id, request) Admission
  }
  class AdmissionRepository {
    <<interface>>
    +save(admission) Admission
    +findById(id) Optional
  }
  class Admission {
    -id Long
    -patientId Long
    -hospitalServiceId Long
    -status AdmissionStatus
  }
  class AdmissionCreateRequest {
    <<record>>
    patientId
    hospitalServiceId
  }
  class AdmissionResponse {
    <<record>>
    id
    status
  }
  class PatientClient {
    <<HTTP>>
    +getById(id)
  }
  class CatalogClient {
    <<HTTP>>
    +getService(id)
  }
  class StayClient {
    <<HTTP>>
    +openStay(command)
  }

  AdmissionController --> AdmissionService : utilise
  AdmissionController ..> AdmissionCreateRequest : @Valid
  AdmissionController ..> AdmissionResponse : retourne
  AdmissionService --> AdmissionRepository
  AdmissionService --> PatientClient
  AdmissionService --> CatalogClient
  AdmissionService --> StayClient
  AdmissionRepository --> Admission : persiste
```

### 7.3 Séquence — authentification (login)

```mermaid
sequenceDiagram
  autonumber
  actor U as Utilisateur
  participant Web as SPA Web
  participant BFF as BFF
  participant Ctrl as AuthController
  participant Svc as AuthService
  participant Repo as AppUserRepository
  participant Jwt as JwtService
  participant Ref as RefreshTokenRepository

  U->>Web: login / mot de passe
  Web->>BFF: POST /api/v1/auth/login
  BFF->>Ctrl: LoginRequest
  Ctrl->>Svc: authenticate()
  Svc->>Repo: findByUsername()
  Repo-->>Svc: AppUser
  alt utilisateur invalide
    Svc-->>Ctrl: 401
    Ctrl-->>Web: erreur
  else OK
    Svc->>Jwt: issueAccessToken()
    Svc->>Jwt: issueRefreshToken()
    Svc->>Ref: save(tokenHash)
    Svc-->>Ctrl: AuthResponse + me
    Ctrl-->>Web: 200 + tokens
    Web-->>U: tableau de bord
  end
```

### 7.4 Séquence — enregistrer une admission

```mermaid
sequenceDiagram
  autonumber
  actor R as Réceptionniste
  participant Web as SPA Web
  participant BFF as BFF
  participant Ctrl as AdmissionController
  participant Svc as AdmissionService
  participant Pat as patient-service
  participant Cat as catalog-service
  participant Repo as AdmissionRepository
  participant Stay as stay-service

  R->>Web: formulaire admission
  Web->>BFF: POST /admissions + JWT
  BFF->>Ctrl: AdmissionCreateRequest
  Ctrl->>Svc: admit()
  Svc->>Pat: GET /patients/{id}
  Pat-->>Svc: 200 Patient
  Svc->>Cat: GET /hospital-services/{id}
  Cat-->>Svc: 200 Service actif
  Svc->>Repo: save(Admission)
  Repo-->>Svc: Admission
  Svc->>Stay: POST /stays
  Stay-->>Svc: 201 Stay
  Svc-->>Ctrl: AdmissionResponse
  Ctrl-->>Web: 201 Created
  Web-->>R: confirmation
```

### 7.5 Séquence — prescription et administration

```mermaid
sequenceDiagram
  autonumber
  actor M as Médecin
  actor I as Infirmier
  participant Web as SPA Web
  participant BFF as BFF
  participant Clin as PrescriptionController
  participant Nur as MedicationAdminController
  participant ClinSvc as ClinicalRecordService
  participant NurSvc as NursingService
  participant Repo as MedicalRecordRepository

  Note over M,Repo: Prescription
  M->>Web: saisie prescription
  Web->>BFF: POST /prescription-lines
  BFF->>Clin: PrescriptionLineCreateRequest
  Clin->>ClinSvc: addPrescription()
  ClinSvc->>Repo: findOrCreateMedicalRecord()
  ClinSvc->>Repo: save(PrescriptionLine)
  ClinSvc-->>Web: 201

  Note over I,Repo: Administration
  I->>Web: marquer exécution
  Web->>BFF: POST /medication-administrations
  BFF->>Nur: MedicationAdministrationCreateRequest
  Nur->>NurSvc: administer()
  NurSvc->>Repo: findPrescriptionLine()
  NurSvc->>Repo: save(MedicationAdministration)
  NurSvc-->>Web: 201
```

### 7.6 Diagrammes d’états (agrégats)

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

États **`Admission`** (`care-entry-service`). Pour **`Stay`** : `PLANIFIE` → `EN_COURS` → `CLOTURE` / `ANNULE` — voir [plantuml/ETAT_STAY_AFYA.puml](plantuml/ETAT_STAY_AFYA.puml).

### 7.7 Séquence — prise en charge médicale (consultation)

```mermaid
sequenceDiagram
  autonumber
  actor M as Médecin
  participant Web as SPA Web
  participant BFF as BFF
  participant Cons as ConsultationController
  participant ConsSvc as ConsultationService
  participant CatSvc as DiseaseCatalogService
  participant Rx as PrescriptionController
  participant ClinSvc as ClinicalRecordService
  participant DB as BD clinical

  M->>Web: ouvrir fiche consultation
  Web->>BFF: GET /consultations/{id}/events
  BFF->>Cons: consultationEvents()
  Cons->>ConsSvc: liste chronologie
  ConsSvc->>DB: SELECT events
  DB-->>Web: timeline

  M->>Web: diagnostic type + maladie
  Web->>BFF: GET /disease-catalog?diseaseType=
  BFF->>ConsSvc: listSelectable()
  Web->>BFF: POST /consultations/{id}/diagnostics
  ConsSvc->>DB: INSERT event + catalogue
  DB-->>Web: 201

  M->>Web: prescription
  Web->>BFF: POST /patients/{id}/prescriptions
  BFF->>Rx: addPrescription()
  Rx->>ClinSvc: save PrescriptionLine
  ClinSvc-->>Web: 201
```

Voir aussi [MERMAID_MEMOIRE_AFYA.md § Conception](MERMAID_MEMOIRE_AFYA.md#7-diagrammes-de-conception-mermaid).

### 7.8 Patron consultation — `clinical-record-service`

Diagramme complet (attributs + méthodes) : [MERMAID_MEMOIRE_AFYA.md §7.6](MERMAID_MEMOIRE_AFYA.md#76-classes--consultation-et-catalogue-conception) et [plantuml/CONCEPTION_CONSULTATION_AFYA.puml](plantuml/CONCEPTION_CONSULTATION_AFYA.puml).

```mermaid
classDiagram
  direction TB
  class ConsultationController {
    -consultationService ConsultationService
    +list(...) Page
    +getById(id) ConsultationResponse
    +consultationEvents(id) List
    +create(request) ConsultationResponse
    +addDiagnostic(id,request) ConsultationEventResponse
  }
  class DiseaseCatalogController {
    -diseaseCatalogService DiseaseCatalogService
    +listSelectable(diseaseType) List
  }
  class ConsultationService {
    -consultationRepository ConsultationRepository
    -diseaseCatalogService DiseaseCatalogService
    +addDiagnostic(id,request,username) ConsultationEventResponse
    +create(request,username,authHeader) ConsultationResponse
  }
  class DiseaseCatalogService {
    +recordUsage(type,name) void
    +listSelectable(type) List
  }
  class Consultation {
    -id Long
    -patientId Long
    -admissionId Long
  }
  class ConsultationEvent {
    -diseaseType String
    -diseaseName String
    -content String
  }
  class DiseaseCatalog {
    -usageCount int
    +isSelectable() boolean
  }
  ConsultationController --> ConsultationService
  DiseaseCatalogController --> DiseaseCatalogService
  ConsultationService --> DiseaseCatalogService
  ConsultationService --> Consultation
  ConsultationService --> ConsultationEvent
  DiseaseCatalogService --> DiseaseCatalog
```

### 7.9 Fichiers PlantUML (export PNG/PDF)

| Fichier | Contenu |
|---------|---------|
| [CONCEPTION_COUCHE_SERVICE_AFYA.puml](plantuml/CONCEPTION_COUCHE_SERVICE_AFYA.puml) | Patron couches détaillé (care-entry) |
| [CONCEPTION_SEQUENCE_AUTHENTIFICATION_AFYA.puml](plantuml/CONCEPTION_SEQUENCE_AUTHENTIFICATION_AFYA.puml) | Séquence login |
| [CONCEPTION_SEQUENCE_ADMISSION_AFYA.puml](plantuml/CONCEPTION_SEQUENCE_ADMISSION_AFYA.puml) | Séquence admission |
| [CONCEPTION_SEQUENCE_CLINICAL_AFYA.puml](plantuml/CONCEPTION_SEQUENCE_CLINICAL_AFYA.puml) | Séquence prescription / administration |
| [CONCEPTION_SEQUENCE_PRISE_EN_CHARGE_AFYA.puml](plantuml/CONCEPTION_SEQUENCE_PRISE_EN_CHARGE_AFYA.puml) | Séquence consultation, diagnostic, catalogue, prescription |
| [CONCEPTION_CONSULTATION_AFYA.puml](plantuml/CONCEPTION_CONSULTATION_AFYA.puml) | Couches `ConsultationService` / `DiseaseCatalog` |
| [ETAT_ADMISSION_AFYA.puml](plantuml/ETAT_ADMISSION_AFYA.puml) | Cycle de vie `Admission` |
| [ETAT_STAY_AFYA.puml](plantuml/ETAT_STAY_AFYA.puml) | Cycle de vie `Stay` |

---

## 8. Classes participantes (analyse)

### 8.0 Mermaid (8 cas d'utilisation)

Diagrammes `classDiagram` avec **attributs et méthodes** par classe : [MERMAID_MEMOIRE_AFYA.md §5](MERMAID_MEMOIRE_AFYA.md#5-classes-participantes-mermaid).

### 8.1 PlantUML

Un diagramme **par cas d'utilisation** (notation frontière / contrôle / entité, avec BFF + microservices réels) :

| CU | Fichier PlantUML |
|----|------------------|
| h — S'authentifier | [CLASSES_PARTICIPANTES_AUTH.puml](plantuml/CLASSES_PARTICIPANTES_AUTH.puml) |
| a — Gérer les utilisateurs | [CLASSES_PARTICIPANTES_UTILISATEURS.puml](plantuml/CLASSES_PARTICIPANTES_UTILISATEURS.puml) |
| b — Gérer les services hospitaliers | [CLASSES_PARTICIPANTES_SERVICES_HOSP.puml](plantuml/CLASSES_PARTICIPANTES_SERVICES_HOSP.puml) |
| c — Gérer les activités du système | [CLASSES_PARTICIPANTES_ACTIVITES.puml](plantuml/CLASSES_PARTICIPANTES_ACTIVITES.puml) |
| d — Enregistrer un patient | [CLASSES_PARTICIPANTES_PATIENT.puml](plantuml/CLASSES_PARTICIPANTES_PATIENT.puml) |
| e — Gérer les admissions | [CLASSES_PARTICIPANTES_ADMISSIONS.puml](plantuml/CLASSES_PARTICIPANTES_ADMISSIONS.puml) |
| f — Prise en charge médicale | [CLASSES_PARTICIPANTES_PRISE_EN_CHARGE.puml](plantuml/CLASSES_PARTICIPANTES_PRISE_EN_CHARGE.puml) |
| g — Enregistrer les soins | [CLASSES_PARTICIPANTES_SOINS.puml](plantuml/CLASSES_PARTICIPANTES_SOINS.puml) |

Vue synthèse : [CLASSES_PARTICIPANTES_AFYA.puml](plantuml/CLASSES_PARTICIPANTES_AFYA.puml).

**Modèle du domaine** (conceptuel, contextes délimités) : [MODELE_DOMAINE_AFYA.puml](plantuml/MODELE_DOMAINE_AFYA.puml) — inclut `Consultation`, `ConsultationEvent`, `DiseaseCatalog` (migrations V6/V7).

---

## 9. Correspondance rapide UML ↔ documentation

| Élément UML | Où le trouver |
|-------------|----------------|
| Cas d’utilisation / acteurs | §1–2 de ce fichier + [CARTOGRAPHIE_EXIGENCES.md](CARTOGRAPHIE_EXIGENCES.md) |
| Composants / responsabilités | §3 + [ARCHITECTURE_SERVICES.md](ARCHITECTURE_SERVICES.md) |
| **Modèle du domaine (MCD)** | **§5** (Mermaid) + [plantuml/MODELE_DOMAINE_AFYA.puml](plantuml/MODELE_DOMAINE_AFYA.puml) |
| **Diagrammes de conception** | **§7** (Mermaid) + [plantuml/CONCEPTION_*.puml](plantuml/README.md) |
| Classes participantes (8 CU) | §8 + [plantuml/CLASSES_PARTICIPANTES_*.puml](plantuml/README.md) |
| Dictionnaire des données | [DICTIONNAIRE_DES_DONNEES.md](DICTIONNAIRE_DES_DONNEES.md) |
| Activités (8 CU) | §6 + [MERMAID_MEMOIRE_AFYA.md](MERMAID_MEMOIRE_AFYA.md) + [plantuml/ACTIVITE_*.puml](plantuml/README.md) |
| Séquences mémoire II.3.2 | Figures II.3–II.10 (texte) + §7.3–7.7 Mermaid + [plantuml/CONCEPTION_SEQUENCE_*.puml](plantuml/README.md) |
| **Mémoire complet en Mermaid** | **[MERMAID_MEMOIRE_AFYA.md](MERMAID_MEMOIRE_AFYA.md)** |
| **Classes par service** | **[MERMAID_CLASSES_PAR_SERVICE.md](MERMAID_CLASSES_PAR_SERVICE.md)** |

Pour exporter en **PNG/SVG** : coller les blocs `` ```mermaid `` dans [mermaid.live](https://mermaid.live) ou utiliser `mmdc` (CLI Mermaid) dans votre pipeline CI.
