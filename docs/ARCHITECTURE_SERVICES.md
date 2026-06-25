# Architecture — prototype Afya (7 services)

> **Référence mémoire (9 microservices)** : [MODELE_DOMAINE_MEMOIRE_9_SERVICES.md](MODELE_DOMAINE_MEMOIRE_9_SERVICES.md)  
> **Mapping mémoire → prototype** : [MAPPING_MODELE_ANALYSE_AFYA.md](MAPPING_MODELE_ANALYSE_AFYA.md)

## 1. Vue d’ensemble

Architecture **multi-services** (microservices modérés) : chaque service est **déployable séparément**, possède **sa persistance**, expose une **API REST** versionnée, et communique avec les autres par **HTTP** (synchrone) et **événements** (asynchrone, surtout pour l’audit).

```
                    [ Navigateur web responsive ]
                                │
                                ▼
                      [ BFF (afya-bff) ]
                                │
    ┌───────────┬───────────┬───┴───┬───────────┬───────────┬───────────┐
    ▼           ▼           ▼       ▼           ▼           ▼           ▼
 identity    catalog     patient  care-entry   stay    clinical-    audit
                                              record
    │           │           │       │           │           │           ▲
    └───────────┴───────────┴───────┴───────────┴───────────┴───────────┘
                          événements métier (qui / quoi / quand)
```

Le **BFF** (`afya-bff`) n’est pas un service métier : il agrège les appels pour le SPA React et évite d’exposer 7 URLs au navigateur.

> **Décision d’architecture** : une **API Gateway** dédiée (routage, TLS, rate limiting, accès multi-clients) est **prévue en fin de projet** ; en attendant, le **BFF seul** constitue le point d’entrée API pour le front. Lors de l’introduction de la gateway, le BFF restera en général **derrière** elle (la gateway ne remplace pas l’agrégation métier du BFF).

## 2. Les 7 services

### 2.1 identity-service

| Élément | Détail |
|---------|--------|
| **Responsabilité** | Authentification, comptes utilisateurs, rôles (Admin, Réceptionniste, Médecin, Infirmier), périmètre (services hospitaliers assignés). |
| **Données** | `users`, `roles`, `assignments`, tokens / sessions. |
| **API exemple** | `POST /api/v1/auth/login`, `GET /api/v1/users`, CRUD comptes (Admin). |
| **Acteurs** | Tous (s’authentifier) ; Admin (gérer comptes). |

### 2.2 catalog-service

| Élément | Détail |
|---------|--------|
| **Responsabilité** | Départements, services hospitaliers, capacités (lits si besoin). |
| **Données** | Catalogue référentiel, peu volatile. |
| **API exemple** | `GET /api/v1/hospital-services`, CRUD (Admin). |
| **Acteurs** | Admin ; lecture par réception, affectation patient. |

### 2.3 patient-service

| Élément | Détail |
|---------|--------|
| **Responsabilité** | Enregistrement patient, recherche, identifiant stable `patientId`. |
| **Données** | Identité administrative (nom, contacts, etc.) — pas le détail clinique complet. |
| **API exemple** | `POST /api/v1/patients`, `GET /api/v1/patients?query=`. |
| **Acteurs** | Réceptionniste, Médecin, Infirmier (recherche). |

### 2.4 care-entry-service

| Élément | Détail |
|---------|--------|
| **Responsabilité** | Passages **urgences**, **admissions**, affectation à un service, **transferts administratifs**, historique des admissions. |
| **Données** | Liens `patientId`, `hospitalServiceId`, statuts de passage. |
| **API exemple** | `POST /api/v1/admissions`, `POST /api/v1/urgences`, `POST /api/v1/transfers`. |
| **Acteurs** | Réceptionniste ; Médecin (décision sortie/transfert côté parcours). |

### 2.5 stay-service

| Élément | Détail |
|---------|--------|
| **Responsabilité** | **Séjour** d’hospitalisation, **fiche d’hospitalisation** (données structurées du séjour). |
| **Données** | Chambre/lit, dates, lien admission / `stayId`. |
| **API exemple** | `GET/PUT /api/v1/stays/{id}/hospitalization-form`. |
| **Acteurs** | Réceptionniste, Médecin, Infirmier (consultation fiche séjour). |

> **Variante 6 services** : fusionner **care-entry** + **stay** → **encounter-stay-service**.

### 2.6 clinical-record-service

| Élément | Détail |
|---------|--------|
| **Responsabilité** | **Dossier médical** : diagnostics, observations, **prescriptions**, **soins infirmiers**, exécution / marquage, **documents et images** (métadonnées + stockage objet). |
| **Données** | Entrées cliniques ; fichiers dans **MinIO/S3** (pas en BLOB lourd en Oracle/Postgres). |
| **API exemple** | `GET /api/v1/patients/{id}/medical-record`, prescriptions, nursing-care, `POST /api/v1/documents`. |
| **Acteurs** | Médecin, Infirmier. |

### 2.7 audit-service

| Élément | Détail |
|---------|--------|
| **Responsabilité** | **Traces** : qui a fait quoi, quand, sur quelle ressource ; **rapports d’activité** et **statistiques** pour l’Admin. |
| **Données** | Journal append-only ; pas de contenu clinique sensible dans le message si possible. |
| **API exemple** | `GET /api/v1/audit/events`, `GET /api/v1/reports/activity`. |
| **Acteurs** | Admin (rapports, stats) ; alimentation automatique par tous les services. |

## 3. Règles transverses

- **Pas de JOIN cross-service** en SQL : références par identifiants (`patientId`, `stayId`, `admissionId`).  
- **Sécurité** : JWT émis par **identity** ; chaque service valide le token et les rôles.  
- **Audit** : chaque service publie un événement (`UserX created PatientY`) vers **audit** (file ou HTTP asynchrone).  
- **Rendez-vous** : **hors périmètre** Afya Platform (pas de planification RDV ; la table `appointments` dans `patient-service` n’est pas exposée ni maintenue fonctionnellement).

## 4. Ordre de réalisation suggéré

1. identity-service  
2. catalog-service  
3. patient-service  
4. care-entry-service + stay-service (ou fusion encounter-stay)  
5. clinical-record-service (+ stockage objet)  
6. audit-service + branchement événements  
7. BFF + front web responsive  
8. API Gateway (phase E — `infra/gateway`, port **8090**, devant le BFF, sans logique métier)  
