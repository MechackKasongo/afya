# Classes participantes et activités — mémoire Afya

Diagrammes **phase d'analyse** (avant développement), alignés sur le § II.3.2 du mémoire.

## Fichiers principaux (français)

| Fichier | Contenu |
|---------|---------|
| [CLASSES_PARTICIPANTES_ANALYSE_FR.puml](CLASSES_PARTICIPANTES_ANALYSE_FR.puml) | **8 diagrammes** regroupés en un fichier multi-pages (export PDF) |
| Fichiers `CLASSES_PARTICIPANTES_*_FR.puml` | **Un diagramme par CU**, en **français** (phase analyse) — voir tableau ci-dessous |
| [DIAGRAMME_PERSISTANCE_AFYA.puml](DIAGRAMME_PERSISTANCE_AFYA.puml) | **Diagramme de persistance seul** — entités, attributs, associations (sans interface ni contrôleur) |
| [RELATIONS_UML_DIAGRAMMES.md](RELATIONS_UML_DIAGRAMMES.md) | **Guide des relations UML** — types, symboles, bonnes pratiques |
| [RELATIONS_CLASSES_PARTICIPANTES_AFYA.md](RELATIONS_CLASSES_PARTICIPANTES_AFYA.md) | **Toutes les relations — classes participantes** (8 CU, FR + impl.) |
| [RELATIONS_CLASSES_PARTICIPANTES_FR.puml](RELATIONS_CLASSES_PARTICIPANTES_FR.puml) | Diagramme relations seules — analyse FR (8 pages) |
| [RELATIONS_CLASSES_PARTICIPANTES_IMPL.puml](RELATIONS_CLASSES_PARTICIPANTES_IMPL.puml) | Diagramme relations seules — implémentation (8 pages) |
| [TOUTES_LES_RELATIONS_UML_AFYA.md](TOUTES_LES_RELATIONS_UML_AFYA.md) | Inventaire global (persistance + participantes + CU) |
| [RELATIONS_PERSISTANCE_AFYA.puml](RELATIONS_PERSISTANCE_AFYA.puml) | Diagramme relations seules — persistance |
| [RELATIONS_GATEWAYS_BFF_AFYA.md](RELATIONS_GATEWAYS_BFF_AFYA.md) | Relations des gateways/BFF (controllers -> clients) |
| [RELATIONS_GATEWAYS_BFF.puml](RELATIONS_GATEWAYS_BFF.puml) | Diagramme relations seules — gateway/BFF |

## Relations UML utilisées

| Diagramme | Relations | Symbole PlantUML |
|-----------|-----------|------------------|
| Classes participantes (analyse) | **Association** (flux acteur → interface → contrôle → entité) | `-->` |
| Persistance | **Composition** (dossier contient prescriptions) | `"1" *-- "0..*"` |
| Persistance | **Association** (FK, transferts) | `"1" -- "0..*"` |
| Persistance / domaine | **Dépendance** (ref logique entre microservices) | `..> identifiantPatient` |
| Implémentation | **Dépendance** (clients HTTP externes) | `..> PatientServiceClient` |

Détail complet : [RELATIONS_UML_DIAGRAMMES.md](RELATIONS_UML_DIAGRAMMES.md).

## Classes participantes — analyse (français, un fichier par CU)

| CU | Fichier |
|----|---------|
| 1 — S'authentifier | [CLASSES_PARTICIPANTES_AUTHENTIFICATION_FR.puml](CLASSES_PARTICIPANTES_AUTHENTIFICATION_FR.puml) |
| 2 — Gérer les utilisateurs | [CLASSES_PARTICIPANTES_UTILISATEURS_FR.puml](CLASSES_PARTICIPANTES_UTILISATEURS_FR.puml) |
| 3 — Gérer les services hospitaliers | [CLASSES_PARTICIPANTES_SERVICES_HOSP_FR.puml](CLASSES_PARTICIPANTES_SERVICES_HOSP_FR.puml) |
| 4 — Gérer les activités du système | [CLASSES_PARTICIPANTES_ACTIVITES_FR.puml](CLASSES_PARTICIPANTES_ACTIVITES_FR.puml) |
| 5 — Enregistrer un patient | [CLASSES_PARTICIPANTES_PATIENT_FR.puml](CLASSES_PARTICIPANTES_PATIENT_FR.puml) |
| 6 — Gérer les admissions | [CLASSES_PARTICIPANTES_ADMISSIONS_FR.puml](CLASSES_PARTICIPANTES_ADMISSIONS_FR.puml) |
| 7 — Prise en charge médicale | [CLASSES_PARTICIPANTES_PRISE_EN_CHARGE_FR.puml](CLASSES_PARTICIPANTES_PRISE_EN_CHARGE_FR.puml) |
| 8 — Enregistrer les soins | [CLASSES_PARTICIPANTES_SOINS_FR.puml](CLASSES_PARTICIPANTES_SOINS_FR.puml) |

Stéréotypes : `<<frontière>>`, `<<contrôle>>`, `<<entité>>`. Les fichiers sans suffixe `_FR` restent en **anglais** (implémentation Java/BFF).

## Diagrammes d'activité (par CU)

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

## Classes participantes (implémentation — anglais)

Fichiers individuels par CU (`CLASSES_PARTICIPANTES_*.puml`) : noms Java / BFF après développement.

## Export PNG / PDF

```bash
# Un CU en français
plantuml docs/plantuml/class_participantes_et_activite/CLASSES_PARTICIPANTES_AUTHENTIFICATION_FR.puml

# Tous les CU français
plantuml docs/plantuml/class_participantes_et_activite/CLASSES_PARTICIPANTES_*_FR.puml

# Regroupé multi-pages (PDF)
plantuml docs/plantuml/class_participantes_et_activite/CLASSES_PARTICIPANTES_ANALYSE_FR.puml
plantuml docs/plantuml/class_participantes_et_activite/DIAGRAMME_PERSISTANCE_AFYA.puml
```

Version Mermaid (français + anglais) : [../../MEMOIRE_UML_ANALYSE_FR.md](../../MEMOIRE_UML_ANALYSE_FR.md) · [../../MEMOIRE_UML_ANALYSE_EN.md](../../MEMOIRE_UML_ANALYSE_EN.md)
