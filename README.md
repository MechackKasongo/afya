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

## Services (cible)

1. **identity-service** — authentification, comptes, rôles, périmètres  
2. **catalog-service** — départements et services hospitaliers  
3. **patient-service** — référentiel patient, recherche  
4. **care-entry-service** — urgences, admissions, transferts administratifs  
5. **stay-service** — séjour, fiche d’hospitalisation  
6. **clinical-record-service** — dossier médical, prescriptions, soins, documents/images  
7. **audit-service** — traces et activité système (rapports admin)

Option **6 services** : fusionner **care-entry** + **stay** en **encounter-stay-service**.

## Stack envisagée (à confirmer)

- Java 21, Spring Boot 4.x par service  
- API REST versionnée `/api/v1/`  
- BDD par service ; stockage objet pour images/PDF (clinical-record)  
- **API Gateway** (Nginx, port **8090**) + **BFF** (agrégation, port **8080**) pour le front web responsive  

## Démarrage rapide

Voir **[docs/DEMARRAGE.md](docs/DEMARRAGE.md)** — dev Maven + `podman compose`, ou stack complète `./scripts/stack-up.sh` (UI **8088**, API **8090**/**8443**). Smoke : `./scripts/smoke-api.sh`.

## Modules implémentés

| Module | Port | Statut |
|--------|------|--------|
| `afya-shared` | — | Exceptions + handler HTTP partagés |
| `identity-service` | 8081 | Auth JWT (login, refresh, logout, me) |
| `catalog-service` | 8082 | Départements, services hospitaliers, lits |
| `patient-service` | 8083 | Registre patient, recherche |
| `care-entry-service` | 8084 | Admissions, urgences, transferts |
| `stay-service` | 8085 | Séjour, fiche d'hospitalisation |
| `clinical-record-service` | 8086 | Dossier médical, prescriptions, soins, documents |
| `audit-service` | 8087 | Journal d'audit, rapports d'activité (admin) |
| `afya-bff` | 8080 | BFF : agrégation métier (interne à la gateway) |
| `infra/gateway` (`api`) | 8090 / 8443 | API Gateway : HTTP + TLS auto-signé |
| `frontend/` | 5173 (dev) | React + Vite, proxy `/api` → gateway **8090** |

## Front + stack API

```bash
./mvnw -pl afya-bff spring-boot:run   # après identity, patient, catalog, care-entry, stay, clinical-record, audit
podman compose up -d api              # gateway → BFF
cd frontend && npm install && npm run dev
./scripts/smoke-api.sh
```

Détails : **[docs/DEMARRAGE.md](docs/DEMARRAGE.md)** §10–12.
