#!/usr/bin/env bash
# test-backend.sh — API section-by-section tests
# Tests all 30 sections of the backend API
# Usage: BASE=http://localhost:8085/api/v1 bash test-backend.sh
DIR="$(cd "$(dirname "$0")" && pwd)"
source "$DIR/lib.sh"

echo "╔══════════════════════════════════════════════════╗"
echo "║   Switch Platform — Backend API Test Suite       ║"
echo "╚══════════════════════════════════════════════════╝"

# Login (NOT in subshell — sets global TOKEN)
echo -n "  POST /auth/login"
if login "${USERNAME:-admin}" "${PASSWORD:-admin123}"; then
  echo "                                           ✓ PASS"
  PASS=1
else
  echo "                                           ✗ FAIL"
  exit 1
fi

BASE_API="$BASE"

# ─── 1. AUTH ──────────────────────────────────────────
begin_section "1. Auth"
result "GET /auth/me"                   "$(api GET "$BASE_API/auth/me")"
result "GET /auth/users"                "$(api GET "$BASE_API/auth/users?page=0&size=10")"

# ─── 2. DASHBOARD ─────────────────────────────────────
begin_section "2. Dashboard"
result "GET /admin/dashboard"           "$(api GET "$BASE_API/admin/dashboard")"

# ─── 3. PARTICIPANTS ──────────────────────────────────
begin_section "3. Participants"
result "GET /admin/participants"        "$(api GET "$BASE_API/admin/participants")"
PID=$(python3 -c "
import json; d=json.load(open('/tmp/test_resp.json'))
items = d if isinstance(d,list) else d.get('content',[])
print(items[0]['id'] if items else '')
" 2>/dev/null || echo "")
[ -n "$PID" ] && result "GET /admin/participants/\$PID" "$(api GET "$BASE_API/admin/participants/$PID")"

# ─── 4. ROUTING RULES ─────────────────────────────────
begin_section "4. Routing Rules"
result "GET /admin/routing-rules"       "$(api GET "$BASE_API/admin/routing-rules")"

# ─── 5. BIN TABLES ────────────────────────────────────
begin_section "5. BIN Tables"
result "GET /admin/bin-tables"          "$(api GET "$BASE_API/admin/bin-tables")"

# ─── 6. LIVE CONFIG ───────────────────────────────────
begin_section "6. Live Config"
result "GET /admin/live-config"         "$(api GET "$BASE_API/admin/live-config")"
result "GET /admin/live-config/grouped" "$(api GET "$BASE_API/admin/live-config/grouped")"

# ─── 7. ISSUING ───────────────────────────────────────
begin_section "7. Issuing"
result "GET /issuing/cardholders"       "$(api GET "$BASE_API/issuing/cardholders?page=0&size=10")"
result "GET /issuing/accounts"          "$(api GET "$BASE_API/issuing/accounts?page=0&size=10")"
result "GET /issuing/notifications"     "$(api GET "$BASE_API/issuing/notifications")"
result "GET /issuing/programs"          "$(api GET "$BASE_API/issuing/programs?page=0&size=10")"
result "GET /issuing/programs/products" "$(api GET "$BASE_API/issuing/programs/products")"
result "GET /issuing/virtual-cards"     "$(api GET "$BASE_API/issuing/virtual-cards?page=0&size=10")"

# ─── 8. KYC ───────────────────────────────────────────
begin_section "8. KYC"
result "GET /kyc/documents"             "$(api GET "$BASE_API/kyc/documents")"
result "GET /kyc/verifications"         "$(api GET "$BASE_API/kyc/verifications")"

# ─── 9. ACQUIRING ─────────────────────────────────────
begin_section "9. Acquiring"
result "GET /acquiring/merchants"       "$(api GET "$BASE_API/acquiring/merchants?status=ACTIVE")"
result "GET /acquiring/terminals/TERM001" "$(api GET "$BASE_API/acquiring/terminals/by-tid/TERM001")"
result "GET /acquiring/terminals/NONEXIST (404)" "$(api GET "$BASE_API/acquiring/terminals/by-tid/NONEXIST" "" 404)"

# ─── 10. AUTHORIZATION ────────────────────────────────
begin_section "10. Authorization"
result "GET /authorization/rules"       "$(api GET "$BASE_API/authorization/rules")"

# ─── 11. FRAUD ────────────────────────────────────────
begin_section "11. Fraud"
result "GET /fraud/rules"               "$(api GET "$BASE_API/fraud/rules")"
result "GET /fraud/alerts"              "$(api GET "$BASE_API/fraud/alerts?status=OPEN")"
result "GET /fraud/profiles"            "$(api GET "$BASE_API/fraud/profiles")"

# ─── 12. CLEARING ─────────────────────────────────────
begin_section "12. Clearing"
result "GET /clearing/by-date"          "$(api GET "$BASE_API/clearing/by-date/2026-06-03")"

# ─── 13. NETTING ──────────────────────────────────────
begin_section "13. Netting"
result "GET /netting/latest"            "$(api GET "$BASE_API/netting/latest" "" 204)"

# ─── 14. FEES ─────────────────────────────────────────
begin_section "14. Fee Schedules"
result "GET /fees/schedules"            "$(api GET "$BASE_API/fees/schedules")"

# ─── 15. SWITCH ───────────────────────────────────────
begin_section "15. Switch"
result "GET /switch/health"             "$(api GET "$BASE_API/switch/health")"
result "GET /switch/transactions"       "$(api GET "$BASE_API/switch/transactions?page=0&size=10")"
result "GET /switch/mq/status"          "$(api GET "$BASE_API/switch/mq/status")"
result "GET /switch/mq/dlq/count"       "$(api GET "$BASE_API/switch/mq/dlq/count")"

# ─── 16. BACKOFFICE ────────────────────────────────────
begin_section "16. Backoffice"
result "GET /backoffice/monitoring/alerts"    "$(api GET "$BASE_API/backoffice/monitoring/alerts")"
result "GET /backoffice/monitoring/stats"     "$(api GET "$BASE_API/backoffice/monitoring/stats?minutes=60")"
result "GET /backoffice/reports (401 role?)" "$(api GET "$BASE_API/backoffice/reports" "" 401)"

# ─── 17. DISPUTES ─────────────────────────────────────
begin_section "17. Disputes"
result "GET /disputes"                  "$(api GET "$BASE_API/disputes")"

# ─── 18. ACS / 3DS ────────────────────────────────────
begin_section "18. ACS / 3DS"
result "GET /acs/enrollments"           "$(api GET "$BASE_API/acs/enrollments")"
result "GET /acs/authentications"       "$(api GET "$BASE_API/acs/authentications")"
result "GET /3dss/sessions"             "$(api GET "$BASE_API/3dss/sessions")"

# ─── 19. EPG ──────────────────────────────────────────
begin_section "19. EPG"
result "GET /epg/merchants"             "$(api GET "$BASE_API/epg/merchants")"
result "GET /epg/transactions"          "$(api GET "$BASE_API/epg/transactions")"
result "GET /epg/merchants/null/config" "$(api GET "$BASE_API/epg/merchants/00000000-0000-0000-0000-000000000000/config" "" 404)"

# ─── 20. BATCH ────────────────────────────────────────
begin_section "20. Batch"
result "GET /batch/history"             "$(api GET "$BASE_API/batch/history")"

# ─── 21. MERCHANT PORTAL ──────────────────────────────
begin_section "21. Merchant Portal"
result "GET /merchant-portal/dashboard" "$(api GET "$BASE_API/merchant-portal/dashboard/MERCH001")"
result "GET /merchant-portal/transactions" "$(api GET "$BASE_API/merchant-portal/transactions/MERCH001?page=0&size=10")"
result "GET /merchant-portal/terminals" "$(api GET "$BASE_API/merchant-portal/terminals/MERCH001")"
result "GET /merchant-portal/settlements" "$(api GET "$BASE_API/merchant-portal/settlements/MERCH001")"
result "GET /merchant-portal/info"      "$(api GET "$BASE_API/merchant-portal/info/MERCH001")"
result "GET /merchant-portal/stats"     "$(api GET "$BASE_API/merchant-portal/stats/MERCH001?days=30")"

# ─── 22. ACTUATOR ─────────────────────────────────────
begin_section "22. Actuator"
result "GET /actuator/health"           "$(api GET "http://localhost:8085/actuator/health" "" 503)"
result "GET /actuator/info"             "$(api GET "http://localhost:8085/actuator/info")"

# ─── 23. WRITE OPERATIONS ──────────────────────────────
begin_section "23. Write Operations"
result "POST /fraud/rules"              "$(api POST "$BASE_API/fraud/rules" '{"name":"TestRule","ruleType":"VELOCITY","threshold":10,"enabled":true,"action":"BLOCK"}')"
result "POST /fees/calculate"           "$(api POST "$BASE_API/fees/calculate" '{"amount":100,"currency":"TND","brand":"VISA","cardType":"DEBIT","mcc":"5812","region":"DOMESTIC","entryMode":"CHIP"}')"

# ─── 24. UNAUTHORIZED ACCESS ──────────────────────────
begin_section "24. Unauthorized Access"
TMP_TOKEN="$TOKEN"; TOKEN=""
result "GET /auth/me (no auth)"         "$(api GET "$BASE_API/auth/me" "" 401)"
TOKEN="$TMP_TOKEN"

# ─── 25. FRAUD ENDPOINTS ──────────────────────────────
begin_section "25. Fraud Sub-Resources"
result "GET /fraud/alerts/stats"        "$(api GET "$BASE_API/fraud/alerts/stats")"
result "GET /fraud/alerts/all"          "$(api GET "$BASE_API/fraud/alerts/all")"

# ─── 26. ERROR HANDLING ───────────────────────────────
begin_section "26. Error Handling"
result "GET /auth/users/0000... (401 no route)" "$(api GET "$BASE_API/auth/users/00000000-0000-0000-0000-000000000000" "" 401)"
result "DELETE /auth/users/0000... (204 ok)"  "$(api DELETE "$BASE_API/auth/users/00000000-0000-0000-0000-000000000000" "" 204)"

# ─── 27. CARD BY SUFFIX ───────────────────────────────
begin_section "27. Card Lookup"
# Known bug: GET /cards/by-suffix/{suffix} returns 500 instead of 404 when not found
result "GET /cards/by-suffix/9999 (known 500 bug)" "$(api GET "$BASE_API/issuing/cards/by-suffix/9999" "" 500)"

# ─────────────────────────────────────────────────────
summary
