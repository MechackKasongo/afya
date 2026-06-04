# Diagramme de conception — plateforme Afya

Vue **architecture logicielle** (couches, BFF, transversal, référence care-entry).  
PlantUML : [plantuml/CONCEPTION_AFYA.puml](plantuml/CONCEPTION_AFYA.puml)

Diagrammes complémentaires :

| Fichier | Contenu |
|---------|---------|
| [CONCEPTION_COUCHE_SERVICE_AFYA.puml](plantuml/CONCEPTION_COUCHE_SERVICE_AFYA.puml) | Classes care-entry (détail) |
| [CONCEPTION_CONSULTATION_AFYA.puml](plantuml/CONCEPTION_CONSULTATION_AFYA.puml) | Classes consultation / maladies |
| [CONCEPTION_SEQUENCE_ADMISSION_AFYA.puml](plantuml/CONCEPTION_SEQUENCE_ADMISSION_AFYA.puml) | Séquence admission |
| [CONCEPTION_SEQUENCE_AUTHENTIFICATION_AFYA.puml](plantuml/CONCEPTION_SEQUENCE_AUTHENTIFICATION_AFYA.puml) | Séquence login |
| [CONCEPTION_SEQUENCE_CLINICAL_AFYA.puml](plantuml/CONCEPTION_SEQUENCE_CLINICAL_AFYA.puml) | Séquence prescription |
| [CONCEPTION_SEQUENCE_PRISE_EN_CHARGE_AFYA.puml](plantuml/CONCEPTION_SEQUENCE_PRISE_EN_CHARGE_AFYA.puml) | Séquence consultation |

## Patron en couches (microservice)

```mermaid
flowchart TB
  subgraph presentation["Présentation"]
    CTRL[Controllers REST\n/api/v1]
  end

  subgraph application["Application"]
    SVC[Services @Transactional]
    WRITER[Writers optionnels]
  end

  subgraph integration["Intégration"]
    CLIENT[*ServiceClient\nRestClient]
  end

  subgraph persistence["Persistance"]
    REPO[Repositories JPA]
    ENT[Entities + enums]
  end

  subgraph transfer["Transfert"]
    DTO[Records Request/Response]
  end

  subgraph shared["afya-shared"]
    JWT[JwtAuthenticationFilter]
    CORR[CorrelationIdFilter]
    RES[HttpResilienceInterceptor]
    AUD[AuditEventPublisher]
    ERR[GlobalExceptionHandler]
  end

  CTRL --> SVC
  SVC --> REPO
  SVC --> CLIENT
  CTRL -.-> DTO
  SVC -.-> DTO
  REPO --> ENT
  SVC --> shared
  CLIENT --> shared
```

## afya-bff (agrégation)

```mermaid
flowchart LR
  subgraph bff["afya-bff"]
    BC[BffControllers x16]
    BCL[Clients HTTP x7]
    FACT[DownstreamRestClientFactory]
    SUP[PlatformReportService\nAdmissionUiEnricher]
  end

  BC --> BCL
  BC --> SUP
  BCL --> FACT
  FACT --> RES[HttpResilienceInterceptor]

  BCL --> ID[identity]
  BCL --> CAT[catalog]
  BCL --> PAT[patient]
  BCL --> CE[care-entry]
  BCL --> ST[stay]
  BCL --> CR[clinical]
  BCL --> AUD[audit]
```

## Référence care-entry — classes clés

```mermaid
classDiagram
  class AdmissionController {
    +create()
    +transfer()
    +discharge()
  }
  class AdmissionService {
    +admit()
    +transfer()
    +discharge()
  }
  class PatientServiceClient
  class CatalogServiceClient
  class StayServiceClient
  class AdmissionRepository
  class Admission
  class AuditEventPublisher

  AdmissionController --> AdmissionService
  AdmissionService --> AdmissionRepository
  AdmissionService --> PatientServiceClient
  AdmissionService --> CatalogServiceClient
  AdmissionService --> StayServiceClient
  AdmissionService --> AuditEventPublisher
  AdmissionRepository --> Admission
```

## Règles de conception

1. **Une base par service** — pas de JOIN inter-bases ; IDs logiques (`patientId`, `admissionId`).
2. **Transaction locale** — `@Transactional` dans le service du microservice concerné.
3. **Orchestration HTTP** — validation cross-service via `*ServiceClient` + résilience.
4. **Audit** — `RestAuditEventPublisher` vers audit-service après action réussie.
5. **Sécurité** — JWT validé dans chaque service ; `HospitalScopeSupport` pour le périmètre.
6. **BFF** — pas d’entités JPA ; proxy et agrégation pour le SPA uniquement.
