# Cartographie des exigences fonctionnelles → microservices

Ce document relie les **cas d'utilisation** du mémoire (II.2.1.2) aux **9 microservices** de référence ([MODELE_DOMAINE_MEMOIRE_9_SERVICES.md](MODELE_DOMAINE_MEMOIRE_9_SERVICES.md)).

> **Prototype Afya (7 services Java)** : voir [annexe — implémentation](#annexe--correspondance-prototype-afya-7-services).

---

## Légende — modèle mémoire (9 services)

| Code | Microservice | Responsabilité |
|------|--------------|----------------|
| **AUTH** | Auth Service (MD-01) | Authentification, JWT, journal connexion |
| **USER** | User Service (MD-02) | Comptes, rôles, affectations |
| **HOSP** | Hospital Service (MD-03) | Services hospitaliers, lits, occupation |
| **PAT** | Patient Service (MD-04) | Identité patient, antécédents, contacts urgence |
| **ADM** | Admission Service (MD-05) | Admissions, transferts, sorties, notifications |
| **MED** | Medical Service (MD-06) | Dossier médical, consultations, prescriptions, décisions |
| **LAB** | Lab Service (MD-07) | Demandes d'examens, prélèvements, résultats |
| **NUR** | Nursing Service (MD-08) | Soins infirmiers, constantes vitales, alertes |
| **RPT** | Report Service (MD-09) | Rapports et statistiques d'activité |

---

## Admin

| Cas d'utilisation | Service principal | Service(s) secondaire | Notes |
|-------------------|-------------------|------------------------|-------|
| S'authentifier | **AUTH** | — | Login, JWT, refresh |
| Gérer compte utilisateurs | **USER** | **AUTH**, **RPT** | CRUD comptes ; credentials dans Auth |
| Gérer département et services hospitaliers | **HOSP** | **RPT** | Référentiel hospitalier |
| Générer des rapports sur l'activité du système | **RPT** | **ADM**, **MED**, **LAB**, **NUR** | Agrégation multi-sources |
| Consulter les statistiques | **RPT** | — | Tableaux de bord, KPI |
| Gérer utilisateurs (tableau ANALYSE) | **USER** | **AUTH** | Idem gestion comptes |
| Générer les activités du système | **RPT** | **USER** | Journal actions + export |

---

## Réceptionniste

| Cas d'utilisation | Service principal | Service(s) secondaire | Notes |
|-------------------|-------------------|------------------------|-------|
| S'authentifier | **AUTH** | — | |
| Enregistrer un patient | **PAT** | **RPT** | Création référentiel + antécédents |
| Rechercher un patient existant | **PAT** | — | |
| Enregistrer l'admission d'un patient | **ADM** | **PAT**, **HOSP**, **RPT** | Vérifie patient + lit disponible |
| Affecter un patient à un service | **ADM** | **HOSP** | Affectation lit / service |
| Gérer les transferts administratifs | **ADM** | **HOSP**, **RPT** | Transfert inter-services |
| Consulter l'historique des admissions | **ADM** | — | Liste par `patientId` |
| Gérer les admissions (tableau ANALYSE) | **ADM** | **HOSP** | Ouverture / clôture parcours |

---

## Médecin

| Cas d'utilisation | Service principal | Service(s) secondaire | Notes |
|-------------------|-------------------|------------------------|-------|
| S'authentifier | **AUTH** | — | |
| Accéder au dossier médical du patient | **MED** | **PAT** | Lecture dossier ; identité depuis PAT |
| Ajouter diagnostics et observations | **MED** | **RPT** | Consultation, diagnostic |
| Enregistrer prescriptions médicales | **MED** | **NUR**, **RPT** | Notification infirmière |
| Prescrire examens de laboratoire | **LAB** | **MED**, **PAT** | DemandeExamen |
| Décider transfert ou sortie du patient | **MED** | **ADM** | DecisionMedicale → ADM |
| Prise en charge médicale (tableau ANALYSE) | **MED** | **ADM** | Parcours + actes cliniques |

---

## Infirmier(ère)

| Cas d'utilisation | Service principal | Service(s) secondaire | Notes |
|-------------------|-------------------|------------------------|-------|
| S'authentifier | **AUTH** | — | |
| Rechercher et accéder au dossier du patient | **PAT** + **MED** | — | |
| Consulter les prescriptions médicales | **MED** | **NUR** | NotificationPrescription |
| Enregistrer les soins infirmiers réalisés | **NUR** | **RPT** | SoinInfirmier |
| Relever les constantes vitales | **NUR** | **RPT** | ConstanteVitale, AlerteConstante |
| Marquer l'exécution des prescriptions | **NUR** | **MED** | Lien prescription ↔ soin |
| Consulter l'historique des interventions | **NUR** | — | Timeline soins |
| Enregistrer les soins (tableau ANALYSE) | **NUR** | **MED** | |

---

## Laborantin

| Cas d'utilisation | Service principal | Service(s) secondaire | Notes |
|-------------------|-------------------|------------------------|-------|
| S'authentifier | **AUTH** | — | |
| Consulter les demandes d'examens | **LAB** | **MED** | DemandeExamen en attente |
| Enregistrer un prélèvement | **LAB** | **RPT** | Prelevement |
| Saisir les résultats d'examen | **LAB** | **MED**, **RPT** | ResultatExamen, ParametreResultat |
| Gérer le catalogue des types d'examens | **LAB** | — | TypeExamen (Admin) |

---

## Synthèse par microservice (mémoire)

| Microservice | Acteurs principaux | Cas d'utilisation couverts |
|--------------|-------------------|----------------------------|
| **AUTH** | Tous | Connexion, déconnexion, tokens |
| **USER** | Admin | Gestion comptes, rôles, affectations |
| **HOSP** | Admin, Réception | Services, lits, occupation |
| **PAT** | Réception, Médecin, Infirmier | Enregistrement, recherche patient |
| **ADM** | Réception, Médecin | Admission, transfert, sortie |
| **MED** | Médecin | Dossier, consultation, prescription, décision |
| **LAB** | Médecin, Laborantin | Examens, prélèvements, résultats |
| **NUR** | Infirmier | Soins, constantes, alertes |
| **RPT** | Admin | Rapports, statistiques, journaux |

---

## Hors périmètre

| Fonctionnalité | Statut |
|----------------|--------|
| **Rendez-vous (RDV)** | Non retenu dans le périmètre HGR Jason Sendwe |

---

## Annexe — correspondance prototype Afya (7 services)

Le prototype logiciel regroupe certains microservices du mémoire :

| Code mémoire | Module Afya |
|--------------|-------------|
| AUTH + USER | `auth-service` + `user-service` |
| HOSP | `hospital-service` |
| PAT | `patient-service` |
| ADM | `admission-service` |
| MED | `medical-service` |
| NUR | `nursing-service` |
| LAB | *non implémenté* |
| RPT (partiel) | `audit-service` + `afya-bff` |

Détail : [MAPPING_MODELE_ANALYSE_AFYA.md](MAPPING_MODELE_ANALYSE_AFYA.md) · Architecture Java : [ARCHITECTURE_SERVICES.md](ARCHITECTURE_SERVICES.md).
