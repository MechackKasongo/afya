#!/usr/bin/env bash
# Affiche la liste des terminaux à ouvrir (stack cible 9 microservices).

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cat <<EOF
Afya — stack cible (9 services) — un terminal par service (depuis ${ROOT})

Terminal 0 — Infra (Postgres, MinIO)
  cd ${ROOT}
  podman compose up -d

Terminal 1 — auth-service (8081)
  cd ${ROOT} && ./mvnw -pl auth-service spring-boot:run

Terminal 2 — user-service (8089)
  cd ${ROOT} && ./mvnw -pl user-service spring-boot:run

Terminal 3 — hospital-service (8082)
  cd ${ROOT} && ./mvnw -pl hospital-service spring-boot:run

Terminal 4 — patient-service (8083)
  cd ${ROOT} && ./mvnw -pl patient-service spring-boot:run

Terminal 5 — admission-service (8084) — après patient + hospital
  cd ${ROOT} && ./mvnw -pl admission-service spring-boot:run

Terminal 6 — medical-service (8085)
  cd ${ROOT} && ./mvnw -pl medical-service spring-boot:run

Terminal 7 — nursing-service (8093)
  cd ${ROOT} && ./mvnw -pl nursing-service spring-boot:run

Terminal 8 — lab-service (8092)
  cd ${ROOT} && ./mvnw -pl lab-service spring-boot:run

Terminal 9 — report-service (8094)
  cd ${ROOT} && ./mvnw -pl report-service spring-boot:run

Terminal 10 — audit-service (8087)
  cd ${ROOT} && ./mvnw -pl audit-service spring-boot:run

Terminal 11 — afya-bff (8080)
  cd ${ROOT} && ./mvnw -pl afya-bff spring-boot:run

Terminal 12 — Gateway (8090) — après le BFF
  cd ${ROOT} && podman compose up -d api

Terminal 13 — frontend Vite (5173)
  cd ${ROOT}/frontend && npm run dev

Vérification :
  ./scripts/check-ports.sh
  ./scripts/smoke-api.sh

Alternative tmux : ./scripts/dev-tmux.sh
Stack conteneurisée : ./scripts/stack-up.sh
EOF
