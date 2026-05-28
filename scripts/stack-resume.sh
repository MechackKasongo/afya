#!/usr/bin/env bash
# Reprend une stack interrompue (ex. build clinical ou bff raté par réseau Maven).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

export COMPOSE_PARALLEL_LIMIT="${COMPOSE_PARALLEL_LIMIT:-1}"

SERVICES="${STACK_SERVICES:-identity catalog patient audit care-entry stay clinical bff api web}"
echo "Build : ${SERVICES}"
podman compose -f docker-compose.stack.yml build ${SERVICES}

echo "Démarrage / mise à jour des conteneurs…"
if ! podman compose -f docker-compose.stack.yml up -d; then
  echo "Premier « up » échoué (souvent une course Podman au healthcheck) — nouvel essai dans 15s…" >&2
  sleep 15
  podman compose -f docker-compose.stack.yml up -d
fi

# Après recréation du BFF, nginx peut garder un upstream invalide (502) jusqu’au reload.
if podman ps --format '{{.Names}}' 2>/dev/null | grep -qx 'afya-api-1'; then
  podman exec afya-api-1 nginx -s reload 2>/dev/null || true
fi

"${ROOT}/scripts/stack-wait.sh"

echo ""
echo "Smoke : GATEWAY_BASE=https://127.0.0.1:8443 ./scripts/smoke-api.sh"
