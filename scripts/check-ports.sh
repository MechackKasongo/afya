#!/usr/bin/env bash
# Liste les ports habituels des services Afya + indique les conflits.
set -euo pipefail

PORTS=(5173 8090 8080 8081 8082 8083 8084 8085 8087 8089 8092 8093 8094 9000)
LABELS=(
  "Vite (frontend)"
  "API Gateway"
  "BFF"
  "auth"
  "hospital"
  "patient"
  "admission"
  "medical"
  "audit"
  "user"
  "lab"
  "nursing"
  "report"
  "MinIO"
)

echo "Ports Afya (dev) :"
echo "-------------------"
for i in "${!PORTS[@]}"; do
  p="${PORTS[$i]}"
  lbl="${LABELS[$i]:-}"
  if ss -tlnp 2>/dev/null | grep -q ":${p}\\b"; then
    printf "  %5s  OCCUPÉ   (%s)\n" "$p" "$lbl"
    ss -tlnp 2>/dev/null | grep ":${p}\\b" || true
  else
    printf "  %5s  libre    (%s)\n" "$p" "$lbl"
  fi
done
echo ""
echo "PostgreSQL (compose) : voir ports dans docker-compose.yml (auth 5443, user 5444, …)"
