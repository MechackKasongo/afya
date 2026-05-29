# Classes participantes et activités — mémoire Afya

Diagrammes **phase d'analyse** (avant développement), alignés sur le § II.3.2 du mémoire.

## Fichiers principaux (français)

| Fichier | Contenu |
|---------|---------|
| [CLASSES_PARTICIPANTES_ANALYSE_FR.puml](CLASSES_PARTICIPANTES_ANALYSE_FR.puml) | **8 diagrammes de classes participantes** en un seul fichier (frontière / contrôle / entité, attributs et méthodes en **français**) — une page par CU |
| [DIAGRAMME_PERSISTANCE_AFYA.puml](DIAGRAMME_PERSISTANCE_AFYA.puml) | **Diagramme de persistance seul** — entités, attributs, associations (sans interface ni contrôleur) |
| [RELATIONS_UML_DIAGRAMMES.md](RELATIONS_UML_DIAGRAMMES.md) | **Guide des relations UML** — association, dépendance, composition, multiplicités |

## Relations UML utilisées

| Diagramme | Relations | Symbole PlantUML |
|-----------|-----------|------------------|
| Classes participantes (analyse) | **Association** (flux acteur → interface → contrôle → entité) | `-->` |
| Persistance | **Composition** (dossier contient prescriptions) | `"1" *-- "0..*"` |
| Persistance | **Association** (FK, transferts) | `"1" -- "0..*"` |
| Persistance / domaine | **Dépendance** (ref logique entre microservices) | `..> identifiantPatient` |
| Implémentation | **Dépendance** (clients HTTP externes) | `..> PatientServiceClient` |

Détail complet : [RELATIONS_UML_DIAGRAMMES.md](RELATIONS_UML_DIAGRAMMES.md).

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
plantuml docs/plantuml/class_participantes_et_activite/CLASSES_PARTICIPANTES_ANALYSE_FR.puml
plantuml docs/plantuml/class_participantes_et_activite/DIAGRAMME_PERSISTANCE_AFYA.puml
```

Version Mermaid (français + anglais) : [../../MEMOIRE_UML_ANALYSE_FR.md](../../MEMOIRE_UML_ANALYSE_FR.md) · [../../MEMOIRE_UML_ANALYSE_EN.md](../../MEMOIRE_UML_ANALYSE_EN.md)
