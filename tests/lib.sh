#!/usr/bin/env bash
# lib.sh — Shared helpers for section-by-section API tests
BASE="${BASE:-http://localhost:8085/api/v1}"
TOKEN=""
PASS=0
FAIL=0
SKIP=0
SECTION=""

curl() { command curl -sk "$@"; }

# Sets global TOKEN, use directly (not in subshell)
login() {
  local u="${1:-admin}" p="${2:-admin123}"
  TOKEN=$(curl -s -X POST "$BASE/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$u\",\"password\":\"$p\"}" | python3 -c "
import sys,json
try:
    d=json.load(sys.stdin)
    print(d.get('accessToken',''))
except: print('')
" 2>/dev/null || echo "")
  [ -n "$TOKEN" ]
}

api() {
  local method="${1:-GET}" url="$2" body="${3:-}" expected="${4:-200}"
  local args=(-s -o /tmp/test_resp.json -w "%{http_code}" -X "$method" -H "Content-Type: application/json")
  if [ -n "$TOKEN" ]; then args+=(-H "Authorization: Bearer $TOKEN"); fi
  if [ -n "$body" ]; then args+=(-d "$body"); fi
  local rc
  rc=$(curl "${args[@]}" "$url" 2>/dev/null || echo "000")
  # Retry once if rate limited
  if [ "$rc" = "429" ]; then
    sleep 1
    rc=$(curl "${args[@]}" "$url" 2>/dev/null || echo "000")
  fi
  if [ "$rc" = "$expected" ]; then echo "ok"
  elif [ "$rc" = "429" ]; then echo "RATE_LIMITED"
  else echo "FAIL (got $rc, expected $expected)"; fi
}

result() {
  local label="$1" status="$2"
  if [ "$status" = "ok" ]; then
    PASS=$((PASS + 1))
    printf "  %-62s %s\n" "$label" "✓ PASS"
  elif [ "$status" = "RATE_LIMITED" ]; then
    SKIP=$((SKIP + 1))
    printf "  %-62s %s\n" "$label" "− SKIP (rate limited)"
  else
    FAIL=$((FAIL + 1))
    printf "  %-62s %s\n" "$label" "✗ $status"
  fi
  sleep 0.2
}

begin_section() {
  SECTION="$1"
  echo ""
  echo "─── $SECTION ───"
}

summary() {
  echo ""
  echo "=========================================="
  echo "  TOTAL  : $((PASS + FAIL + SKIP)) tests"
  echo "  PASSED : $PASS"
  echo "  FAILED : $FAIL"
  echo "  SKIPPED: $SKIP (rate limited)"
  echo "=========================================="
}
