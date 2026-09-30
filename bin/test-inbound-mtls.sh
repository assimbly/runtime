#!/usr/bin/env bash
#
# Tests mutual TLS on an HTTPS inbound (source) flow.
#
# Usage:
#   bin/test-inbound-mtls.sh -u <url> (-d <dil.json> | -c <client.p12>) [-p <password>] [-n <plain-url>]
#
#   -u  URL of the mutual TLS flow, e.g. https://localhost:9001/regressiontests/
#   -d  DIL file: the client certificate is taken from the (first) connection "certificate" data url
#   -c  Client certificate/key as PKCS12 (.p12/.pfx), instead of -d
#   -p  Password of the PKCS12 (default: fluxygen)
#   -n  Optional URL of an HTTPS flow without mutual TLS (should still work without a client certificate)
#
# Checks:
#   1. trusted client certificate    -> 200
#   2. no client certificate         -> 403
#   3. untrusted client certificate  -> 403 (a self-signed certificate is generated)
#   4. plain flow, no certificate    -> 200 (only with -n)
#
# Requires openssl and curl. Windows curl (Schannel) can't send a client certificate from a PKCS12 file,
# in that case the certificate checks are done with openssl s_client (or run this script from WSL).

set -u

PASSWORD="fluxygen"
URL=""
DIL=""
P12=""
PLAIN_URL=""

while getopts "u:d:c:p:n:" opt; do
  case $opt in
    u) URL=$OPTARG ;;
    d) DIL=$OPTARG ;;
    c) P12=$OPTARG ;;
    p) PASSWORD=$OPTARG ;;
    n) PLAIN_URL=$OPTARG ;;
    *) sed -n '3,22p' "$0"; exit 2 ;;
  esac
done

if [ -z "$URL" ] || { [ -z "$DIL" ] && [ -z "$P12" ]; }; then
  sed -n '3,22p' "$0"; exit 2
fi

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

if [ -n "$DIL" ]; then
  grep -o '"certificate": *"data:[^"]*' "$DIL" | head -1 | sed 's/.*base64,//' | base64 -d > "$WORK/client.p12" 2>/dev/null
  if [ ! -s "$WORK/client.p12" ]; then
    echo "No certificate data url found in $DIL"; exit 2
  fi
else
  cp "$P12" "$WORK/client.p12" || exit 2
fi
P12="$WORK/client.p12"

# Client certificate and key as PEM (for openssl s_client)
openssl pkcs12 -in "$P12" -passin "pass:$PASSWORD" -nokeys -clcerts -out "$WORK/client.crt" 2>/dev/null &&
openssl pkcs12 -in "$P12" -passin "pass:$PASSWORD" -nocerts -nodes -out "$WORK/client.key" 2>/dev/null ||
  { echo "Can't read $P12 with the given password"; exit 2; }

# Untrusted self-signed client certificate
MSYS2_ARG_CONV_EXCL="/CN=" openssl req -x509 -newkey rsa:2048 -nodes -days 1 -subj "/CN=UntrustedClient" \
  -keyout "$WORK/untrusted.key" -out "$WORK/untrusted.crt" 2>/dev/null
openssl pkcs12 -export -inkey "$WORK/untrusted.key" -in "$WORK/untrusted.crt" -passout pass:untrusted -out "$WORK/untrusted.p12"

if curl -V | head -1 | grep -qi openssl; then
  USE_CURL_CERT=true
else
  USE_CURL_CERT=false
  echo "curl doesn't use OpenSSL (Windows Schannel): certificate checks are done with openssl s_client"
fi

# Returns the HTTP status code of a POST request, $1 = url, $2 = p12 file (optional), $3 = p12 password
status() {
  local url=$1 p12=${2:-} pass=${3:-}
  if [ -z "$p12" ]; then
    curl -sk --ssl-no-revoke -m 15 -X POST -o /dev/null -w "%{http_code}" "$url" 2>/dev/null ||
      curl -sk -m 15 -X POST -o /dev/null -w "%{http_code}" "$url"
  elif [ "$USE_CURL_CERT" = true ]; then
    curl -sk -m 15 --cert-type P12 --cert "$p12:$pass" -X POST -o /dev/null -w "%{http_code}" "$url"
  else
    local hostport path crt key
    hostport=$(echo "$url" | sed -E 's#^https://([^/]+).*#\1#')
    path=$(echo "$url" | sed -E 's#^https://[^/]+##'); path=${path:-/}
    crt="${p12%.*}.crt"; key="${p12%.*}.key"
    printf 'POST %s HTTP/1.1\r\nHost: %s\r\nContent-Length: 0\r\nConnection: close\r\n\r\n' "$path" "$hostport" |
      timeout 15 openssl s_client -quiet -connect "$hostport" -servername "${hostport%:*}" -cert "$crt" -key "$key" 2>/dev/null |
      head -1 | awk '{print $2}'
  fi
}

FAILED=0
check() {
  local name=$1 expected=$2 actual=$3
  if [ "$actual" = "$expected" ]; then
    echo "PASS  $name: HTTP $actual"
  else
    echo "FAIL  $name: expected HTTP $expected, got ${actual:-no response}"
    FAILED=1
  fi
}

check "trusted client certificate" 200 "$(status "$URL" "$P12" "$PASSWORD")"
check "no client certificate" 403 "$(status "$URL")"
check "untrusted client certificate" 403 "$(status "$URL" "$WORK/untrusted.p12" untrusted)"
if [ -n "$PLAIN_URL" ]; then
  check "plain flow without client certificate" 200 "$(status "$PLAIN_URL")"
fi

exit $FAILED
