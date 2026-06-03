# Classes participantes et activités — mémoire Afya

Diagrammes des classes participantes et activités, alignés sur le § II.3.2 du mémoire.

## Fichiers principaux (français)

| Fichier | Contenu |
|---------|---------|
| Fichiers `CLASSES_PARTICIPANTES_*_FR.puml` | **Un diagramme par CU**, version FR **alignée** aux fichiers EN (mêmes classes, attributs, relations) |
| [DIAGRAMME_PERSISTANCE_AFYA.puml](DIAGRAMME_PERSISTANCE_AFYA.puml) | **Diagramme de persistance seul** — entités, attributs, associations (sans interface ni contrôleur) |
| [RELATIONS_UML_DIAGRAMMES.md](RELATIONS_UML_DIAGRAMMES.md) | **Inventaire unique** — symboles, relations, cardinalités (persistance, 8 CU, BFF) |

## Relations UML utilisées

| Diagramme | Relations | Symbole PlantUML |
|-----------|-----------|------------------|
| Classes participantes (analyse) | **Association** (flux acteur → interface → contrôle → entité) | `-->` |
| Persistance | **Composition** (dossier contient prescriptions) | `"1" *-- "0..*"` |
| Persistance | **Association** (FK, transferts) | `"1" -- "0..*"` |
| Persistance / domaine | **Dépendance** (ref logique entre microservices) | `..> identifiantPatient` |
| Implémentation | **Dépendance** (clients HTTP externes) | `..> PatientServiceClient` |

Détail complet : [RELATIONS_UML_DIAGRAMMES.md](RELATIONS_UML_DIAGRAMMES.md).

## Classes participantes — FR aligné EN (un fichier par CU)

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

Stéréotypes : `<<boundary>>`, `<<control>>`, `<<entity>>` (noms techniques identiques EN/FR pour éviter les divergences). Les fichiers sans suffixe `_FR` restent en anglais.

Règle de maintenance : tout changement dans un fichier `CLASSES_PARTICIPANTES_*.puml` doit être répliqué dans son équivalent `*_FR.puml`.

**Sur chaque diagramme** : type de relation (`association`, `composition`, `dépendance`) et cardinalités (`"1"`, `"0..*"`, `"0..1"`) sont annotés sur les flèches. Inventaire tabulaire : [RELATIONS_UML_DIAGRAMMES.md](RELATIONS_UML_DIAGRAMMES.md).

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

## Diagrammes d'activité (par acteur)

Chaque acteur parcourt les cas d’utilisation qui lui sont associés dans le mémoire (authentification incluse au début).

| Acteur | Fichier |
|--------|---------|
| Administrateur | [ACTIVITE_ADMIN_AFYA.puml](ACTIVITE_ADMIN_AFYA.puml) |
| Réceptionniste | [ACTIVITE_RECEPTIONNISTE_AFYA.puml](ACTIVITE_RECEPTIONNISTE_AFYA.puml) |
| Médecin | [ACTIVITE_MEDECIN_AFYA.puml](ACTIVITE_MEDECIN_AFYA.puml) |
| Infirmier·ère | [ACTIVITE_INFERMIER_AFYA.puml](ACTIVITE_INFERMIER_AFYA.puml) |

## Diagramme d'activité général

[ACTIVITE_GENERALE_AFYA.puml](ACTIVITE_GENERALE_AFYA.puml)

## Diagramme d'activité — acteurs et capacités (4 fichiers)

Vue **menu utilisateur** (style OPJ/Admin) : authentification (décision), **Choisir opération**, **fork** des cas d'utilisation, **décisions** ◇ seulement pour les vrais choix (trouvé/valide/sortie…). Le détail technique est dans les `ACTIVITE_*_AFYA.puml` par CU. Aligné sur [CAS_UTILISATION_AFYA.puml](CAS_UTILISATION_AFYA.puml).

| Acteur | Fichier |
|--------|---------|
| Administrateur | [ACTIVITE_CAPACITES_ADMINISTRATEUR_AFYA.puml](ACTIVITE_CAPACITES_ADMINISTRATEUR_AFYA.puml) |
| Réceptionniste | [ACTIVITE_CAPACITES_RECEPTIONNISTE_AFYA.puml](ACTIVITE_CAPACITES_RECEPTIONNISTE_AFYA.puml) |
| Médecin | [ACTIVITE_CAPACITES_MEDECIN_AFYA.puml](ACTIVITE_CAPACITES_MEDECIN_AFYA.puml) |
| Infirmier(ère) | [ACTIVITE_CAPACITES_INFIRMIER_AFYA.puml](ACTIVITE_CAPACITES_INFIRMIER_AFYA.puml) |

## Diagramme d'activité — une colonne par acteur

**Un fichier PlantUML par acteur** (colonne swimlane + flux complet).  
`newpage` / `detach` ne sont pas fiables en diagramme d'activité (beta PlantUML).

| Acteur | Fichier |
|--------|---------|
| Administrateur | [ACTIVITE_COLONNE_ADMINISTRATEUR_AFYA.puml](ACTIVITE_COLONNE_ADMINISTRATEUR_AFYA.puml) |
| Réceptionniste | [ACTIVITE_COLONNE_RECEPTIONNISTE_AFYA.puml](ACTIVITE_COLONNE_RECEPTIONNISTE_AFYA.puml) |
| Médecin | [ACTIVITE_COLONNE_MEDECIN_AFYA.puml](ACTIVITE_COLONNE_MEDECIN_AFYA.puml) |
| Infirmier | [ACTIVITE_COLONNE_INFIRMIER_AFYA.puml](ACTIVITE_COLONNE_INFIRMIER_AFYA.puml) |

Fichier regroupé (admin + note) : [ACTIVITE_PAR_ACTEUR_COLONNES_AFYA.puml](ACTIVITE_PAR_ACTEUR_COLONNES_AFYA.puml)

```bash
plantuml docs/plantuml/class_participantes_et_activite/ACTIVITE_COLONNE_*_AFYA.puml
```

## Classes participantes (implémentation — anglais)

Fichiers individuels par CU (`CLASSES_PARTICIPANTES_*.puml`) : noms Java / BFF après développement.

## Export PNG / PDF

```bash
# Un CU en français
plantuml docs/plantuml/class_participantes_et_activite/CLASSES_PARTICIPANTES_AUTHENTIFICATION_FR.puml

# Tous les CU français
plantuml docs/plantuml/class_participantes_et_activite/CLASSES_PARTICIPANTES_*_FR.puml

# Persistance (modèle complet)
plantuml docs/plantuml/class_participantes_et_activite/DIAGRAMME_PERSISTANCE_AFYA.puml
```

Version Mermaid (français + anglais) : [../../MEMOIRE_UML_ANALYSE_FR.md](../../MEMOIRE_UML_ANALYSE_FR.md) · [../../MEMOIRE_UML_ANALYSE_EN.md](../../MEMOIRE_UML_ANALYSE_EN.md)

## Autres vues (PlantUML)

| Fichier | Contenu |
|---------|---------|
| [../COMPOSANTS_AFYA.puml](../COMPOSANTS_AFYA.puml) | Diagrammes de composants (BFF, services, observabilité) |
| [../DEPLOIEMENT_AFYA.puml](../DEPLOIEMENT_AFYA.puml) | Diagramme de déploiement (gateway, conteneurs, métriques) |
