# Migration — 7 services → 9 microservices (mémoire HGR)

**Référence domaine :** [MODELE_DOMAINE_MEMOIRE_9_SERVICES.md](MODELE_DOMAINE_MEMOIRE_9_SERVICES.md)

## Cible — 9 microservices

| # | Service mémoire | Module Maven | Port | Base PostgreSQL |
|---|-----------------|--------------|------|-----------------|
| MD-01 | Auth Service | `auth-service` | 8081 | `afya_auth` |
| MD-02 | User Service | `user-service` | 8089 | `afya_user` |
| MD-03 | Hospital Service | `hospital-service` | 8082 | `afya_hospital` |
| MD-04 | Patient Service | `patient-service` | 8083 | `afya_patient` |
| MD-05 | Admission Service | `admission-service` | 8084 | `afya_admission` |
| MD-06 | Medical Service | `medical-service` | 8085 | `afya_medical` |
| MD-07 | Lab Service | `lab-service` | 8092 | `afya_lab` |
| MD-08 | Nursing Service | `nursing-service` | 8093 | `afya_nursing` |
| MD-09 | Report Service | `report-service` | 8094 | `afya_report` |

**Transversal :** `afya-bff` (8080), `infra/gateway` (8090), `frontend` (8088).

## Correspondance modules actuels → cible

| Module actuel | Devient | Action |
|---------------|---------|--------|
| `identity-service` | `auth-service` + `user-service` | Scinder Auth / Users — **supprimé** |
| `catalog-service` | `hospital-service` | Renommer (MD-03) — **supprimé** |
| `patient-service` | `patient-service` | Conserver + MD-04 (antécédents, contacts) |
| `care-entry-service` + `stay-service` | `admission-service` | Fusionner (MD-05) — **supprimés** |
| `clinical-record-service` | `medical-service` + `nursing-service` | Scinder (MD-06 / MD-08) — **supprimé** |
| *(absent)* | `lab-service` | **Créer** (MD-07) |
| `audit-service` | `audit-service` (ingestion) + `report-service` (rapports) | Scinder MD-09 |

## Phases d'implémentation

### Phase 1 — Nouveaux services (en cours)
- [x] `lab-service` — MD-07 (port **8092**)
- [x] `admission-service` — MD-05 fusion `care-entry` + `stay` (port **8084**, base `afya_admission`)
- [x] `report-service` — MD-09 scission rapports depuis audit (port **8094**, base `afya_report`)

### Phase 2 — Scissions
- [x] `auth-service` + `user-service` depuis identity (ports **8081** / **8089**, bases `afya_auth` / `afya_user`)
- [x] `medical-service` + `nursing-service` depuis clinical-record (ports **8085** / **8093**)
- [x] `hospital-service` — renommage catalog (port **8082**, base `afya_hospital`)

### Phase 3 — Intégration
- [x] Mettre à jour `afya-bff` (clients REST : `AdmissionClient`, `AdmissionStayClient`, `admission-base-url`)
- [x] `docker-compose.stack.yml` — stack cible 9 services
- [x] Chart Helm `infra/k8s/helm/afya/` — services & bases cible
- [x] CI `.github/workflows/cd.yml` — images cible
- [x] Scripts `dev-print-terminals.sh`, `dev-tmux.sh`, `stack-up.sh`
### Phase 4 — Suppression modules legacy
- [x] `identity-service` (remplacé par auth + user)
- [x] `catalog-service` (remplacé par hospital-service)
- [x] `care-entry-service` + `stay-service` (remplacés par admission-service)
- [x] `clinical-record-service` (remplacé par medical + nursing)

### Enrichissement domaine mémoire
- [x] Patient : `AntecedentMedical`, `ContactUrgence` (MD-04)
- [x] Admission : `NotificationAdmission`, entité `Sortie` (MD-05)
- [x] Nursing : `ConstanteVitale`, `AlerteConstante` (MD-08)
- [x] Nursing : `NotificationPrescription`
- [x] Hospital : historique `OccupationLit` (occupations lit)
- [x] Report : `Rapport` PDF/Excel, statistiques labo/soins

## Ordre de démarrage (runtime)

```
report → auth → user → hospital → patient → admission → medical → nursing → lab → bff → gateway → web
```

## Commandes dev (cible)

```bash
# Phase 1 partielle — lab + modules existants
./mvnw -pl lab-service spring-boot:run          # 8092
./mvnw -pl auth-service,user-service spring-boot:run
# …
```

---

*Document de suivi migration — mettre à jour les cases à cocher à chaque phase.*
