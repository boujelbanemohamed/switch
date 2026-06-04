#!/usr/bin/env bash
#
# api-smoke.sh — Teste chaque endpoint de l'API avec un compte admin.
# Valide la couche que les tests UI ne voient pas (réponses HTTP, sécurité de base).
#
# Prérequis : le backend doit tourner sur http://localhost:8085
# Usage : bash scripts/api-smoke.sh
#
set -uo pipefail

BASE="http://localhost:8085"
G='\033[0;32m'; R='\033[0;31m'; Y='\033[0;33m'; N='\033[0m'

echo "== Login admin =="
TOKEN=$(curl -sf -X POST "$BASE/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | \
  grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "${TOKEN:-}" ]; then
  echo -e "${R}Échec du login. Le backend tourne-t-il ? Le profil dev est-il actif (DataSeeder) ?${N}"
  exit 1
fi
echo -e "${G}Token obtenu.${N}"

PASS=0; FAIL=0

check() {
  local method="$1" path="$2" expected="$3"
  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" \
    -H "Authorization: Bearer $TOKEN" "$BASE$path")
  if [ "$code" = "$expected" ] || { [ "$expected" = "2xx" ] && [ "${code:0:1}" = "2" ]; }; then
    echo -e "${G}✓${N} $method $path → $code"
    PASS=$((PASS+1))
  else
    echo -e "${R}✗${N} $method $path → $code (attendu $expected)"
    FAIL=$((FAIL+1))
  fi
}

echo ""
echo "== GET endpoints (lecture) =="
check GET "/actuator/health"                         "2xx"
check GET "/api/v1/disputes"                          "2xx"
check GET "/api/v1/batch/history"                     "2xx"
check GET "/api/v1/netting/latest"                    "2xx"
check GET "/api/v1/fees/schedules"                    "2xx"
check GET "/api/v1/issuing/programs"                  "2xx"
check GET "/api/v1/issuing/virtual-cards"             "2xx"
check GET "/api/v1/kyc/verifications"                 "2xx"
check GET "/api/v1/kyc/documents"                     "2xx"
check GET "/api/v1/admin/live-config"                 "2xx"
check GET "/api/v1/backoffice/reports?type=TRANSACTION" "2xx"
check GET "/api/v1/issuing/cardholders"               "2xx"
check GET "/api/v1/acquiring/merchants?status=ACTIVE" "2xx"

echo ""
echo "== Sécurité : sans token, on doit être rejeté (401) =="
NOAUTH=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/v1/disputes")
if [ "$NOAUTH" = "401" ] || [ "$NOAUTH" = "403" ]; then
  echo -e "${G}✓${N} GET /disputes sans token → $NOAUTH (correctement rejeté)"
  PASS=$((PASS+1))
else
  echo -e "${R}✗${N} GET /disputes sans token → $NOAUTH (FAILLE: devrait être 401/403)"
  FAIL=$((FAIL+1))
fi

echo ""
echo "============================================"
echo -e "Résultat : ${G}$PASS OK${N}, ${R}$FAIL KO${N}"
echo "============================================"
[ "$FAIL" -gt 0 ] && exit 1 || exit 0
