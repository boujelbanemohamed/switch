ALTER TABLE loyalty_programs ADD COLUMN point_value NUMERIC(10,6) NOT NULL DEFAULT 0.01;

ALTER TABLE loyalty_transactions ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE loyalty_transactions ADD COLUMN remaining NUMERIC(18,3);

INSERT INTO ledger_accounts (account_number, account_type, currency, label)
VALUES ('LOYALTY_POINTS_LIABILITY', 'LIABILITY', 'TND', 'Provisions points de fidélité');

INSERT INTO ledger_accounts (account_number, account_type, currency, label)
VALUES ('LOYALTY_EXPENSE', 'EXPENSE', 'TND', 'Coût programme de fidélité (points gagnés)');

UPDATE loyalty_programs SET point_value = 0.01 WHERE point_value IS NULL OR point_value = 0;
