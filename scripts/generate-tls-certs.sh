#!/usr/bin/env bash
# Certificats TLS auto-signés pour la gateway Afya (dev / stack conteneurisée).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CERT_DIR="${ROOT}/infra/gateway/certs"
DAYS="${TLS_CERT_DAYS:-825}"
CN="${TLS_CERT_CN:-localhost}"

mkdir -p "${CERT_DIR}"

openssl req -x509 -nodes -days "${DAYS}" -newkey rsa:2048 \
  -keyout "${CERT_DIR}/server.key" \
  -out "${CERT_DIR}/server.crt" \
  -subj "/CN=${CN}/O=Afya Dev/C=CD" \
  -addext "subjectAltName=DNS:localhost,DNS:127.0.0.1,IP:127.0.0.1" \
  2>/dev/null

chmod 644 "${CERT_DIR}/server.crt"
chmod 600 "${CERT_DIR}/server.key"

echo "Certificats générés :"
echo "  ${CERT_DIR}/server.crt"
echo "  ${CERT_DIR}/server.key"
echo ""
echo "API HTTPS : https://127.0.0.1:8443 (après podman compose -f docker-compose.stack.yml up -d api)"
echo "Le navigateur affichera un avertissement (normal pour un certificat auto-signé)."
