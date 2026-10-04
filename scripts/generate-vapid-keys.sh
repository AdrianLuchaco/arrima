#!/usr/bin/env bash
# Generates the VAPID key pair for Web Push (the "¡Tiempo!" notifications), with openssl only.
# Prints the two values for VAPID_PUBLIC_KEY and VAPID_PRIVATE_KEY, base64url-encoded raw keys,
# the form browsers use. Paste them straight into Render (or backend/.env locally); never commit them.
#
# Usage: ./scripts/generate-vapid-keys.sh
set -euo pipefail

workdir=$(mktemp -d)
trap 'rm -rf "$workdir"' EXIT

openssl ecparam -name prime256v1 -genkey -noout -out "$workdir/vapid.pem" 2>/dev/null

base64url() { base64 | tr '+/' '-_' | tr -d '=\n'; }

# The private key in DER (SEC1): 7 bytes of header, then the 32 bytes of the key.
private_key=$(openssl ec -in "$workdir/vapid.pem" -outform DER 2>/dev/null | tail -c +8 | head -c 32 | base64url)
# The public key in DER (SubjectPublicKeyInfo) ends with the 65-byte uncompressed point.
public_key=$(openssl ec -in "$workdir/vapid.pem" -pubout -outform DER 2>/dev/null | tail -c 65 | base64url)

echo "VAPID_PUBLIC_KEY=$public_key"
echo "VAPID_PRIVATE_KEY=$private_key"
