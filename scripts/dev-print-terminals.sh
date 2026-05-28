#!/usr/bin/env bash
# Affiche la liste des terminaux à ouvrir (un microservice par terminal).
# Copier-coller chaque bloc dans un terminal séparé, dans l'ordre indiqué.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cat <<EOF
Afya — un terminal par service (depuis ${ROOT})

Terminal 0 — Infra seulement (Postgres, MinIO)
  cd ${ROOT}
  podman compose up -d

  (La gateway port 8090 : lancer après le BFF → voir Terminal 8b)

Terminal 1 — identity-service (8081)
  cd ${ROOT}
  ./mvnw -pl identity-service spring-boot:run

Terminal 2 — catalog-service (8082)
  cd ${ROOT}
  ./mvnw -pl catalog-service spring-boot:run

Terminal 3 — patient-service (8083)
  cd ${ROOT}
  ./mvnw -pl patient-service spring-boot:run

Terminal 4 — stay-service (8085)
  cd ${ROOT}
  ./mvnw -pl stay-service spring-boot:run

Terminal 5 — care-entry-service (8084) — après patient + catalog
  cd ${ROOT}
  ./mvnw -pl care-entry-service spring-boot:run

Terminal 6 — clinical-record-service (8086)
  cd ${ROOT}
  ./mvnw -pl clinical-record-service spring-boot:run

Terminal 7 — audit-service (8087)
  cd ${ROOT}
  ./mvnw -pl audit-service spring-boot:run

Terminal 8 — afya-bff (8080) — après les microservices
  cd ${ROOT}
  ./mvnw -pl afya-bff spring-boot:run

Terminal 8b — Gateway (8090) — une fois le BFF démarré
  cd ${ROOT}
  podman compose up -d api

Terminal 9 — frontend Vite (5173)
  cd ${ROOT}/frontend
  npm run dev

Vérification :
  ./scripts/check-ports.sh
  ./scripts/smoke-api.sh

Login : admin / Admin@Afya2026!  →  http://localhost:5173

Alternative tmux (fenêtres au lieu de terminaux séparés) :
  ./scripts/dev-tmux.sh
EOF
