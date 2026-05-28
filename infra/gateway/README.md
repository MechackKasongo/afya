# Afya API Gateway (phase E)

Nginx léger devant le **BFF** uniquement. Aucune agrégation métier.

## Dev local (BFF sur l'hôte, port 8080)

```bash
./scripts/generate-tls-certs.sh   # optionnel — active HTTPS 8443
./mvnw -pl afya-bff spring-boot:run
podman compose up -d --build api
curl http://127.0.0.1:8090/gateway/health
curl -k https://127.0.0.1:8443/gateway/health   # si certificats présents
```

## Stack conteneurisée

```bash
./scripts/stack-up.sh
```

Dans `docker-compose.stack.yml`, `BFF_UPSTREAM=http://bff:8080` et les certificats sont montés sur `/etc/nginx/certs`.

## Variables

| Variable | Défaut | Description |
|----------|--------|-------------|
| `BFF_UPSTREAM` | `http://host.docker.internal:8080` (dev) / `http://bff:8080` (stack) | URL de base du BFF |
| `TLS_CERT_PATH` | `/etc/nginx/certs/server.crt` | Certificat (si fichier présent → écoute 443) |
| `TLS_KEY_PATH` | `/etc/nginx/certs/server.key` | Clé privée |

Génération des certificats auto-signés : `scripts/generate-tls-certs.sh` → `infra/gateway/certs/` (ignorés par git).

## Fichiers

- `nginx.conf.template` — HTTP (80), rate limit, proxy `/api/` et `/actuator/`
- `nginx.tls-server.conf.template` — bloc HTTPS (443), ajouté par l'entrypoint si certificats montés
- `docker-entrypoint.sh` — `envsubst` + assemblage TLS conditionnel
