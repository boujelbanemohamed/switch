#!/usr/bin/env bash
# =============================================================
# seed-clearing-demo.sh
# Purpose:  Inject demo clearing records into the database for
#           manual smoke-testing of BCT, reconciliation,
#           quarterly reports, and network format generation.
#
# Usage:    ./scripts/seed-clearing-demo.sh
#
# Effect:   Creates 6 test TST-* clearing records between 3
#           domestic banks (BNA, BH, ATB) plus UIB, with
#           asymmetric amounts to verify BCT equilibrium.
#           Also adds a PROC_EU (foreign) record to verify
#           exclusion logic.
#
# Cleanup:  DELETE FROM clearing_records WHERE transaction_id
#           LIKE 'TST-%';
# =============================================================
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-switch_db}"
DB_USER="${DB_USER:-switch_user}"
DB_PASS="${DB_PASS:-switch_pass}"

PSQL="psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME"

echo "=== Clearing demo seed ==="
echo "  DB: $DB_HOST:$DB_PORT/$DB_NAME"

# ---- Clear any previous test records ----
PGPASSWORD="$DB_PASS" $PSQL <<SQL
DELETE FROM clearing_records WHERE transaction_id LIKE 'TST-%';
DELETE FROM netting_records WHERE netting_date = '2026-06-05';
SQL

# ---- Insert asymmetric demo records ----
PGPASSWORD="$DB_PASS" $PSQL <<SQL
-- BNA acquires 5000, BH issues → BNA should receive 5000 from BH
INSERT INTO clearing_records
    (id, clearing_date, batch_number, transaction_id,
     acquiring_participant_id, issuing_participant_id,
     amount, net_amount, currency_code, status, created_at)
SELECT gen_random_uuid(), '2026-06-05', 'SEED-DEMO', 'TST-BNA-BH-A',
       p1.id, p2.id, 5000, 5000, 'TND', 'CLEARED', NOW()
FROM participants p1, participants p2
WHERE p1.code='BNA' AND p2.code='BH';

-- BH acquires 3500, ATB issues → BH should receive 3500 from ATB
INSERT INTO clearing_records
    (id, clearing_date, batch_number, transaction_id,
     acquiring_participant_id, issuing_participant_id,
     amount, net_amount, currency_code, status, created_at)
SELECT gen_random_uuid(), '2026-06-05', 'SEED-DEMO', 'TST-BH-ATB-A',
       p1.id, p2.id, 3500, 3500, 'TND', 'CLEARED', NOW()
FROM participants p1, participants p2
WHERE p1.code='BH' AND p2.code='ATB';

-- ATB acquires 2000, BNA issues → ATB should receive 2000 from BNA
INSERT INTO clearing_records
    (id, clearing_date, batch_number, transaction_id,
     acquiring_participant_id, issuing_participant_id,
     amount, net_amount, currency_code, status, created_at)
SELECT gen_random_uuid(), '2026-06-05', 'SEED-DEMO', 'TST-ATB-BNA-A',
       p1.id, p2.id, 2000, 2000, 'TND', 'CLEARED', NOW()
FROM participants p1, participants p2
WHERE p1.code='ATB' AND p2.code='BNA';

-- UIB acquires 1500, BNA issues → UIB should receive 1500 from BNA
INSERT INTO clearing_records
    (id, clearing_date, batch_number, transaction_id,
     acquiring_participant_id, issuing_participant_id,
     amount, net_amount, currency_code, status, created_at)
SELECT gen_random_uuid(), '2026-06-05', 'SEED-DEMO', 'TST-UIB-BNA-A',
       p1.id, p2.id, 1500, 1500, 'TND', 'CLEARED', NOW()
FROM participants p1, participants p2
WHERE p1.code='UIB' AND p2.code='BNA';

-- BNA acquires 1200, UIB issues → BNA should receive 1200 from UIB
INSERT INTO clearing_records
    (id, clearing_date, batch_number, transaction_id,
     acquiring_participant_id, issuing_participant_id,
     amount, net_amount, currency_code, status, created_at)
SELECT gen_random_uuid(), '2026-06-05', 'SEED-DEMO', 'TST-BNA-UIB-A',
       p1.id, p2.id, 1200, 1200, 'TND', 'CLEARED', NOW()
FROM participants p1, participants p2
WHERE p1.code='BNA' AND p2.code='UIB';

-- ATB acquires 800, UIB issues → ATB should receive 800 from UIB
INSERT INTO clearing_records
    (id, clearing_date, batch_number, transaction_id,
     acquiring_participant_id, issuing_participant_id,
     amount, net_amount, currency_code, status, created_at)
SELECT gen_random_uuid(), '2026-06-05', 'SEED-DEMO', 'TST-ATB-UIB-A',
       p1.id, p2.id, 800, 800, 'TND', 'CLEARED', NOW()
FROM participants p1, participants p2
WHERE p1.code='ATB' AND p2.code='UIB';

-- PROC_EU (foreign) acquires 99999, BNA issues → excluded from BCT
INSERT INTO clearing_records
    (id, clearing_date, batch_number, transaction_id,
     acquiring_participant_id, issuing_participant_id,
     amount, net_amount, currency_code, status, created_at)
SELECT gen_random_uuid(), '2026-06-05', 'SEED-DEMO', 'TST-FOREIGN-EU',
       p1.id, p2.id, 99999, 99999, 'TND', 'CLEARED', NOW()
FROM participants p1, participants p2
WHERE p1.code='PROC_EU' AND p2.code='BNA';
SQL

# ---- Verify ----
echo ""
echo "=== Inserted records ==="
PGPASSWORD="$DB_PASS" $PSQL <<SQL
SELECT transaction_id, amount, net_amount, status
FROM clearing_records
WHERE transaction_id LIKE 'TST-%'
ORDER BY transaction_id;
SQL

echo ""
echo "=== Expected BCT equilibrium ==="
echo "  BNA: receives 5000+1200=6200, pays 2000+1500=3500, net=+2700"
echo "   BH: receives 3500, pays 5000, net=-1500"
echo "  ATB: receives 2000+800=2800, pays 3500, net=-700"
echo "  UIB: receives 1500, pays 1200+800=2000, net=-500"
echo "  Sum: $((2700-1500-700-500)) = 0 (equilibrium)"
echo ""
echo "To recalculate netting + generate BCT:"
echo "  curl -X POST 'http://localhost:8085/api/v1/clearing/netting/calculate?date=2026-06-05'"
echo "  curl 'http://localhost:8085/api/v1/clearing/files/bct?date=2026-06-05'"
