# Relations — toutes les classes participantes Afya

Inventaire **uniquement** des diagrammes de **classes participantes** (8 cas d’utilisation), phase **analyse (FR)** et **implémentation (Java/BFF)**.

| Vue | Fichier relations (PlantUML) | Fichier classes complètes |
|-----|------------------------------|---------------------------|
| Analyse FR | [RELATIONS_CLASSES_PARTICIPANTES_FR.puml](RELATIONS_CLASSES_PARTICIPANTES_FR.puml) | `CLASSES_PARTICIPANTES_*_FR.puml` |
| Implémentation | [RELATIONS_CLASSES_PARTICIPANTES_IMPL.puml](RELATIONS_CLASSES_PARTICIPANTES_IMPL.puml) | `CLASSES_PARTICIPANTES_*.puml` (sans `_FR`) |
| Regroupé analyse | — | [CLASSES_PARTICIPANTES_ANALYSE_FR.puml](CLASSES_PARTICIPANTES_ANALYSE_FR.puml) |
| Synthèse impl. | — | [CLASSES_PARTICIPANTES_AFYA.puml](CLASSES_PARTICIPANTES_AFYA.puml) |

Persistance et cas d’utilisation : [TOUTES_LES_RELATIONS_UML_AFYA.md](TOUTES_LES_RELATIONS_UML_AFYA.md).

---

## Modèle de relation (analyse)

Chaque CU suit le même **canon** :

```
Acteur --(association)--> <<frontière>>
<<frontière>> --(association)--> <<contrôle>> (contrôleur)
<<contrôle>> (contrôleur) --(association)--> <<contrôle>> (service)
<<contrôle>> (service) --(association)--> <<entité>>
<<entité>> --(association)--> <<entité>>   (liens métier optionnels)
```

| # | Couche | Stéréotype | Rôle |
|---|--------|------------|------|
| 1 | Acteur | — | Utilisateur du système |
| 2 | Interface | `<<frontière>>` | Écran ou point d’entrée |
| 3 | Contrôleur | `<<contrôle>>` | Orchestration requête/réponse |
| 4 | Service | `<<contrôle>>` | Règles métier |
| 5 | Données | `<<entité>>` | Persistance / modèle |

**Symbole unique en analyse :** association `-->` (PlantUML).

---

## CU 1 — S'authentifier

**Fichiers :** `CLASSES_PARTICIPANTES_AUTHENTIFICATION_FR.puml` · `CLASSES_PARTICIPANTES_AUTH.puml`

| # | Source | Relation | Cible | Phase |
|---|--------|----------|-------|-------|
| 1 | Utilisateur / User | association | InterfaceConnexion / LoginPage | FR / impl |
| 2 | InterfaceConnexion / LoginPage | association | ControleurAuthentification / AuthBffController | FR / impl |
| 3 | — / AuthBffController | association | — / AuthController | impl |
| 4 | ControleurAuthentification / AuthController | association | ServiceAuthentification / AuthService | FR / impl |
| 5 | ServiceAuthentification / AuthService | association | UtilisateurCompte / AppUser | FR / impl |
| 6 | ControleurAuthentification / AuthService | association | SessionUtilisateur / JwtService | FR / impl |
| 7 | — / AuthService | association | — / RefreshToken | impl |

---

## CU 2 — Gérer les utilisateurs

**Fichiers :** `CLASSES_PARTICIPANTES_UTILISATEURS_FR.puml` · `CLASSES_PARTICIPANTES_UTILISATEURS.puml`

| # | Source | Relation | Cible |
|---|--------|----------|-------|
| 1 | Administrateur | association | InterfaceGestionUtilisateurs / UsersAdminPage |
| 2 | Interface | association | ControleurUtilisateurs / UserBffController |
| 3 | — | association | UserController (impl) |
| 4 | Contrôleur / UserController | association | ServiceGestionUtilisateurs / UserAdminService |
| 5 | Service | association | UtilisateurCompte / AppUser |
| 6 | UtilisateurCompte / AppUser | association | Role |

---

## CU 3 — Gérer les services hospitaliers

**Fichiers :** `CLASSES_PARTICIPANTES_SERVICES_HOSP_FR.puml` · `CLASSES_PARTICIPANTES_SERVICES_HOSP.puml`

| # | Source | Relation | Cible |
|---|--------|----------|-------|
| 1 | Administrateur | association | InterfaceServicesHospitaliers / HospitalServicesPage |
| 2 | Interface | association | ControleurServicesHospitaliers / HospitalServiceBffController |
| 3 | — | association | HospitalServiceController (impl) |
| 4 | Contrôleur | association | ServiceCatalogueHospitalier / HospitalServiceCatalogService |
| 5 | Service | association | ServiceHospitalier / HospitalService |
| 6 | ServiceHospitalier | association | Lit / Department (impl: HospitalService → Department) |
| 7 | Service (impl) | association | Bed |

---

## CU 4 — Gérer les activités du système

**Fichiers :** `CLASSES_PARTICIPANTES_ACTIVITES_FR.puml` · `CLASSES_PARTICIPANTES_ACTIVITES.puml`

| # | Source | Relation | Cible |
|---|--------|----------|-------|
| 1 | Administrateur / Admin | association | InterfaceSupervision / ReportingPage |
| 2 | Interface | association | ControleurJournalActivite / AuditBffController |
| 3 | Interface (impl) | association | StatsBffController |
| 4 | Contrôleur BFF | association | AuditEventController |
| 5 | Contrôleur | association | ServiceTraçabilite / AuditEventService |
| 6 | Service | association | JournalActivite / AuditEvent |

---

## CU 5 — Enregistrer un patient

**Fichiers :** `CLASSES_PARTICIPANTES_PATIENT_FR.puml` · `CLASSES_PARTICIPANTES_PATIENT.puml`

| # | Source | Relation | Cible |
|---|--------|----------|-------|
| 1 | Receptionniste | association | InterfacePatients / PatientsPage |
| 2 | Interface | association | ControleurPatients / PatientBffController |
| 3 | — | association | PatientController (impl) |
| 4 | Contrôleur | association | ServicePatients / PatientRegistryService |
| 5 | Service | association | Patient |
| 6 | Service (impl) | association | PatientDossierSequence |

---

## CU 6 — Gérer les admissions

**Fichiers :** `CLASSES_PARTICIPANTES_ADMISSIONS_FR.puml` · `CLASSES_PARTICIPANTES_ADMISSIONS.puml`

| # | Source | Relation | Cible | Type |
|---|--------|----------|-------|------|
| 1 | Receptionniste | association | InterfaceAdmissions / AdmissionsPage | |
| 2 | Interface | association | ControleurAdmissions / AdmissionBffController | |
| 3 | — | association | AdmissionController | impl |
| 4 | Contrôleur | association | ServiceAdmissions / AdmissionService | |
| 5 | Service | association | Admission | |
| 6 | Service (FR) | association | Sejour | analyse |
| 7 | AdmissionService | **dépendance** | PatientServiceClient | impl |
| 8 | AdmissionService | **dépendance** | CatalogServiceClient | impl |
| 9 | AdmissionService | **dépendance** | StayServiceClient | impl |

---

## CU 7 — Prise en charge médicale

**Fichiers :** `CLASSES_PARTICIPANTES_PRISE_EN_CHARGE_FR.puml` · `CLASSES_PARTICIPANTES_PRISE_EN_CHARGE.puml`

| # | Source | Relation | Cible |
|---|--------|----------|-------|
| 1 | Medecin / Med | association | InterfaceDossierMedical / ConsultationDetailView |
| 2 | Interface | association | ControleurPriseEnCharge / ConsultationBffController |
| 3 | Interface (impl) | association | DiseaseCatalogBffController |
| 4 | Interface (impl) | association | PrescriptionBffController |
| 5 | BFF | association | ConsultationController |
| 6 | Contrôleur | association | ServiceClinique / ConsultationService |
| 7 | Contrôleur (FR) | association | DossierMedical |
| 8 | Contrôleur (FR) | association | Consultation |
| 9 | Consultation | association | EvenementConsultation / ConsultationEvent |
| 10 | DossierMedical | association | Diagnostic |
| 11 | DossierMedical | association | Prescription |
| 12 | ServiceClinique | association | CatalogueMaladie / DiseaseCatalogService |
| 13 | DiseaseCatalogService | association | DiseaseCatalog |

---

## CU 8 — Enregistrer les soins effectués

**Fichiers :** `CLASSES_PARTICIPANTES_SOINS_FR.puml` · `CLASSES_PARTICIPANTES_SOINS.puml`

| # | Source | Relation | Cible |
|---|--------|----------|-------|
| 1 | Infirmier | association | InterfaceSoins / MedicalRecordDetailPage |
| 2 | Interface | association | ControleurSoins / PatientClinicalBffController |
| 3 | — | association | MedicalRecordController |
| 4 | Contrôleur | association | ServiceSoins / ClinicalRecordService |
| 5 | Service | association | SoinInfirmier / NursingCareRecord |
| 6 | Service | association | AdministrationMedicament / MedicationAdministration |
| 7 | DossierMedical (FR) | association | SoinInfirmier |
| 8 | Prescription (FR) | association | AdministrationMedicament |
| 9 | Service (impl) | association | PrescriptionLine |

---

## Synthèse implémentation (`CLASSES_PARTICIPANTES_AFYA.puml`)

Relations **supplémentaires** (dépendances inter-services) :

| Source | Relation | Cible | Note |
|--------|----------|-------|------|
| UserAdminApplicationService | dépendance | AuditPublisher | audit |
| CatalogAdminApplicationService | dépendance | AuditPublisher | audit |
| AdmissionOrchestrationService | dépendance | PatientRestClient | HTTP patient |
| AdmissionOrchestrationService | dépendance | CatalogReadClient | HTTP catalogue |
| ClinicalApplicationService | dépendance | PatientRestClient | valider patient |
| CarePathApplicationService | dépendance | AdmissionOrchestrationService | sortie / transfert |
| NursingApplicationService | dépendance | MedicalRecord | dossier existant |

---

## Récapitulatif par type (classes participantes seules)

| Type | Analyse FR (8 CU) | Implémentation (8 CU) |
|------|-------------------|------------------------|
| Association `-->` | 42 | ~58 |
| Dépendance `..>` | 0 | 10 |

---

## Export PlantUML

```bash
# Relations seules — analyse FR (8 pages PDF)
plantuml docs/plantuml/class_participantes_et_activite/RELATIONS_CLASSES_PARTICIPANTES_FR.puml

# Relations seules — implémentation
plantuml docs/plantuml/class_participantes_et_activite/RELATIONS_CLASSES_PARTICIPANTES_IMPL.puml
```
