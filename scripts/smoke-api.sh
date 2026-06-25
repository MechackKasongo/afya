#!/usr/bin/env bash
# Smoke test API — par défaut via la gateway (8090). BFF direct : GATEWAY_BASE=http://127.0.0.1:8080
# Tests étendus : SMOKE_EXTENDED=1 (défaut). Désactiver : SMOKE_EXTENDED=0
# Échec sur modules optionnels : SMOKE_STRICT=1 (défaut). Tolérer : SMOKE_STRICT=0
# Forcer une admission existante : SMOKE_ADMISSION_ID=42
set -euo pipefail

GATEWAY_BASE="${GATEWAY_BASE:-http://127.0.0.1:8090}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-Admin@Afya2026!}"
BFF_DIRECT="${BFF_DIRECT:-http://127.0.0.1:8080}"
SMOKE_EXTENDED="${SMOKE_EXTENDED:-1}"
SMOKE_SERVICE_NAME="${SMOKE_SERVICE_NAME:-Médecine interne}"

CURL_OPTS=()
if [[ "${GATEWAY_BASE}" == https://* ]]; then
  CURL_OPTS+=(-k)
fi

http_code() {
  curl "${CURL_OPTS[@]}" -s -o /dev/null -w "%{http_code}" "$@"
}

api_json() {
  local method="$1"
  local url="$2"
  local token="$3"
  local body="${4:-}"
  if [[ -n "$body" ]]; then
    curl "${CURL_OPTS[@]}" -s -X "$method" "$url" \
      -H "Authorization: Bearer ${token}" \
      -H 'Content-Type: application/json' \
      -d "$body"
  else
    curl "${CURL_OPTS[@]}" -s -X "$method" "$url" \
      -H "Authorization: Bearer ${token}"
  fi
}

json_field() {
  local json="$1"
  local path="$2"
  JSON="$json" PATH_EXPR="$path" python3 - <<'PY'
import json, os, sys
data = json.loads(os.environ["JSON"])
path = os.environ["PATH_EXPR"]
for part in path.split("."):
    if not part:
        continue
    if "[" in part:
        key, rest = part.split("[", 1)
        idx = int(rest.rstrip("]"))
        data = data[key][idx] if key else data[idx]
    else:
        data = data[part]
if data is None:
    sys.exit(0)
print(data)
PY
}

today_iso() {
  date -u +%Y-%m-%d
}

resolve_admission_id() {
  local token="$1"
  if [[ -n "${SMOKE_ADMISSION_ID:-}" ]]; then
    echo "${SMOKE_ADMISSION_ID}"
    return 0
  fi

  local admissions_json
  admissions_json=$(api_json GET \
    "${GATEWAY_BASE}/api/v1/admissions?status=EN_COURS&page=0&size=1" \
    "$token")
  local existing_id
  existing_id=$(json_field "$admissions_json" "content[0].id" 2>/dev/null || true)
  if [[ -n "$existing_id" ]]; then
    echo "$existing_id"
    return 0
  fi

  local patients_json patient_id
  patients_json=$(api_json GET "${GATEWAY_BASE}/api/v1/patients?page=0&size=1" "$token")
  patient_id=$(json_field "$patients_json" "content[0].id" 2>/dev/null || true)
  if [[ -z "$patient_id" ]]; then
    local suffix patient_json
    suffix=$(date +%s)
    patient_json=$(api_json POST "${GATEWAY_BASE}/api/v1/patients" "$token" \
      "{\"firstName\":\"Smoke\",\"lastName\":\"Test${suffix}\",\"birthDate\":\"1990-01-15\",\"sex\":\"M\"}")
    patient_id=$(json_field "$patient_json" "id")
  fi

  local admission_json admission_id
  admission_json=$(api_json POST "${GATEWAY_BASE}/api/v1/admissions" "$token" \
    "{\"patientId\":${patient_id},\"serviceName\":\"${SMOKE_SERVICE_NAME}\",\"reason\":\"Smoke test prescriptions\"}")
  admission_id=$(json_field "$admission_json" "id")
  echo "$admission_id"
}

smoke_prescriptions_by_admission() {
  local token="$1"
  echo "== Admissions — prescriptions par séjour =="

  local admission_id line_id rx_json admin_json today
  admission_id=$(resolve_admission_id "$token")
  echo "Admission #${admission_id}"

  rx_json=$(api_json GET \
    "${GATEWAY_BASE}/api/v1/admissions/${admission_id}/prescription-lines" \
    "$token")
  echo "GET prescription-lines : OK ($(echo "$rx_json" | head -c 120))"

  today=$(today_iso)
  rx_json=$(api_json POST \
    "${GATEWAY_BASE}/api/v1/admissions/${admission_id}/prescription-lines" \
    "$token" \
    "{\"medicationName\":\"Paracétamol smoke\",\"dosageText\":\"500 mg · 3x/jour\",\"prescriberName\":\"${ADMIN_USER}\",\"startDate\":\"${today}\"}")
  line_id=$(json_field "$rx_json" "id")
  if [[ -z "$line_id" ]]; then
    echo "Échec : pas d'id dans la réponse POST prescription-lines." >&2
    echo "$rx_json" >&2
    exit 1
  fi
  echo "POST prescription-lines : ligne #${line_id}"

  admin_json=$(api_json GET \
    "${GATEWAY_BASE}/api/v1/admissions/${admission_id}/prescription-lines/${line_id}/administrations" \
    "$token")
  echo "GET administrations : OK"

  admin_json=$(api_json POST \
    "${GATEWAY_BASE}/api/v1/admissions/${admission_id}/prescription-lines/${line_id}/administrations" \
    "$token" \
    "{\"administrationDate\":\"${today}\",\"slot\":\"MATIN\",\"administered\":true}")
  local admin_id
  admin_id=$(json_field "$admin_json" "id")
  if [[ -z "$admin_id" ]]; then
    echo "Échec : pas d'id dans la réponse POST administrations." >&2
    echo "$admin_json" >&2
    exit 1
  fi
  echo "POST administrations : #${admin_id}"

  admin_json=$(api_json GET \
    "${GATEWAY_BASE}/api/v1/admissions/${admission_id}/prescription-lines/${line_id}/administrations" \
    "$token")
  local admin_count
  admin_count=$(json_field "$admin_json" "[0].id" 2>/dev/null || true)
  if [[ -z "$admin_count" ]]; then
    echo "Échec : aucune administration après enregistrement." >&2
    exit 1
  fi
  echo "Parcours prescriptions admission OK"
  echo ""
}

expect_http() {
  local label="$1"
  local expected="$2"
  shift 2
  local code
  code=$(http_code "$@")
  if [[ "$code" != "$expected" ]]; then
    echo "Échec ${label} : HTTP ${code} (attendu ${expected})" >&2
    if [[ "${SMOKE_STRICT:-1}" == "1" ]]; then
      exit 1
    fi
    return 1
  fi
  echo "${label} : HTTP ${code}"
}

smoke_platform_modules() {
  local token="$1"
  echo "== Modules plateforme (régression) =="

  expect_http "GET hospital-services" 200 \
    "${GATEWAY_BASE}/api/v1/hospital-services" \
    -H "Authorization: Bearer ${token}"

  expect_http "GET admissions (liste)" 200 \
    "${GATEWAY_BASE}/api/v1/admissions?page=0&size=1" \
    -H "Authorization: Bearer ${token}"

  expect_http "GET lab exam-types" 200 \
    "${GATEWAY_BASE}/api/v1/lab/exam-types" \
    -H "Authorization: Bearer ${token}"

  expect_http "GET reports operational-stats" 200 \
    "${GATEWAY_BASE}/api/v1/reports/operational-stats" \
    -H "Authorization: Bearer ${token}"

  expect_http "GET reports activity" 200 \
    "${GATEWAY_BASE}/api/v1/reports/activity" \
    -H "Authorization: Bearer ${token}"

  echo "Modules plateforme OK"
  echo ""
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
    echo "  (et auth-service + user-service pour le login)" >&2
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

if [[ "${SMOKE_EXTENDED}" == "1" ]]; then
  if ! command -v python3 >/dev/null 2>&1; then
    echo "SMOKE_EXTENDED=1 requiert python3 pour parser le JSON — tests étendus ignorés." >&2
  else
    smoke_platform_modules "$TOKEN"
    smoke_prescriptions_by_admission "$TOKEN"
  fi
fi

echo "Smoke OK — ${GATEWAY_BASE}"
