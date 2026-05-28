#!/usr/bin/env bash
# Attend que la gateway et le BFF (stack) répondent avant le smoke.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

GATEWAY_HTTP="${GATEWAY_HTTP:-http://127.0.0.1:8090}"
GATEWAY_HTTPS="${GATEWAY_HTTPS:-https://127.0.0.1:8443}"
MAX_WAIT="${STACK_WAIT_SECONDS:-900}"
INTERVAL=10

elapsed=0
echo "Attente santé stack (max ${MAX_WAIT}s)…"

while (( elapsed < MAX_WAIT )); do
  gw=$(curl -s -o /dev/null -w "%{http_code}" "${GATEWAY_HTTP}/gateway/health" 2>/dev/null || echo "000")
  bff=$(curl -k -s -o /dev/null -w "%{http_code}" "${GATEWAY_HTTPS}/actuator/health" 2>/dev/null || echo "000")
  if [[ "${gw}" == "200" && "${bff}" == "200" ]]; then
    echo "Stack prête (gateway + BFF via ${GATEWAY_HTTPS})."
    exit 0
  fi
  echo "  ${elapsed}s — gateway=${gw} bff-via-gateway=${bff} (building/starting…)"
  sleep "${INTERVAL}"
  elapsed=$((elapsed + INTERVAL))
done

echo "Timeout : la stack n'est pas prête après ${MAX_WAIT}s." >&2
echo "État : podman compose -f docker-compose.stack.yml ps" >&2
echo "Logs BFF : podman compose -f docker-compose.stack.yml logs bff --tail 80" >&2
exit 1
