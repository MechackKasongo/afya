#!/bin/sh
set -eu

BFF_UPSTREAM="${BFF_UPSTREAM:-http://bff:8080}"
TLS_CERT_PATH="${TLS_CERT_PATH:-/etc/nginx/certs/server.crt}"
TLS_KEY_PATH="${TLS_KEY_PATH:-/etc/nginx/certs/server.key}"

# http://bff:8080 → host/port pour proxy_pass dynamique (DNS re-résolu à chaque requête).
bff_no_scheme="${BFF_UPSTREAM#http://}"
bff_no_scheme="${bff_no_scheme#https://}"
BFF_HOST="${bff_no_scheme%%:*}"
bff_rest="${bff_no_scheme#*:}"
BFF_PORT="${bff_rest%%/*}"
if [ -z "${BFF_PORT}" ] || [ "${BFF_PORT}" = "${bff_no_scheme}" ]; then
  BFF_PORT="8080"
fi

RESOLVER_NS="$(grep -m1 '^nameserver' /etc/resolv.conf 2>/dev/null | awk '{print $2}')"
if [ -z "${RESOLVER_NS}" ]; then
  RESOLVER_NS="127.0.0.11"
fi

# sed (pas envsubst) : préserve les variables Nginx ($host, $remote_addr, …).
render() {
  sed \
    -e "s|\${RESOLVER_NS}|${RESOLVER_NS}|g" \
    -e "s|\${BFF_HOST}|${BFF_HOST}|g" \
    -e "s|\${BFF_PORT}|${BFF_PORT}|g" \
    -e "s|\${TLS_CERT_PATH}|${TLS_CERT_PATH}|g" \
    -e "s|\${TLS_KEY_PATH}|${TLS_KEY_PATH}|g" \
    "$1"
}

render /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf

if [ -f "${TLS_CERT_PATH}" ] && [ -f "${TLS_KEY_PATH}" ]; then
  render /etc/nginx/nginx.tls-server.conf.template >> /etc/nginx/nginx.conf
fi

echo "}" >> /etc/nginx/nginx.conf

exec nginx -g 'daemon off;'
