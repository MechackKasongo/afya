#!/usr/bin/env bash
# Une fenêtre tmux par microservice (+ BFF + front Vite).
# Prérequis : podman compose up -d, tmux, JDK 21, ./mvnw install une fois.
#
# Usage :
#   chmod +x scripts/dev-tmux.sh
#   ./scripts/dev-tmux.sh              # attache la session
#   BFF_PORT=8088 ./scripts/dev-tmux.sh
#
# Session : afya-platform (TMUX_SESSION_NAME pour changer)
# Arrêter tout : tmux kill-session -t afya-platform

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

# Ordre de démarrage : identity/catalog/patient d'abord, care-entry ensuite, BFF en dernier.
SERVICES=(
  "identity:identity-service"
  "catalog:catalog-service"
  "patient:patient-service"
  "stay:stay-service"
  "care:care-entry-service"
  "clinical:clinical-record-service"
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

echo "Session '$SESSION' créée — une fenêtre par service :"
echo "  identity(8081) catalog(8082) patient(8083) stay(8085) care(8084)"
echo "  clinical(8086) audit(8087) bff(${BFF_PORT}) [web(5173)]"
echo ""
echo "Avant le login UI :"
echo "  podman compose up -d"
echo "  podman compose up -d api    # gateway 8090 (après le BFF)"
echo "  ./scripts/check-ports.sh"
echo ""
echo "  Attacher : tmux attach -t $SESSION"
echo "  Raccourcis : Ctrl+b n (suivante) | p (précédente) | d (détacher)"
echo ""

tmux attach -t "$SESSION"
