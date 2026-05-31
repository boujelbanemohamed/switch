ALTER TABLE merchants DROP CONSTRAINT IF EXISTS merchants_settlement_cycle_check;
ALTER TABLE merchants ADD CONSTRAINT merchants_settlement_cycle_check
    CHECK (settlement_cycle IN ('D', 'D1', 'D2', 'WEEKLY', 'BIWEEKLY', 'MONTHLY'));
