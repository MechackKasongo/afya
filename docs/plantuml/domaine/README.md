# Modèle du domaine — par service (schéma BDD)

Un diagramme par **base PostgreSQL** (tables Flyway + références logiques externes).

| Service | Base | Port | Fichier |
|---------|------|------|---------|
| identity-service | `afya_identity` | 5433 | [MODELE_DOMAINE_IDENTITY_SERVICE.puml](MODELE_DOMAINE_IDENTITY_SERVICE.puml) |
| catalog-service | `afya_catalog` | 5434 | [MODELE_DOMAINE_CATALOG_SERVICE.puml](MODELE_DOMAINE_CATALOG_SERVICE.puml) |
| patient-service | `afya_patient` | 5435 | [MODELE_DOMAINE_PATIENT_SERVICE.puml](MODELE_DOMAINE_PATIENT_SERVICE.puml) |
| care-entry-service | `afya_care_entry` | 5436 | [MODELE_DOMAINE_CARE_ENTRY_SERVICE.puml](MODELE_DOMAINE_CARE_ENTRY_SERVICE.puml) |
| stay-service | `afya_stay` | 5437 | [MODELE_DOMAINE_STAY_SERVICE.puml](MODELE_DOMAINE_STAY_SERVICE.puml) |
| clinical-record-service | `afya_clinical` | 5438 | [MODELE_DOMAINE_CLINICAL_RECORD_SERVICE.puml](MODELE_DOMAINE_CLINICAL_RECORD_SERVICE.puml) |
| audit-service | `afya_audit` | 5439 | [MODELE_DOMAINE_AUDIT_SERVICE.puml](MODELE_DOMAINE_AUDIT_SERVICE.puml) |

Vue **toutes bases** : [../MODELE_DOMAINE_BDD_AFYA.puml](../MODELE_DOMAINE_BDD_AFYA.puml)  
Domaine **JPA + méthodes** : [../MODELE_DOMAINE_AFYA.puml](../MODELE_DOMAINE_AFYA.puml)
