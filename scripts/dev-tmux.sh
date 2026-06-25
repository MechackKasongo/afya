#!/usr/bin/env bash
# Une fenêtre tmux par microservice (+ BFF + front Vite) — stack cible 9 services.
# Prérequis : podman compose up -d, tmux, JDK 21, ./mvnw install une fois.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SESSION="${TMUX_SESSION_NAME:-afya-platform}"

export JWT_ACCESS_SECRET="${JWT_ACCESS_SECRET:-dev-access-secret-at-least-64-characters-long-for-hs512-signing-key}"
export JWT_REFRESH_SECRET="${JWT_REFRESH_SECRET:-dev-refresh-secret-at-least-64-characters-long-for-hs512-signing-key}"
export BFF_PORT="${BFF_PORT:-8080}"

if ! command -v tmux >/dev/null 2>&1; then
  echo "tmux n'est pas installé : sudo dnf install -y tmux" >&2
  exit 1
fi

if tmux has-session -t "$SESSION" 2>/dev/null; then
  echo "Session tmux '$SESSION' existe déjà."
  echo "  Attacher    : tmux attach -t $SESSION"
  echo "  Détruire    : tmux kill-session -t $SESSION"
  exit 1
fi

cd "$ROOT"

run_service() {
  local module="$1"
  printf 'export JWT_ACCESS_SECRET="%s" JWT_REFRESH_SECRET="%s"; ./mvnw -pl %s spring-boot:run; exec bash' \
    "$JWT_ACCESS_SECRET" "$JWT_REFRESH_SECRET" "$module"
}

SERVICES=(
  "auth:auth-service"
  "user:user-service"
  "hospital:hospital-service"
  "patient:patient-service"
  "admission:admission-service"
  "medical:medical-service"
  "nursing:nursing-service"
  "lab:lab-service"
  "report:report-service"
  "audit:audit-service"
)

first=1
for entry in "${SERVICES[@]}"; do
  name="${entry%%:*}"
  module="${entry##*:}"
  cmd="$(run_service "$module")"
  if [[ "$first" -eq 1 ]]; then
    tmux new-session -ds "$SESSION" -n "$name" -c "$ROOT" bash -c "$cmd"
    first=0
  else
    tmux new-window -t "$SESSION" -n "$name" -c "$ROOT" bash -c "$cmd"
  fi
done

tmux new-window -t "$SESSION" -n bff -c "$ROOT" \
  bash -c "export JWT_ACCESS_SECRET=\"$JWT_ACCESS_SECRET\" JWT_REFRESH_SECRET=\"$JWT_REFRESH_SECRET\" SERVER_PORT=\"$BFF_PORT\"; ./mvnw -pl afya-bff spring-boot:run; exec bash"

if [[ -d "$ROOT/frontend/node_modules" ]] || [[ -f "$ROOT/frontend/package.json" ]]; then
  tmux new-window -t "$SESSION" -n web -c "$ROOT/frontend" \
    bash -c "npm run dev; exec bash"
fi

echo "Session '$SESSION' créée — stack cible 9 services :"
echo "  auth(8081) user(8089) hospital(8082) patient(8083) admission(8084)"
echo "  medical(8085) nursing(8093) lab(8092) report(8094) audit(8087) bff(${BFF_PORT}) [web(5173)]"
echo ""
echo "Avant le login UI :"
echo "  podman compose up -d"
echo "  podman compose up -d api"
echo "  ./scripts/check-ports.sh"
echo ""
echo "  Attacher : tmux attach -t $SESSION"
echo ""

tmux attach -t "$SESSION"
