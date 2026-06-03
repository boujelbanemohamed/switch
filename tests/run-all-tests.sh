#!/usr/bin/env bash
# run-all-tests.sh — Consolidated test runner
# Runs backend API tests + frontend Playwright tests
# Generates HTML report
DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$DIR/.." && pwd)"
REPORT_DIR="$DIR/report"
mkdir -p "$REPORT_DIR"
START=$(date +%s)

echo "╔══════════════════════════════════════════════════╗"
echo "║   Switch Platform — Full Test Runner             ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""

# ─── Step 1: Backend API Tests ─────────────────────────
echo "━━━ Step 1/3: Backend API Tests ─━━"
BE_OUT="$REPORT_DIR/backend.txt"
bash "$DIR/test-backend.sh" > "$BE_OUT" 2>&1
echo "[done]"

# Parse backend results
BE_PASS=$(grep -c "✓ PASS" "$BE_OUT" 2>/dev/null) || BE_PASS=0
BE_FAIL=$(grep -c "✗" "$BE_OUT" 2>/dev/null) || BE_FAIL=0
BE_TOTAL=$((BE_PASS + BE_FAIL))
echo "  Backend API : $BE_PASS/$BE_TOTAL"
echo ""

# ─── Step 2: Frontend Playwright Tests ──────────────────
echo "━━━ Step 2/3: Frontend Playwright Tests ─━━"
FE_OUT="$REPORT_DIR/frontend.txt"
cd "$ROOT/frontend"
npx playwright test --reporter=list > "$FE_OUT" 2>&1 || true
echo "[done]"

FE_PASS=$(grep -c "✓" "$FE_OUT" 2>/dev/null) || FE_PASS=0
FE_FAIL=$(grep -c "^  ✘\|^  ✗" "$FE_OUT" 2>/dev/null) || FE_FAIL=0
FE_TOTAL=$((FE_PASS + FE_FAIL))
echo "  Frontend E2E: $FE_PASS/$FE_TOTAL"
echo ""

# ─── Step 3: Generate HTML Report ───────────────────────
END=$(date +%s)
DURATION=$((END - START))
echo "━━━ Step 3/3: Generating HTML Report ─━━"

PW_REPORT="$ROOT/frontend/playwright-report"
[ -d "$PW_REPORT" ] && cp -r "$PW_REPORT" "$REPORT_DIR/playwright-report" 2>/dev/null || true

if [ "$BE_TOTAL" -eq 0 ]; then BE_TOTAL=1; BE_PASS=0; fi
if [ "$FE_TOTAL" -eq 0 ]; then FE_TOTAL=1; FE_PASS=0; fi

cat > "$REPORT_DIR/index.html" << HTML_EOF
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Switch Platform — Test Report</title>
<style>
  *{margin:0;padding:0;box-sizing:border-box}
  body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#0f172a;color:#e2e8f0;padding:40px}
  h1{font-size:28px;margin-bottom:8px}
  .subtitle{color:#94a3b8;margin-bottom:32px}
  .grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:20px;margin-bottom:40px}
  .card{background:#1e293b;border-radius:12px;padding:24px;border:1px solid #334155}
  .card h3{font-size:14px;text-transform:uppercase;letter-spacing:1px;color:#94a3b8;margin-bottom:8px}
  .card .number{font-size:36px;font-weight:700}
  .green{color:#22c55e}.red{color:#ef4444}.blue{color:#3b82f6}
  .badge{display:inline-block;padding:4px 12px;border-radius:9999px;font-size:13px;font-weight:600}
  .badge.pass{background:#166534;color:#86efac}
  .badge.fail{background:#991b1b;color:#fca5a5}
  .section{margin-bottom:32px}
  .section h2{font-size:20px;margin-bottom:16px;padding-bottom:8px;border-bottom:1px solid #334155}
  table{width:100%;border-collapse:collapse}
  th{text-align:left;padding:10px 12px;font-size:12px;text-transform:uppercase;letter-spacing:1px;color:#64748b;border-bottom:1px solid #334155}
  td{padding:10px 12px;border-bottom:1px solid #1e293b;font-size:14px;font-family:'SF Mono',Monaco,monospace}
  .ok{color:#22c55e}.fail{color:#ef4444}
  .meta{color:#94a3b8;font-size:13px;margin-top:32px}
  a{color:#60a5fa;text-decoration:none}
  a:hover{text-decoration:underline}
  pre{font-size:12px;color:#94a3b8;white-space:pre-wrap;max-height:400px;overflow-y:auto}
</style>
</head>
<body>
<h1>Switch Platform — Test Report</h1>
<p class="subtitle">$(date '+%Y-%m-%d %H:%M:%S') · ${DURATION}s</p>
<div class="grid">
  <div class="card">
    <h3>Backend API</h3>
    <span class="number $([ "$BE_PASS" -eq "$BE_TOTAL" ] && echo "green" || echo "red")">$BE_PASS/$BE_TOTAL</span>
    <span class="badge $([ "$BE_PASS" -eq "$BE_TOTAL" ] && echo "pass" || echo "fail")">$([ "$BE_PASS" -eq "$BE_TOTAL" ] && echo "PASS" || echo "FAIL")</span>
  </div>
  <div class="card">
    <h3>Frontend E2E</h3>
    <span class="number $([ "$FE_PASS" -eq "$FE_TOTAL" ] && echo "green" || echo "red")">$FE_PASS/$FE_TOTAL</span>
    <span class="badge $([ "$FE_PASS" -eq "$FE_TOTAL" ] && echo "pass" || echo "fail")">$([ "$FE_PASS" -eq "$FE_TOTAL" ] && echo "PASS" || echo "FAIL")</span>
  </div>
  <div class="card">
    <h3>Total</h3>
    <span class="number blue">$((BE_TOTAL + FE_TOTAL))</span>
    <span style="color:#94a3b8">tests</span>
  </div>
</div>
<div class="section">
  <h2>Backend API Test Results</h2>
  <table>
    <tr><th>Test</th><th>Status</th></tr>
HTML_EOF

while IFS= read -r line; do
  if [[ "$line" =~ ^"  "[^\ ]+ ]]; then
    if echo "$line" | grep -q "✓ PASS"; then
      name=$(echo "$line" | sed -E 's/ {2,}.*$//' | sed 's/^  //')
      echo "<tr><td>${name}</td><td class='ok'>✓ PASS</td></tr>" >> "$REPORT_DIR/index.html"
    elif echo "$line" | grep -q "✗"; then
      name=$(echo "$line" | sed -E 's/ {2,}.*$//' | sed 's/^  //')
      echo "<tr><td>${name}</td><td class='fail'>✗ FAIL</td></tr>" >> "$REPORT_DIR/index.html"
    fi
  fi
done < "$BE_OUT"

cat >> "$REPORT_DIR/index.html" << HTML_EOF
  </table>
</div>
<div class="section">
  <h2>Frontend E2E Test Results</h2>
HTML_EOF

if [ -f "$REPORT_DIR/playwright-report/index.html" ]; then
  echo '<p><a href="playwright-report/index.html">Detailed Playwright Report →</a></p>' >> "$REPORT_DIR/index.html"
fi

echo '<table><tr><th>Test</th><th>Status</th></tr>' >> "$REPORT_DIR/index.html"
while IFS= read -r line; do
  if [[ "$line" =~ ^"  ".*"✓" ]]; then
    name=$(echo "$line" | sed 's/.*✓ //')
    echo "<tr><td>${name}</td><td class='ok'>✓ PASS</td></tr>" >> "$REPORT_DIR/index.html"
  elif echo "$line" | grep -q "✘"; then
    name=$(echo "$line" | sed 's/.*✘ *//' | sed 's/ (.*)//')
    echo "<tr><td>${name}</td><td class='fail'>✗ FAIL</td></tr>" >> "$REPORT_DIR/index.html"
  fi
done < "$FE_OUT"
echo "</table></div>" >> "$REPORT_DIR/index.html"

cat >> "$REPORT_DIR/index.html" << HTML_EOF
<p class="meta">Backend: $BE_PASS/$BE_TOTAL · Frontend: $FE_PASS/$FE_TOTAL · Duration: ${DURATION}s</p>
</body>
</html>
HTML_EOF

echo "✅ Report: $REPORT_DIR/index.html"
echo ""
echo "══════════════════════════════════════════════════"
echo "  Backend API  : $BE_PASS/$BE_TOTAL"
echo "  Frontend E2E : $FE_PASS/$FE_TOTAL"
echo "──────────────────────────────────────────────────"
echo "  TOTAL        : $((BE_TOTAL + FE_TOTAL)) tests"
echo "  Duration     : ${DURATION}s"
echo "══════════════════════════════════════════════════"
