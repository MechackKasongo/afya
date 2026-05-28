#!/usr/bin/env bash
# Pré-télécharge les images de base (évite des échecs en rafale pendant compose build).
set -euo pipefail

IMAGES=(
  docker.io/library/eclipse-temurin:21-jdk
  docker.io/library/eclipse-temurin:21-jre
  docker.io/library/postgres:16-alpine
  docker.io/minio/minio:latest
  docker.io/library/nginx:1.27-alpine
  docker.io/library/node:20-alpine
)

echo "Téléchargement des images de base (une par une)…"
for img in "${IMAGES[@]}"; do
  echo "→ ${img}"
  if ! podman pull "${img}"; then
    echo "Échec : ${img}" >&2
    echo "Vérifiez la connexion à registry-1.docker.io (réseau, proxy, VPN) puis relancez." >&2
    exit 1
  fi
done
echo "OK — images prêtes."
