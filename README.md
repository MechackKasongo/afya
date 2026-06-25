# Afya — plateforme de suivi patient (multi-services)

Nouveau système d’information hospitalier orienté **services indépendants** (6–7 déployables), distinct du dépôt `afya-health-system` (monolithe modulaire).

## Documentation

| Document | Contenu |
|----------|---------|
| [docs/ARCHITECTURE_SERVICES.md](docs/ARCHITECTURE_SERVICES.md) | Découpage 7 services, schéma, dépendances |
| [docs/CARTOGRAPHIE_EXIGENCES.md](docs/CARTOGRAPHIE_EXIGENCES.md) | Cas d’utilisation → service responsable |
| [docs/EXIGENCES_NON_FONCTIONNELLES.md](docs/EXIGENCES_NON_FONCTIONNELLES.md) | Sécurité, disponibilité, accessibilité |
| [docs/DIAGRAMMES_UML.md](docs/DIAGRAMMES_UML.md) | Cas d’utilisation, composants, déploiement (Mermaid) |
| [docs/MERMAID_MEMOIRE_AFYA.md](docs/MERMAID_MEMOIRE_AFYA.md) | Mémoire : domaine, classes participantes, activités (Mermaid) |
| [docs/MERMAID_CLASSES_PAR_SERVICE.md](docs/MERMAID_CLASSES_PAR_SERVICE.md) | Diagrammes de classes par microservice (Mermaid) |
| [docs/MEMOIRE_UML_ANALYSE_FR.md](docs/MEMOIRE_UML_ANALYSE_FR.md) | Mémoire — UML phase analyse (FR, avant développement) |
| [docs/MEMOIRE_UML_ANALYSE_EN.md](docs/MEMOIRE_UML_ANALYSE_EN.md) | Thesis — UML analysis phase (EN, before development) |
| [docs/plantuml/](docs/plantuml/) | PlantUML : cas d’utilisation, composants, déploiement, **activités**, **conception**, domaine |
| [docs/MERMAID_COMPOSANTS_AFYA.md](docs/MERMAID_COMPOSANTS_AFYA.md) | Diagramme de composants (Mermaid) |
| [docs/plantuml/CONCEPTION_AFYA.puml](docs/plantuml/CONCEPTION_AFYA.puml) | Diagramme de conception (couches, BFF, shared) |
| [docs/MERMAID_CONCEPTION_AFYA.md](docs/MERMAID_CONCEPTION_AFYA.md) | Diagramme de conception (Mermaid) |
| [docs/MERMAID_DOMAINE_AFYA.md](docs/MERMAID_DOMAINE_AFYA.md) | Modèle du domaine avec attributs (Mermaid) |
| [docs/MODELE_DOMAINE_MEMOIRE_9_SERVICES.md](docs/MODELE_DOMAINE_MEMOIRE_9_SERVICES.md) | **Mémoire — modèle du domaine officiel (9 microservices)** |
| [docs/MAPPING_MODELE_ANALYSE_AFYA.md](docs/MAPPING_MODELE_ANALYSE_AFYA.md) | Mapping mémoire (9) → prototype Afya (7) |
| [docs/plantuml/memoire/](docs/plantuml/memoire/) | PlantUML MD-01…MD-09 (mémoire) |

## Services

**Mémoire (référence officielle — 9 microservices)** : voir [docs/MODELE_DOMAINE_MEMOIRE_9_SERVICES.md](docs/MODELE_DOMAINE_MEMOIRE_9_SERVICES.md) — Auth, User, Hospital, Patient, Admission, Medical, Lab, Nursing, Report.

**Prototype Afya (stack cible — 9 microservices + BFF + audit)** :
1. **auth-service** (8081) + **user-service** (8089) — Auth & Users (MD-01 / MD-02)
2. **hospital-service** (8082) — Hospital (MD-03)
3. **patient-service** (8083) — Patient (MD-04)
4. **admission-service** (8084) — Admission + séjour (MD-05)
5. **medical-service** (8085) — Medical (MD-06)
6. **nursing-service** (8093) — Nursing (MD-08)
7. **lab-service** (8092) — Lab (MD-07)
8. **report-service** (8094) — Report (MD-09)
9. **audit-service** (8087) — ingestion audit (transversal)

> Migration legacy terminée — voir [docs/MIGRATION_9_SERVICES.md](docs/MIGRATION_9_SERVICES.md). ~~`identity-service`~~ → auth + user ; ~~`catalog-service`~~ → `hospital-service` ; ~~`care-entry-service`~~ + ~~`stay-service`~~ → `admission-service` ; ~~`clinical-record-service`~~ → `medical-service` + `nursing-service`.

## Stack envisagée (à confirmer)

- Java 21, Spring Boot 4.x par service  
- API REST versionnée `/api/v1/`  
- BDD par service ; stockage objet pour images/PDF (medical-service)  
- **API Gateway** (Nginx, port **8090**) + **BFF** (agrégation, port **8080**) pour le front web responsive  

## Démarrage rapide

Voir **[docs/DEMARRAGE.md](docs/DEMARRAGE.md)** — dev Maven + `podman compose`, ou stack complète `./scripts/stack-up.sh` (UI **8088**, API **8090**/**8443**). Smoke : `./scripts/smoke-api.sh`.

Production (Kubernetes + Helm) : **[docs/DEPLOIEMENT_PROD.md](docs/DEPLOIEMENT_PROD.md)** — chart `infra/k8s/helm/afya/`, CD `.github/workflows/cd.yml`.

## Modules implémentés

| Module | Port | Statut |
|--------|------|--------|
| `afya-shared` | — | Bibliothèque partagée |
| `auth-service` | 8081 | Auth JWT (MD-01) |
| `user-service` | 8089 | Utilisateurs & rôles (MD-02) |
| `hospital-service` | 8082 | Organisation hospitalière (MD-03) |
| `patient-service` | 8083 | Registre patient (MD-04) |
| `admission-service` | 8084 | Admissions, urgences, séjour (MD-05) |
| `medical-service` | 8085 | Dossier médical, consultations (MD-06) |
| `nursing-service` | 8093 | Soins, constantes, notifications (MD-08) |
| `lab-service` | 8092 | Laboratoire (MD-07) |
| `report-service` | 8094 | Rapports (MD-09) |
| `audit-service` | 8087 | Journal d'audit |
| `afya-bff` | 8080 | BFF |
| `infra/gateway` (`api`) | 8090 / 8443 | API Gateway |
| `frontend/` | 5173 (dev) | React + Vite |

## Front + stack API

```bash
# Démarrer les services nécessaires (voir docs/DEMARRAGE.md ou ./scripts/dev-print-terminals.sh)
./mvnw -pl afya-bff spring-boot:run
podman compose up -d api
cd frontend && npm install && npm run dev
./scripts/smoke-api.sh
```

Détails : **[docs/DEMARRAGE.md](docs/DEMARRAGE.md)** §10–12.
