#!/usr/bin/env bash
# Smoke test API — par défaut via la gateway (8090). BFF direct : GATEWAY_BASE=http://127.0.0.1:8080
set -euo pipefail

GATEWAY_BASE="${GATEWAY_BASE:-http://127.0.0.1:8090}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-Admin@Afya2026!}"
BFF_DIRECT="${BFF_DIRECT:-http://127.0.0.1:8080}"

CURL_OPTS=()
if [[ "${GATEWAY_BASE}" == https://* ]]; then
  CURL_OPTS+=(-k)
fi

http_code() {
  curl "${CURL_OPTS[@]}" -s -o /dev/null -w "%{http_code}" "$@"
}

echo "== Gateway health =="
curl "${CURL_OPTS[@]}" -sf "${GATEWAY_BASE}/gateway/health" | head -1
echo ""

echo "== BFF health (via gateway) =="
BFF_CODE=$(http_code "${GATEWAY_BASE}/actuator/health" || true)
if [[ "${BFF_CODE}" != "200" ]]; then
  echo "Échec HTTP ${BFF_CODE} sur ${GATEWAY_BASE}/actuator/health" >&2
  DIRECT_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${BFF_DIRECT}/actuator/health" 2>/dev/null) || DIRECT_CODE="000"
  if [[ "${DIRECT_CODE}" != "200" ]]; then
    echo "" >&2
    echo "Le BFF ne répond pas sur ${BFF_DIRECT} (code ${DIRECT_CODE})." >&2
    echo "La gateway proxy vers le BFF (BFF_UPSTREAM) — démarrez-le avant le smoke :" >&2
    echo "  ./mvnw -pl afya-bff spring-boot:run" >&2
    echo "  (et identity-service pour le login)" >&2
    echo "" >&2
    echo "Ou stack complète sans Maven sur l'hôte :" >&2
    echo "  ./scripts/stack-up.sh" >&2
    echo "  GATEWAY_BASE=https://127.0.0.1:8443 ./scripts/smoke-api.sh" >&2
  else
    echo "BFF joignable en direct (${BFF_DIRECT}) mais pas via la gateway." >&2
    echo "Vérifiez BFF_UPSTREAM dans docker-compose.yml (défaut host.docker.internal:8080)." >&2
  fi
  exit 1
fi
curl "${CURL_OPTS[@]}" -sf "${GATEWAY_BASE}/actuator/health" | head -c 200
echo ""
echo ""

echo "== Login =="
LOGIN_JSON=$(curl "${CURL_OPTS[@]}" -sf -X POST "${GATEWAY_BASE}/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}")
echo "$LOGIN_JSON" | head -c 300
echo ""

TOKEN=$(echo "$LOGIN_JSON" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
if [[ -z "$TOKEN" ]]; then
  echo "Échec : pas de jeton dans la réponse login." >&2
  exit 1
fi

echo "== Profil /auth/me =="
curl "${CURL_OPTS[@]}" -sf "${GATEWAY_BASE}/api/v1/auth/me" -H "Authorization: Bearer ${TOKEN}" | head -c 400
echo ""
echo ""
echo "Smoke OK — ${GATEWAY_BASE}"
