#!/usr/bin/env bash
# Test data for trying the whole flow by hand, LOCALLY: a new club with two melees in the sign-up
# phase, one with 40 players (fits in doublettes) and one with 25 (does not fit).
# It goes through the real API, exactly as the app would.
#
# Requirements: backend running with the "local" profile and the Docker database up.
# Usage, from the repository root:   ./scripts/seed-dev.sh
# Never run it against production.
set -euo pipefail

API="${API:-http://localhost:8080}"
cd "$(dirname "$0")/../backend"
set -a && . ./.env && set +a

json() { python3 -c "import json,sys; print(json.load(sys.stdin)$1)"; }

post() { # post <path> <json> [token]
  curl -sf -X POST "$API$1" -H 'Content-Type: application/json' ${3:+-H "Authorization: Bearer $3"} -d "$2"
}

NAMES=(Manuel Paqui Pepe Antonio Lola Juan Carmen Paco Rosario Andrés Encarna Miguel Pilar José Mari Luis
  Concha Rafael Dolores Javier Isabel Fernando Amparo Ángel Remedios Vicente Teresa Ramón Inma Alfonso
  Mercedes Julián Fina Tomás Asun Emilio Rocío Salvador Puri Ginés)

participants() { # participants <count> → JSON list "1. Manuel", "2. Paqui"...
  local items=() i
  for ((i = 1; i <= $1; i++)); do
    items+=("{\"listNumber\":$i,\"name\":\"${NAMES[$(((i - 1) % ${#NAMES[@]}))]}\"}")
  done
  (IFS=,; echo "{\"participants\":[${items[*]}]}")
}

CODE=$(docker compose exec -T postgres psql -U "$DB_USERNAME" -d arrima -tA < ../scripts/create-invitation.sql | tail -1)
EMAIL="prueba-$(date +%s)@arrima.local"
PASSWORD="petanca de prueba"

TOKEN=$(post /api/auth/register \
  "{\"invitationCode\":\"$CODE\",\"clubName\":\"Club de Petanca de Prueba\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
  | json '["accessToken"]')

for players in 40 25; do
  MELEE=$(post /api/melees '{"teamSize":2}' "$TOKEN" | json '["id"]')
  post "/api/melees/$MELEE/participants/import" "$(participants "$players")" "$TOKEN" > /dev/null
  echo "Melé $MELEE: $players jugadores"
done

echo
echo "Entra en http://localhost:5173/entrar con:"
echo "  correo:     $EMAIL"
echo "  contraseña: $PASSWORD"
