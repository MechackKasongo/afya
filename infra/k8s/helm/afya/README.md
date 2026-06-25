# Chart Helm — Afya

Déploie les microservices Spring, le BFF, la gateway Nginx et le frontend React.

## Prérequis

- Cluster Kubernetes 1.28+
- Ingress controller (ex. ingress-nginx)
- cert-manager (TLS Let's Encrypt)
- PostgreSQL managé (10 bases : auth, user, hospital, patient, admission, medical, nursing, lab, report, audit) + object store S3/MinIO
- Secret `afya-secrets` (ou `secrets.create: true` hors prod)

## Installation rapide

```bash
# 1. Créer le Secret (recommandé en prod)
kubectl create namespace afya-prod
kubectl -n afya-prod create secret generic afya-secrets \
  --from-literal=jwt-access-secret='...' \
  --from-literal=jwt-refresh-secret='...' \
  --from-literal=audit-ingestion-key='...' \
  --from-literal=minio-access-key='...' \
  --from-literal=minio-secret-key='...' \
  --from-literal=audit-db-password='...' \
  --from-literal=auth-db-password='...' \
  --from-literal=user-db-password='...' \
  --from-literal=hospital-db-password='...' \
  --from-literal=patient-db-password='...' \
  --from-literal=admission-db-password='...' \
  --from-literal=medical-db-password='...' \
  --from-literal=nursing-db-password='...' \
  --from-literal=lab-db-password='...' \
  --from-literal=report-db-password='...'

# 2. Déployer
helm upgrade --install afya ./infra/k8s/helm/afya \
  -f infra/k8s/helm/afya/values-prod.yaml \
  --set global.imageRegistry=ghcr.io/VOTRE_ORG \
  --set global.imageTag=v1.0.0 \
  --set databases.auth.host=afya-db.example.com \
  --set databases.user.host=afya-db.example.com \
  --set databases.hospital.host=afya-db.example.com \
  --set databases.patient.host=afya-db.example.com \
  --set databases.admission.host=afya-db.example.com \
  --set databases.medical.host=afya-db.example.com \
  --set databases.nursing.host=afya-db.example.com \
  --set databases.lab.host=afya-db.example.com \
  --set databases.report.host=afya-db.example.com \
  --set databases.audit.host=afya-db.example.com \
  --set objectStore.endpoint=https://s3.eu-west-1.amazonaws.com \
  --set ingress.host=afya.example.com \
  --set cors.allowedOrigins=https://afya.example.com
```

## Ordre de démarrage

Le chart déploie tout en parallèle. En cas d'échec en cascade, redéployer dans cet ordre : `audit` → `auth`/`user` → `hospital`/`patient` → `admission` → `medical`/`nursing`/`lab`/`report` → `bff` → `api` → `web`.

## Documentation complète

Voir [docs/DEPLOIEMENT_PROD.md](../../../../docs/DEPLOIEMENT_PROD.md).
