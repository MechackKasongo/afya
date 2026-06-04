# Diagramme de composants — plateforme Afya

Vue **déploiement** : composants exécutables, ports, persistance et dépendances REST.  
PlantUML (détaillé) : [plantuml/COMPOSANTS_AFYA.puml](plantuml/COMPOSANTS_AFYA.puml).

## Vue d'ensemble

```mermaid
flowchart TB
  subgraph client["Poste client"]
    BR[Navigateur]
    SPA[frontend React + Vite]
    WEB[Nginx UI :8088]
    BR --> SPA
    SPA --> WEB
  end

  subgraph edge["Couche exposition"]
    GW[API Gateway Nginx\n8090 / 8443]
    BFF[afya-bff :8080]
    GW --> BFF
  end

  WEB -->|HTTPS /api/v1| GW

  subgraph shared["afya-shared (bibliothèque)"]
    CORR[CorrelationIdFilter]
    RES[HttpResilienceInterceptor]
    AUDP[RestAuditEventPublisher]
    JWT[JwtAuthDetails / HospitalScope]
  end

  subgraph svc["Services métier"]
    ID[identity :8081]
    CAT[catalog :8082]
    PAT[patient :8083]
    CE[care-entry :8084]
    ST[stay :8085]
    CR[clinical-record :8086]
    AUD[audit :8087]
  end

  BFF --> ID & CAT & PAT & CE & ST & CR & AUD

  CE -->|REST| PAT
  CE -->|REST| CAT
  CE -->|REST| ST
  ST -->|REST| CE
  ST -->|REST| PAT
  CR -->|REST| PAT
  CR -->|REST| CE

  ID & CAT & PAT & CE & ST & CR -.->|HTTP audit| AUDP
  AUDP --> AUD

  BFF -.-> CORR & RES & JWT
  CE & ST & CR -.-> RES
  ID & CAT & PAT & CE & ST & CR & AUD -.-> CORR

  subgraph data["Persistance"]
    D1[(PostgreSQL identity)]
    D2[(PostgreSQL catalog)]
    D3[(PostgreSQL patient)]
    D4[(PostgreSQL care-entry)]
    D5[(PostgreSQL stay)]
    D6[(PostgreSQL clinical)]
    D7[(PostgreSQL audit)]
    MINIO[(MinIO :9000)]
  end

  ID --> D1
  CAT --> D2
  PAT --> D3
  CE --> D4
  ST --> D5
  CR --> D6
  AUD --> D7
  CR --> MINIO
```

## afya-bff (décomposition interne)

| Composant | Rôle |
|-----------|------|
| **Contrôleurs REST** | Point d'entrée unique `/api/v1/*` pour le front (auth, patients, admissions, urgences, consultations, users, audit, stats…) |
| **Clients REST** | `IdentityClient`, `CatalogClient`, `PatientClient`, `CareEntryClient`, `StayClient`, `ClinicalRecordClient`, `AuditClient` |
| **Services d'agrégation** | KPI tableau de bord, rapports admin, enrichissement listes admissions |

## Matrice des dépendances REST

| Source | Cible | Usage |
|--------|--------|--------|
| **web / SPA** | API Gateway | Toutes les requêtes API |
| **Gateway** | BFF | Proxy `/api/`, `/actuator/` |
| **BFF** | 7 services | Agrégation métier, JWT relay |
| **care-entry** | patient, catalog, stay | Vérifier patient/service ; ouvrir séjour à l'admission |
| **stay** | care-entry, patient | Valider admission ; afficher identité |
| **clinical-record** | patient, care-entry | Valider patient/admission avant consultation |
| **Tous services métier** | audit | `RestAuditEventPublisher` → `POST /api/v1/audit/events` |

## Stack Docker (`docker-compose.stack.yml`)

| Composant | Image / build | Port exposé |
|-----------|---------------|-------------|
| identity … audit | `Dockerfile.spring` par module | internes 8081–8087 |
| bff | afya-bff | interne 8080 |
| api | `infra/gateway` | **8090**, **8443** |
| web | `frontend` (Nginx) | **8088** |
| minio | minio/minio | **9000**, 9001 |
| *-db | postgres:16-alpine | interne |

## Observabilité

```mermaid
flowchart LR
  SVC[Services + BFF\nActuator]
  PROM[Prometheus]
  GRAF[Grafana]
  PROM -->|scrape /actuator/prometheus| SVC
  GRAF -->|PromQL| PROM
```

Métriques résilience : `afya.http.resilience.*` (retry, 5xx, circuit breaker).
