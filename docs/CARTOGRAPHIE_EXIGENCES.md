# Cartographie des exigences fonctionnelles → services

Ce document relie les **cas d’utilisation** de l’analyse (II.2.1.2 et besoins fonctionnels) aux **7 services** cibles.

## Légende des services

| Code | Service |
|------|---------|
| **ID** | identity-service |
| **CAT** | catalog-service |
| **PAT** | patient-service |
| **CE** | care-entry-service |
| **ST** | stay-service |
| **CR** | clinical-record-service |
| **AUD** | audit-service |
| **BFF** | API Gateway / BFF (agrégation pour le front) |

---

## Admin

| Cas d’utilisation | Service principal | Service(s) secondaire | Notes |
|-------------------|-------------------|------------------------|-------|
| S’authentifier | **ID** | — | Login, JWT, refresh. |
| Gérer compte utilisateurs | **ID** | **AUD** | CRUD users ; trace Admin. |
| Gérer département et services hospitaliers | **CAT** | **AUD** | Référentiel hospitalier. |
| Générer des rapports sur l’activité du système | **AUD** | **BFF** | Agrégation stats multi-sources si besoin. |
| Consulter les statistiques | **AUD** | **BFF** | Tableaux de bord à partir des événements. |
| Gérer utilisateurs (tableau ANALYSE) | **ID** | **AUD** | Idem gestion comptes. |
| Générer les activités du système | **AUD** | — | Export / consultation journal. |

---

## Réceptionniste

| Cas d’utilisation | Service principal | Service(s) secondaire | Notes |
|-------------------|-------------------|------------------------|-------|
| S’authentifier | **ID** | — | |
| Enregistrer un patient | **PAT** | **AUD** | Création référentiel. |
| Rechercher un patient existant | **PAT** | — | |
| Enregistrer l’admission d’un patient | **CE** | **PAT**, **CAT**, **AUD** | Vérifie patient + service actif. |
| Affecter un patient à un service | **CE** | **CAT** | |
| Gérer les transferts administratifs | **CE** | **ST**, **AUD** | Transfert peut mettre à jour séjour. |
| Consulter l’historique des admissions | **CE** | **BFF** | Liste / pagination par `patientId`. |
| Gérer les admissions (tableau ANALYSE) | **CE** | **ST** | Ouverture / clôture parcours. |

---

## Médecin

| Cas d’utilisation | Service principal | Service(s) secondaire | Notes |
|-------------------|-------------------|------------------------|-------|
| S’authentifier | **ID** | — | |
| Accéder au dossier médical du patient | **CR** | **PAT**, **BFF** | Lecture dossier ; identité depuis **PAT**. |
| Ajouter diagnostics et observations | **CR** | **AUD** | |
| Enregistrer prescriptions médicales | **CR** | **AUD** | |
| Décider transfert ou sortie du patient | **CE** | **ST**, **AUD** | Décision métier → commande vers **CE** / **ST**. |
| Prise en charge médicale (tableau ANALYSE) | **CR** + **CE** | **ST** | Parcours + actes cliniques. |

---

## Infirmier(ère)

| Cas d’utilisation | Service principal | Service(s) secondaire | Notes |
|-------------------|-------------------|------------------------|-------|
| S’authentifier | **ID** | — | |
| Rechercher et accéder au dossier du patient | **PAT** + **CR** | **BFF** | |
| Consulter les prescriptions médicales | **CR** | — | Lecture seule côté prescription. |
| Enregistrer les soins infirmiers réalisés | **CR** | **AUD** | Module nursing-care. |
| Marquer l’exécution | **CR** | **AUD** | Statut prescription / administration. |
| Consulter l’historique des interventions | **CR** | **BFF** | Timeline soins. |
| Enregistrer les soins (tableau ANALYSE) | **CR** | **AUD** | |

---

## Synthèse par service (charge fonctionnelle)

| Service | Acteurs principaux | Cas d’utilisation couverts |
|---------|-------------------|----------------------------|
| **identity** | Tous, Admin | Auth, gestion comptes |
| **catalog** | Admin, CE | Services / départements |
| **patient** | Réception, Médecin, Infirmier | CRUD patient, recherche |
| **care-entry** | Réception, Médecin | Urgence, admission, transfert, historique admissions |
| **stay** | Réception, Médecin, Infirmier | Séjour, fiche d’hospitalisation |
| **clinical-record** | Médecin, Infirmier | Dossier, prescriptions, soins, images |
| **audit** | Admin | Traces, rapports, statistiques |

---

## Hors périmètre

| Fonctionnalité | Statut |
|----------------|--------|
| **Rendez-vous (RDV)** | Non retenu — pas de cas d’utilisation, pas d’exposition BFF/front ; table `appointments` éventuellement présente en base sans usage métier. |

## Points à trancher en atelier métier

1. **Urgences** : même service que admission (**CE**) ou microservice dédié si volumétrie forte.  
2. **Fiche d’hospitalisation** : formulaire structuré (**ST**) vs document PDF (**CR** + stockage objet) — souvent les deux (données + pièce jointe).
