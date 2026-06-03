# Diagrammes PlantUML — Afya

Fichiers **`.puml`** prêts pour le mémoire (export PNG/PDF/SVG) ou la doc technique.

## Diagrammes de classes par service (conception)

Un fichier par microservice : **controllers**, **services**, **repositories**, **entités** (attributs + méthodes).

| Service | Port | Fichier |
|---------|------|---------|
| identity-service | 8081 | [CLASSES_SERVICE_IDENTITY.puml](CLASSES_SERVICE_IDENTITY.puml) |
| catalog-service | 8082 | [CLASSES_SERVICE_CATALOG.puml](CLASSES_SERVICE_CATALOG.puml) |
| patient-service | 8083 | [CLASSES_SERVICE_PATIENT.puml](CLASSES_SERVICE_PATIENT.puml) |
| care-entry-service | 8084 | [CLASSES_SERVICE_CARE_ENTRY.puml](CLASSES_SERVICE_CARE_ENTRY.puml) |
| stay-service | 8085 | [CLASSES_SERVICE_STAY.puml](CLASSES_SERVICE_STAY.puml) |
| clinical-record-service | 8086 | [CLASSES_SERVICE_CLINICAL.puml](CLASSES_SERVICE_CLINICAL.puml) |
| audit-service | 8087 | [CLASSES_SERVICE_AUDIT.puml](CLASSES_SERVICE_AUDIT.puml) |
| afya-bff | 8080 | [CLASSES_SERVICE_BFF.puml](CLASSES_SERVICE_BFF.puml) |

Version Mermaid : [../MERMAID_CLASSES_PAR_SERVICE.md](../MERMAID_CLASSES_PAR_SERVICE.md).

Relations et cardinalités (inventaire unique) :
[class_participantes_et_activite/RELATIONS_UML_DIAGRAMMES.md](class_participantes_et_activite/RELATIONS_UML_DIAGRAMMES.md)

## Modèle du domaine

| Fichier | Contenu |
|---------|---------|
| [MODELE_DOMAINE_AFYA.puml](MODELE_DOMAINE_AFYA.puml) | Domaine conceptuel par microservice (7 contextes + liens logiques) |

## Classes participantes (analyse — 8 cas d'utilisation)

Chaque fichier inclut **attributs** et **méthodes** des classes boundary / control / entity.

| CU | Fichier |
|----|---------|
| S'authentifier | [CLASSES_PARTICIPANTES_AUTH.puml](CLASSES_PARTICIPANTES_AUTH.puml) |
| Gérer les utilisateurs | [CLASSES_PARTICIPANTES_UTILISATEURS.puml](CLASSES_PARTICIPANTES_UTILISATEURS.puml) |
| Gérer les services hospitaliers | [CLASSES_PARTICIPANTES_SERVICES_HOSP.puml](CLASSES_PARTICIPANTES_SERVICES_HOSP.puml) |
| Gérer les activités du système | [CLASSES_PARTICIPANTES_ACTIVITES.puml](CLASSES_PARTICIPANTES_ACTIVITES.puml) |
| Enregistrer un patient | [CLASSES_PARTICIPANTES_PATIENT.puml](CLASSES_PARTICIPANTES_PATIENT.puml) |
| Gérer les admissions | [CLASSES_PARTICIPANTES_ADMISSIONS.puml](CLASSES_PARTICIPANTES_ADMISSIONS.puml) |
| Prise en charge médicale | [CLASSES_PARTICIPANTES_PRISE_EN_CHARGE.puml](CLASSES_PARTICIPANTES_PRISE_EN_CHARGE.puml) |
| Enregistrer les soins | [CLASSES_PARTICIPANTES_SOINS.puml](CLASSES_PARTICIPANTES_SOINS.puml) |

Synthèse : [CLASSES_PARTICIPANTES_AFYA.puml](CLASSES_PARTICIPANTES_AFYA.puml)

**Classes participantes FR alignées EN** : [class_participantes_et_activite/README.md](class_participantes_et_activite/README.md) · [DIAGRAMME_PERSISTANCE_AFYA.puml](class_participantes_et_activite/DIAGRAMME_PERSISTANCE_AFYA.puml)

## Diagrammes d'activité

| CU | Fichier |
|----|---------|
| S'authentifier | [ACTIVITE_AUTHENTIFICATION_AFYA.puml](ACTIVITE_AUTHENTIFICATION_AFYA.puml) |
| Gérer les utilisateurs | [ACTIVITE_GERER_UTILISATEURS_AFYA.puml](ACTIVITE_GERER_UTILISATEURS_AFYA.puml) |
| Gérer les services hospitaliers | [ACTIVITE_GERER_SERVICES_HOSP_AFYA.puml](ACTIVITE_GERER_SERVICES_HOSP_AFYA.puml) |
| Gérer les activités du système | [ACTIVITE_GERER_ACTIVITES_AFYA.puml](ACTIVITE_GERER_ACTIVITES_AFYA.puml) |
| Enregistrer un patient | [ACTIVITE_ENREGISTRER_PATIENT_AFYA.puml](ACTIVITE_ENREGISTRER_PATIENT_AFYA.puml) |
| Gérer les admissions | [ACTIVITE_ADMISSION_PATIENT_AFYA.puml](ACTIVITE_ADMISSION_PATIENT_AFYA.puml) |
| Prise en charge médicale | [ACTIVITE_PRISE_EN_CHARGE_MEDICALE_AFYA.puml](ACTIVITE_PRISE_EN_CHARGE_MEDICALE_AFYA.puml) |
| Enregistrer les soins | [ACTIVITE_SOIN_INFIRMIER_AFYA.puml](ACTIVITE_SOIN_INFIRMIER_AFYA.puml) |

Complément : [ACTIVITE_SORTIE_TRANSFERT_AFYA.puml](ACTIVITE_SORTIE_TRANSFERT_AFYA.puml)

Diagrammes d'activité (par acteur) :

| Acteur | Fichier |
|--------|---------|
| Administrateur | [ACTIVITE_ADMIN_AFYA.puml](ACTIVITE_ADMIN_AFYA.puml) |
| Réceptionniste | [ACTIVITE_RECEPTIONNISTE_AFYA.puml](ACTIVITE_RECEPTIONNISTE_AFYA.puml) |
| Médecin | [ACTIVITE_MEDECIN_AFYA.puml](ACTIVITE_MEDECIN_AFYA.puml) |
| Infirmier·ère | [ACTIVITE_INFERMIER_AFYA.puml](ACTIVITE_INFERMIER_AFYA.puml) |

Diagramme d'activité général :

[ACTIVITE_GENERALE_AFYA.puml](ACTIVITE_GENERALE_AFYA.puml)

Diagramme **une colonne par acteur** (4 fichiers) :

[ACTIVITE_COLONNE_ADMINISTRATEUR_AFYA.puml](ACTIVITE_COLONNE_ADMINISTRATEUR_AFYA.puml) · [ACTIVITE_COLONNE_RECEPTIONNISTE_AFYA.puml](ACTIVITE_COLONNE_RECEPTIONNISTE_AFYA.puml) · [ACTIVITE_COLONNE_MEDECIN_AFYA.puml](ACTIVITE_COLONNE_MEDECIN_AFYA.puml) · [ACTIVITE_COLONNE_INFIRMIER_AFYA.puml](ACTIVITE_COLONNE_INFIRMIER_AFYA.puml)

## Diagrammes de conception

| Fichier | Type |
|---------|------|
| [CONCEPTION_COUCHE_SERVICE_AFYA.puml](CONCEPTION_COUCHE_SERVICE_AFYA.puml) | Classes de conception — couches (care-entry) |
| [CONCEPTION_CONSULTATION_AFYA.puml](CONCEPTION_CONSULTATION_AFYA.puml) | Classes — consultation / catalogue maladies |
| [CONCEPTION_SEQUENCE_AUTHENTIFICATION_AFYA.puml](CONCEPTION_SEQUENCE_AUTHENTIFICATION_AFYA.puml) | Séquence login |
| [CONCEPTION_SEQUENCE_ADMISSION_AFYA.puml](CONCEPTION_SEQUENCE_ADMISSION_AFYA.puml) | Séquence admission multi-services |
| [CONCEPTION_SEQUENCE_CLINICAL_AFYA.puml](CONCEPTION_SEQUENCE_CLINICAL_AFYA.puml) | Séquence prescription / administration |
| [CONCEPTION_SEQUENCE_PRISE_EN_CHARGE_AFYA.puml](CONCEPTION_SEQUENCE_PRISE_EN_CHARGE_AFYA.puml) | Séquence consultation + diagnostic + prescription |

## Autres vues

| Fichier | Contenu |
|---------|---------|
| [CAS_UTILISATION_AFYA.puml](CAS_UTILISATION_AFYA.puml) | Cas d'utilisation globaux |
| [COMPOSANTS_AFYA.puml](COMPOSANTS_AFYA.puml) | Composants (BFF, services, persistance) |
| [DEPLOIEMENT_AFYA.puml](DEPLOIEMENT_AFYA.puml) | Déploiement |
| [ETAT_ADMISSION_AFYA.puml](ETAT_ADMISSION_AFYA.puml) | États `Admission` |
| [ETAT_STAY_AFYA.puml](ETAT_STAY_AFYA.puml) | États `Stay` |

## Rendu rapide

```bash
# Tous les diagrammes (nécessite plantuml.jar ou paquet plantuml)
plantuml docs/plantuml/*.puml
```

- **VS Code / Cursor** : extension « PlantUML »
- **En ligne** : [plantuml.com/plantuml](http://www.plantuml.com/plantuml/uml/) (éviter données sensibles)

Index Mermaid (8 CU, activités, classes participantes) : [../MERMAID_MEMOIRE_AFYA.md](../MERMAID_MEMOIRE_AFYA.md)  
Index général : [../DIAGRAMMES_UML.md](../DIAGRAMMES_UML.md)
