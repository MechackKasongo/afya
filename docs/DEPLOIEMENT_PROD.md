# Déploiement production — plateforme Afya

Guide opérationnel pour déployer Afya sur Kubernetes avec PostgreSQL managé, object store S3/MinIO et observabilité Prometheus/Grafana.

**Artefacts du projet :**

| Artefact | Chemin |
|----------|--------|
| Profil Spring `prod` | `*/src/main/resources/application-prod.properties` |
| Chart Helm | `infra/k8s/helm/afya/` |
| Pipeline CD (images GHCR) | `.github/workflows/cd.yml` |
| Alertes Prometheus | `docs/PROMETHEUS_ALERTS_RESILIENCE_AFYA.yml` |
| Dashboard Grafana | `docs/GRAFANA_DASHBOARD_RESILIENCE_AFYA.json` |
| Runbook incidents | `docs/RUNBOOK_RESILIENCE_AFYA.md` |

---

## 1. Architecture cible (9 microservices)

```
Internet → Ingress (TLS) → web (SPA) + api (gateway Nginx)
                              ↓
                            bff → 9 microservices métier → PostgreSQL (10 bases)
                            medical → S3 / MinIO (documents cliniques)
Prometheus ← scrape /actuator/prometheus
```

Composants déployés par le chart Helm (`infra/k8s/helm/afya/`) :

| Pod | Image GHCR | Port | Base PostgreSQL |
|-----|------------|------|-----------------|
| `auth` | `afya-auth` | 8081 | `afya_auth` |
| `user` | `afya-user` | 8089 | `afya_user` |
| `hospital` | `afya-hospital` | 8082 | `afya_hospital` |
| `patient` | `afya-patient` | 8083 | `afya_patient` |
| `admission` | `afya-admission` | 8084 | `afya_admission` |
| `medical` | `afya-medical` | 8085 | `afya_medical` |
| `nursing` | `afya-nursing` | 8093 | `afya_nursing` |
| `lab` | `afya-lab` | 8092 | `afya_lab` |
| `report` | `afya-report` | 8094 | `afya_report` |
| `audit` | `afya-audit` | 8087 | `afya_audit` |
| `bff` | `afya-bff` | 8080 | — |
| `api` | `afya-gateway` | 80 | — |
| `web` | `afya-web` | 80 | — |

---

## 2. Prérequis infrastructure

### Cluster Kubernetes

- Version ≥ 1.28
- Ingress controller (ingress-nginx recommandé)
- [cert-manager](https://cert-manager.io/) pour TLS automatique
- (Optionnel) [Prometheus Operator](https://prometheus-operator.dev/) pour les `ServiceMonitor`

### Données

- **PostgreSQL 16** managé avec **10 bases** (une par service persistant) :
  - `afya_auth`, `afya_user`, `afya_hospital`, `afya_patient`, `afya_admission`, `afya_medical`, `afya_nursing`, `afya_lab`, `afya_report`, `afya_audit`
- Utilisateur dédié par base (principe du moindre privilège)
- Sauvegardes automatiques + test de restauration
- TLS activé (`sslmode=require` — configurable via `DB_SSLMODE`)

### Object store

- Bucket S3 (ou MinIO HA) pour `medical-service`
- Variables : `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET`

---

## 3. Construction et publication des images

### Via GitHub Actions (recommandé)

1. Pousser un tag semver : `git tag v1.0.0 && git push origin v1.0.0`
2. Le workflow **CD** (`.github/workflows/cd.yml`) publie **13 images** sur `ghcr.io/<org>/` :
   - `afya-auth`, `afya-user`, `afya-hospital`, `afya-patient`, `afya-admission`, `afya-medical`, `afya-nursing`, `afya-lab`, `afya-report`, `afya-audit`, `afya-bff`, `afya-gateway`, `afya-web`

Déclenchement manuel :

```bash
gh workflow run cd.yml -f image_tag=v1.0.0-rc1
```

### En local

```bash
export REGISTRY=ghcr.io/VOTRE_ORG
export TAG=v1.0.0

for module in auth-service user-service hospital-service patient-service admission-service medical-service nursing-service lab-service report-service audit-service afya-bff; do
  image="afya-${module%-service}"
  image="${image/afya-afya-/afya-}"
  docker build -f infra/docker/Dockerfile.spring --build-arg MAVEN_MODULE="$module" \
    -t "${REGISTRY}/${image}:${TAG}" .
  docker push "${REGISTRY}/${image}:${TAG}"
done

docker build -t "${REGISTRY}/afya-gateway:${TAG}" infra/gateway && docker push "${REGISTRY}/afya-gateway:${TAG}"
docker build -t "${REGISTRY}/afya-web:${TAG}" frontend && docker push "${REGISTRY}/afya-web:${TAG}"
```

---

## 4. Secrets production

**Ne jamais** commiter de secrets dans Git. Le validateur `ProductionSecretsValidator` refuse au démarrage :

- `JWT_ACCESS_SECRET` / `JWT_REFRESH_SECRET` avec valeurs dev
- `AUDIT_INGESTION_KEY` = `dev-audit-ingestion-key`
- `app.bootstrap.password` = `Admin@Afya2026!`
- `app.bootstrap.auto-provision=true`

### Créer le Secret Kubernetes

```bash
kubectl create namespace afya-prod

kubectl -n afya-prod create secret generic afya-secrets \
  --from-literal=jwt-access-secret="$(openssl rand -base64 64)" \
  --from-literal=jwt-refresh-secret="$(openssl rand -base64 64)" \
  --from-literal=audit-ingestion-key="$(openssl rand -base64 32)" \
  --from-literal=minio-access-key="VOTRE_ACCESS_KEY" \
  --from-literal=minio-secret-key="VOTRE_SECRET_KEY" \
  --from-literal=auth-db-password="..." \
  --from-literal=user-db-password="..." \
  --from-literal=hospital-db-password="..." \
  --from-literal=patient-db-password="..." \
  --from-literal=admission-db-password="..." \
  --from-literal=medical-db-password="..." \
  --from-literal=nursing-db-password="..." \
  --from-literal=lab-db-password="..." \
  --from-literal=report-db-password="..." \
  --from-literal=audit-db-password="..."
```

Voir aussi `infra/k8s/helm/afya/README.md` pour la liste complète des clés attendues par le chart.

En production, préférer **External Secrets Operator**, Vault ou le gestionnaire de secrets du cloud.

### Compte administrateur initial

`app.bootstrap.auto-provision=false` en profil `prod`. Créer le premier admin :

- via procédure SQL/Flyway seed contrôlée, ou
- déploiement temporaire avec auto-provision sur un environnement staging isolé, puis export contrôlé.

---

## 5. Déploiement Helm

### Staging

```bash
helm upgrade --install afya-staging infra/k8s/helm/afya \
  -f infra/k8s/helm/afya/values-staging.yaml \
  --namespace afya-staging --create-namespace \
  --set global.imageRegistry=ghcr.io/VOTRE_ORG \
  --set global.imageTag=v1.0.0-rc1 \
  --set databases.auth.host=afya-staging.xxxx.rds.amazonaws.com \
  --set databases.user.host=afya-staging.xxxx.rds.amazonaws.com \
  --set databases.hospital.host=afya-staging.xxxx.rds.amazonaws.com \
  --set databases.patient.host=afya-staging.xxxx.rds.amazonaws.com \
  --set databases.admission.host=afya-staging.xxxx.rds.amazonaws.com \
  --set databases.medical.host=afya-staging.xxxx.rds.amazonaws.com \
  --set databases.nursing.host=afya-staging.xxxx.rds.amazonaws.com \
  --set databases.lab.host=afya-staging.xxxx.rds.amazonaws.com \
  --set databases.report.host=afya-staging.xxxx.rds.amazonaws.com \
  --set databases.audit.host=afya-staging.xxxx.rds.amazonaws.com \
  --set objectStore.endpoint=https://s3.eu-west-1.amazonaws.com \
  --set ingress.host=staging.afya.example.com \
  --set cors.allowedOrigins=https://staging.afya.example.com \
  --wait --timeout 15m
```

### Production

```bash
helm upgrade --install afya infra/k8s/helm/afya \
  -f infra/k8s/helm/afya/values-prod.yaml \
  --namespace afya-prod --create-namespace \
  --set global.imageRegistry=ghcr.io/VOTRE_ORG \
  --set global.imageTag=v1.0.0 \
  --set databases.auth.host=afya-prod.xxxx.rds.amazonaws.com \
  --set databases.user.host=afya-prod.xxxx.rds.amazonaws.com \
  --set databases.hospital.host=afya-prod.xxxx.rds.amazonaws.com \
  --set databases.patient.host=afya-prod.xxxx.rds.amazonaws.com \
  --set databases.admission.host=afya-prod.xxxx.rds.amazonaws.com \
  --set databases.medical.host=afya-prod.xxxx.rds.amazonaws.com \
  --set databases.nursing.host=afya-prod.xxxx.rds.amazonaws.com \
  --set databases.lab.host=afya-prod.xxxx.rds.amazonaws.com \
  --set databases.report.host=afya-prod.xxxx.rds.amazonaws.com \
  --set databases.audit.host=afya-prod.xxxx.rds.amazonaws.com \
  --set ingress.host=afya.example.com \
  --set cors.allowedOrigins=https://afya.example.com \
  --wait --timeout 20m
```

### TLS (cert-manager)

```yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: afya-prod-tls
  namespace: afya-prod
spec:
  secretName: afya-prod-tls
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
    - afya.example.com
```

---

## 6. Vérifications post-déploiement

```bash
# Santé des pods
kubectl -n afya-prod get pods

# Gateway (via port-forward ou URL publique)
curl -sf https://afya.example.com/gateway/health

# Smoke API (adapter GATEWAY_BASE et identifiants admin prod)
GATEWAY_BASE=https://afya.example.com \
ADMIN_USER=admin \
ADMIN_PASS='...' \
./scripts/smoke-api.sh

# Smoke minimal (health + login uniquement)
GATEWAY_BASE=https://afya.example.com SMOKE_EXTENDED=0 ./scripts/smoke-api.sh

# Métriques Prometheus (depuis le cluster)
kubectl -n afya-prod port-forward svc/bff 8080:8080
curl -s http://127.0.0.1:8080/actuator/prometheus | head
```

**Ne pas exposer** `/actuator` publiquement — l'Ingress route uniquement `/api` vers la gateway et `/` vers le web.

---

## 7. Observabilité

1. Installer Prometheus + Grafana (kube-prometheus-stack ou équivalent)
2. Activer les ServiceMonitor : `serviceMonitor.enabled: true` dans `values-prod.yaml`
3. Importer `docs/GRAFANA_DASHBOARD_RESILIENCE_AFYA.json`
4. Charger `docs/PROMETHEUS_ALERTS_RESILIENCE_AFYA.yml` dans Prometheus/Alertmanager
5. Documenter l'astreinte avec `docs/RUNBOOK_RESILIENCE_AFYA.md`

---

## 8. CI/CD continu (optionnel)

Pour déployer automatiquement en staging depuis GitHub Actions :

1. Créer l'environnement GitHub **staging**
2. Ajouter le secret `KUBE_CONFIG_DATA` (kubeconfig encodé base64)
3. Lancer le workflow CD en `workflow_dispatch`

Pour la production : déploiement **manuel** ou via tag protégé + approbation GitHub Environment.

---

## 9. Checklist go-live

- [ ] Tag semver poussé (`v*`) ou `gh workflow run cd.yml` — **13 images** sur GHCR
- [ ] Secret `afya-secrets` créé (aucune valeur dev, voir §4)
- [ ] **10 bases** PostgreSQL provisionnées + Flyway OK au premier démarrage
- [ ] Bucket S3/MinIO créé, credentials dans le Secret
- [ ] Ingress + TLS valides (`cert-manager` ou certificat manuel)
- [ ] `CORS_ALLOWED_ORIGINS` = domaine prod uniquement
- [ ] `app.bootstrap.auto-provision=false` (profil `prod`)
- [ ] Compte admin + credential auth créés manuellement
- [ ] `./scripts/smoke-api.sh` OK en prod (`SMOKE_EXTENDED=1`)
- [ ] Dashboard Grafana + alertes Prometheus actives
- [ ] Sauvegarde/restauration BDD testée (au moins `afya_auth`, `afya_admission`, `afya_medical`)
- [ ] `/actuator` non accessible depuis Internet (routage Ingress `/api` + `/` uniquement)

---

## 10. Dépannage

| Symptôme | Action |
|----------|--------|
| Pod `CrashLoopBackOff` au démarrage | Logs : `kubectl logs -n afya-prod deploy/auth` — souvent secret JWT manquant ou valeur dev |
| `IllegalStateException` sécurité | Secret avec valeur par défaut interdite — regénérer |
| Erreur JDBC SSL | Ajuster `DB_SSLMODE` (`require`, `verify-full`) selon le fournisseur |
| 502 gateway → BFF | Vérifier que `bff` est Ready ; `BFF_UPSTREAM=http://bff:8080` |
| Flyway échec migration | Vérifier droits DDL sur la base ; restaurer backup si nécessaire |
| Smoke prescriptions échoue | Vérifier `admission`, `medical`, `nursing`, `hospital`, `patient` Ready ; lit libre sur le service seed |

Ordre de redéploiement en cas de cascade : `audit` → `auth`/`user` → `hospital`/`patient` → `admission` → `medical`/`nursing`/`lab`/`report` → `bff` → `api` → `web`.

---

## 11. Références

- Stack locale de référence : `./scripts/stack-up.sh` (`docker-compose.stack.yml`)
- Diagramme : `docs/plantuml/DEPLOIEMENT_AFYA.puml`
- Chart Helm : `infra/k8s/helm/afya/README.md`
