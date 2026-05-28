#!/usr/bin/env bash
# Stack Afya entièrement conteneurisée + TLS auto-signé sur la gateway.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl est requis pour les certificats TLS." >&2
  exit 1
fi

"${ROOT}/scripts/generate-tls-certs.sh"

# Limite les builds Maven parallèles (réseau + CPU) ; surcharge : COMPOSE_PARALLEL_LIMIT=4
export COMPOSE_PARALLEL_LIMIT="${COMPOSE_PARALLEL_LIMIT:-2}"

if [[ "${STACK_SKIP_PULL:-}" != "1" ]]; then
  echo "Pré-téléchargement des images de base (STACK_SKIP_PULL=1 pour ignorer)…"
  "${ROOT}/scripts/pull-stack-images.sh" || {
    echo "" >&2
    echo "Impossible de télécharger les images. Solutions :" >&2
    echo "  • Réessayer dans quelques minutes (erreur réseau Docker Hub fréquente)" >&2
    echo "  • Vérifier proxy / VPN / pare-feu" >&2
    echo "  • Dev sans stack : podman compose up -d && ./mvnw -pl identity-service,afya-bff spring-boot:run" >&2
    exit 1
  }
fi

echo "Construction et démarrage de la stack (1ère fois : long, contexte Maven allégé via .dockerignore)…"
if ! podman compose -f docker-compose.stack.yml up -d --build; then
  echo "" >&2
  echo "Échec podman compose (build ou démarrage)." >&2
  echo "  podman compose -f docker-compose.stack.yml logs --tail 50" >&2
  exit 1
fi

echo ""
echo "Attente des services (healthchecks)…"
if "${ROOT}/scripts/stack-wait.sh"; then
  echo ""
  echo "Points d'accès :"
  echo "  UI        : http://127.0.0.1:8088"
  echo "  API HTTP  : http://127.0.0.1:8090"
  echo "  API HTTPS : https://127.0.0.1:8443  (certificat auto-signé)"
  echo "  MinIO     : http://127.0.0.1:9001 (console) / 9000 (API)"
  echo ""
  echo "Smoke : GATEWAY_BASE=https://127.0.0.1:8443 ./scripts/smoke-api.sh"
else
  echo "" >&2
  echo "Stack partiellement démarrée — consultez les logs avant le smoke." >&2
  exit 1
fi

echo "Arrêt : podman compose -f docker-compose.stack.yml down"
