# Relations UML et cardinalités — Afya

**Document unique** : symboles, tableaux exhaustifs (persistance, classes participantes, BFF) et bonnes pratiques.  
Chaque fichier `.puml` indique aussi **sur le diagramme** le type de relation et les cardinalités (`"1" --> "0..*"`, `: association`, `: composition`, `: dépendance`).  
Diagramme visuel persistance : [DIAGRAMME_PERSISTANCE_AFYA.puml](DIAGRAMME_PERSISTANCE_AFYA.puml).

---

## 1. Symboles UML

| Relation UML | PlantUML | Signification | Exemple Afya |
|--------------|----------|---------------|--------------|
| **Association** | `A --> B` | Utilisation / communication | `InterfaceConnexion --> ControleurAuthentification` |
| **Association (FK)** | `A "1" -- "0..*" B` | Lien structurel avec cardinalité | `Admission "1" -- "0..*" DemandeTransfert` |
| **Dépendance** | `A ..> B` | Référence faible ou inter-service | `Patient ..> Admission` |
| **Composition** | `A "1" *-- "0..*" B` | Tout–partie, cycle de vie lié | `DossierMedical "1" *-- "0..*" Prescription` |
| **Agrégation** | `A o-- B` | Tout–partie faible | Peu utilisée |
| **Réalisation** | `A ..\|> B` | Implémente une interface | `ObjectStorageService` |

| Cardinalité | Signification |
|-------------|---------------|
| `1` | Exactement un |
| `0..1` | Zéro ou un |
| `0..*` | Zéro ou plusieurs |
| `1..*` | Un ou plusieurs |
| `*` | Plusieurs (équivalent `0..*` ici) |

---

## 2. Inventaire complet — persistance (`DIAGRAMME_PERSISTANCE_AFYA.puml`)

### 2.1 Liens intra-contexte (même base / FK ou composition)

| # | Source | Relation | Cible | Cardinalité (source → cible) | Contexte |
|---|--------|----------|-------|-------------------------------|----------|
| P01 | UtilisateurCompte | association | UtilisateurRole | 1 → 0..* | Identité (table N–N) |
| P02 | Role | association | UtilisateurRole | 1 → 0..* | Identité (table N–N) |
| P03 | UtilisateurCompte | association | SessionUtilisateur | 1 → 0..* | Identité |
| P04 | Departement | composition | ServiceHospitalier | 1 → 0..* | Catalogue |
| P05 | ServiceHospitalier | composition | Lit | 1 → 0..* | Catalogue |
| P06 | Admission | association | DemandeTransfert | 1 → 0..* | Parcours |
| P07 | Sejour | composition | FormulaireHospitalisation | 1 → 0..1 | Séjour |
| P08 | DossierMedical | composition | Diagnostic | 1 → 0..* | Clinique |
| P09 | DossierMedical | composition | NoteClinique | 1 → 0..* | Clinique |
| P10 | DossierMedical | composition | Prescription | 1 → 0..* | Clinique |
| P11 | DossierMedical | composition | SoinInfirmier | 1 → 0..* | Clinique |
| P12 | DossierMedical | composition | DocumentClinique | 1 → 0..* | Clinique |
| P13 | Consultation | composition | EvenementConsultation | 1 → 0..* | Clinique |
| P14 | Prescription | association | AdministrationMedicament | 1 → 0..* | Clinique |

### 2.2 Liens inter-contextes (référence logique, microservices)

| # | Source | Relation | Cible | Cardinalité | Libellé / clé |
|---|--------|----------|-------|-------------|---------------|
| P15 | Patient | dépendance | Admission | 1 → 0..* | identifiantPatient |
| P16 | Patient | dépendance | VisiteUrgence | 1 → 0..* | identifiantPatient |
| P17 | Patient | dépendance | DossierMedical | 1 → 0..1 | identifiantPatient (UK) |
| P18 | Patient | dépendance | Consultation | 1 → 0..* | identifiantPatient |
| P19 | Patient | dépendance | Sejour | 1 → 0..* | identifiantPatient |
| P20 | ServiceHospitalier | dépendance | Admission | 1 → 0..* | identifiantService |
| P21 | Admission | dépendance | Sejour | 1 → 0..1 | identifiantAdmission (UK) |
| P22 | Admission | dépendance | Consultation | 1 → 0..* | identifiantAdmission |

---

## 3. Inventaire complet — classes participantes (8 CU)

**Canon analyse** : Acteur `1` → `<<frontière>>` `1` → `<<contrôle>>` contrôleur `1` → `<<contrôle>>` service `1` → `<<entité>>` (cardinalité implicite **1** par flèche de flux).

Fichiers : `CLASSES_PARTICIPANTES_*_FR.puml` (analyse) · `CLASSES_PARTICIPANTES_*.puml` (implémentation).

| CU | # | Source | Relation | Cible | Cardinalité | Phase |
|----|---|--------|----------|-------|-------------|-------|
| 1 Auth | 1 | Utilisateur / User | association | InterfaceConnexion / LoginPage | 1 → 1 | FR / impl |
| 1 Auth | 2 | InterfaceConnexion | association | ControleurAuthentification / AuthBffController | 1 → 1 | FR / impl |
| 1 Auth | 3 | AuthBffController | association | AuthController | 1 → 1 | impl |
| 1 Auth | 4 | ControleurAuthentification | association | ServiceAuthentification / AuthService | 1 → 1 | FR / impl |
| 1 Auth | 5 | ServiceAuthentification | association | UtilisateurCompte / AppUser | 1 → 1 | FR / impl |
| 1 Auth | 6 | ControleurAuthentification | association | SessionUtilisateur / JwtService | 1 → 1 | FR / impl |
| 1 Auth | 7 | AuthService | association | RefreshToken | 1 → 0..* | impl |
| 2 Users | 1 | Administrateur | association | InterfaceGestionUtilisateurs | 1 → 1 | FR |
| 2 Users | 2 | Interface | association | ControleurUtilisateurs / UserBffController | 1 → 1 | FR / impl |
| 2 Users | 3 | UserBffController | association | UserController | 1 → 1 | impl |
| 2 Users | 4 | Contrôleur | association | ServiceGestionUtilisateurs | 1 → 1 | FR |
| 2 Users | 5 | Service | association | UtilisateurCompte / AppUser | 1 → 1 | FR |
| 2 Users | 6 | UtilisateurCompte | association | Role | * → * | FR (N–N) |
| 3 Services | 1 | Administrateur | association | InterfaceServicesHospitaliers | 1 → 1 | FR |
| 3 Services | 2 | Interface | association | ControleurServicesHospitaliers | 1 → 1 | FR |
| 3 Services | 3 | BFF | association | HospitalServiceController | 1 → 1 | impl |
| 3 Services | 4 | Contrôleur | association | ServiceCatalogueHospitalier | 1 → 1 | FR |
| 3 Services | 5 | Service | association | ServiceHospitalier | 1 → 1 | FR |
| 3 Services | 6 | ServiceHospitalier | association | Lit / Department | 1 → 0..* | FR / impl |
| 3 Services | 7 | Service (impl) | association | Bed | 1 → 0..* | impl |
| 4 Activités | 1 | Administrateur | association | InterfaceSupervision | 1 → 1 | FR |
| 4 Activités | 2 | Interface | association | ControleurJournalActivite / AuditBffController | 1 → 1 | FR |
| 4 Activités | 3 | Interface | association | StatsBffController | 1 → 1 | impl |
| 4 Activités | 4 | AuditBffController | association | AuditEventController | 1 → 1 | impl |
| 4 Activités | 5 | Contrôleur | association | ServiceTraçabilite | 1 → 1 | FR |
| 4 Activités | 6 | Service | association | JournalActivite / AuditEvent | 1 → 0..* | FR |
| 5 Patient | 1 | Receptionniste | association | InterfacePatients | 1 → 1 | FR |
| 5 Patient | 2 | Interface | association | ControleurPatients | 1 → 1 | FR |
| 5 Patient | 3 | BFF | association | PatientController | 1 → 1 | impl |
| 5 Patient | 4 | Contrôleur | association | ServicePatients | 1 → 1 | FR |
| 5 Patient | 5 | Service | association | Patient | 1 → 0..* | FR |
| 5 Patient | 6 | Service (impl) | association | PatientDossierSequence | 1 → 1 | impl |
| 6 Admissions | 1 | Receptionniste | association | InterfaceAdmissions | 1 → 1 | FR |
| 6 Admissions | 2 | Interface | association | ControleurAdmissions | 1 → 1 | FR |
| 6 Admissions | 3 | BFF | association | AdmissionController | 1 → 1 | impl |
| 6 Admissions | 4 | Contrôleur | association | ServiceAdmissions | 1 → 1 | FR |
| 6 Admissions | 5 | Service | association | Admission | 1 → 0..* | FR |
| 6 Admissions | 6 | Service (FR) | association | Sejour | 1 → 0..1 | analyse |
| 6 Admissions | 7 | AdmissionService | dépendance | PatientServiceClient | 1 → 1 | impl |
| 6 Admissions | 8 | AdmissionService | dépendance | CatalogServiceClient | 1 → 1 | impl |
| 6 Admissions | 9 | AdmissionService | dépendance | StayServiceClient | 1 → 1 | impl |
| 7 Prise en charge | 1 | Medecin | association | InterfaceDossierMedical | 1 → 1 | FR |
| 7 Prise en charge | 2 | Interface | association | ControleurPriseEnCharge | 1 → 1 | FR |
| 7 Prise en charge | 3 | Interface | association | DiseaseCatalogBffController | 1 → 1 | impl |
| 7 Prise en charge | 4 | Interface | association | PrescriptionBffController | 1 → 1 | impl |
| 7 Prise en charge | 5 | BFF | association | ConsultationController | 1 → 1 | impl |
| 7 Prise en charge | 6 | Contrôleur | association | ServiceClinique | 1 → 1 | FR |
| 7 Prise en charge | 7 | Contrôleur | association | DossierMedical | 1 → 1 | FR |
| 7 Prise en charge | 8 | Contrôleur | association | Consultation | 1 → 0..* | FR |
| 7 Prise en charge | 9 | Consultation | association | EvenementConsultation | 1 → 0..* | FR |
| 7 Prise en charge | 10 | DossierMedical | association | Diagnostic | 1 → 0..* | FR |
| 7 Prise en charge | 11 | DossierMedical | association | Prescription | 1 → 0..* | FR |
| 7 Prise en charge | 12 | ServiceClinique | association | CatalogueMaladie | 1 → 0..* | FR |
| 7 Prise en charge | 13 | DiseaseCatalogService | association | DiseaseCatalog | 1 → 0..* | impl |
| 8 Soins | 1 | Infirmier | association | InterfaceSoins | 1 → 1 | FR |
| 8 Soins | 2 | Interface | association | ControleurSoins | 1 → 1 | FR |
| 8 Soins | 3 | BFF | association | MedicalRecordController | 1 → 1 | impl |
| 8 Soins | 4 | Contrôleur | association | ServiceSoins | 1 → 1 | FR |
| 8 Soins | 5 | Service | association | SoinInfirmier | 1 → 0..* | FR |
| 8 Soins | 6 | Service | association | AdministrationMedicament | 1 → 0..* | FR |
| 8 Soins | 7 | DossierMedical | association | SoinInfirmier | 1 → 0..* | FR |
| 8 Soins | 8 | Prescription | association | AdministrationMedicament | 1 → 0..* | FR |
| 8 Soins | 9 | Service (impl) | association | PrescriptionLine | 1 → 0..* | impl |

### 3.1 Dépendances inter-services (synthèse implémentation)

| Source | Relation | Cible | Cardinalité | Note |
|--------|----------|-------|-------------|------|
| UserAdminApplicationService | dépendance | AuditPublisher | 1 → 1 | audit |
| CatalogAdminApplicationService | dépendance | AuditPublisher | 1 → 1 | audit |
| AdmissionOrchestrationService | dépendance | PatientRestClient | 1 → 1 | HTTP |
| AdmissionOrchestrationService | dépendance | CatalogReadClient | 1 → 1 | HTTP |
| ClinicalApplicationService | dépendance | PatientRestClient | 1 → 1 | HTTP |
| CarePathApplicationService | dépendance | AdmissionOrchestrationService | 1 → 1 | sortie / transfert |
| NursingApplicationService | dépendance | MedicalRecord | 1 → 1 | dossier requis |

---

## 4. Inventaire complet — gateway / BFF

Source diagramme : [CLASSES_SERVICE_BFF.puml](../CLASSES_SERVICE_BFF.puml).

| # | Source (controller BFF) | Relation | Cible (client HTTP) | Cardinalité | Microservice cible |
|---|-------------------------|----------|---------------------|-------------|-------------------|
| G01 | AuthBffController | association | IdentityClient | 1 → 1 | identity-service |
| G02 | UserBffController | association | IdentityClient | 1 → 1 | identity-service |
| G03 | PatientBffController | association | PatientClient | 1 → 1 | patient-service |
| G04 | AdmissionBffController | association | CareEntryClient | 1 → 1 | care-entry-service |
| G05 | EmergencyBffController | association | CareEntryClient | 1 → 1 | care-entry-service |
| G06 | StayBffController | association | StayClient | 1 → 1 | stay-service |
| G07 | PatientClinicalBffController | association | ClinicalRecordClient | 1 → 1 | clinical-record-service |
| G08 | ConsultationBffController | association | ClinicalRecordClient | 1 → 1 | clinical-record-service |
| G09 | PrescriptionBffController | association | ClinicalRecordClient | 1 → 1 | clinical-record-service |
| G10 | DiseaseCatalogBffController | association | ClinicalRecordClient | 1 → 1 | clinical-record-service |
| G11 | HospitalServiceBffController | association | CatalogClient | 1 → 1 | catalog-service |
| G12 | AuditBffController | association | AuditClient | 1 → 1 | audit-service |
| G13 | StatsBffController | association | AuditClient | 1 → 1 | audit-service |
| G14 | StatsBffController | association | CareEntryClient | 1 → 1 | care-entry-service |
| G15 | StatsBffController | association | ClinicalRecordClient | 1 → 1 | clinical-record-service |

**Transversal runtime (non dessiné sur chaque flèche)** : `X-Correlation-Id`, timeout / retry / circuit-breaker sur les clients HTTP (`afya-shared`).

---

## 5. Correspondance diagrammes ↔ fichiers

| Type | Fichier PlantUML | Tableau |
|------|------------------|---------|
| Persistance | `DIAGRAMME_PERSISTANCE_AFYA.puml` | § 2 |
| Classes participantes | `CLASSES_PARTICIPANTES_*_FR.puml` / `CLASSES_PARTICIPANTES_*.puml` | § 3 |
| BFF | `CLASSES_SERVICE_BFF.puml` | § 4 |
| Modèle domaine | `MODELE_DOMAINE_AFYA.puml` | aligné sur § 2 |
| Cas d'utilisation | `CAS_UTILISATION_AFYA.puml` | acteur → CU (`1` → `1`) |

---

## 6. Bonnes pratiques (rapport)

1. **Persistance** : toujours indiquer la cardinalité sur le diagramme (`"1" *-- "0..*"`).
2. **Microservices** : liens inter-bases en **dépendance** `..>` avec libellé d’identifiant.
3. **Classes participantes** : ne pas mélanger avec la persistance sur un même diagramme.
4. **Référence unique** : ce fichier remplace les anciens inventaires relations séparés.

---

## 7. English summary

| French | English | PlantUML |
|--------|---------|----------|
| Association | Association | `-->` / `--` |
| Dépendance | Dependency | `..>` |
| Composition | Composition | `*--` |
| Cardinalité | Multiplicity | `"1"`, `"0..*"`, `"0..1"` |

Export PNG/PDF : [README.md](README.md).
