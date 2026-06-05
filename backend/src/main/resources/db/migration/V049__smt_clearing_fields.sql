ALTER TABLE participants ADD COLUMN IF NOT EXISTS bank_code VARCHAR(5);

ALTER TABLE clearing_records ADD COLUMN IF NOT EXISTS merchant_number VARCHAR(10);
ALTER TABLE clearing_records ADD COLUMN IF NOT EXISTS card_number VARCHAR(19);
ALTER TABLE clearing_records ADD COLUMN IF NOT EXISTS mcc VARCHAR(4);
ALTER TABLE clearing_records ADD COLUMN IF NOT EXISTS authorization_number VARCHAR(6);
ALTER TABLE clearing_records ADD COLUMN IF NOT EXISTS origin_identifier VARCHAR(1);
ALTER TABLE clearing_records ADD COLUMN IF NOT EXISTS operation_nature VARCHAR(1);
ALTER TABLE clearing_records ADD COLUMN IF NOT EXISTS operation_code VARCHAR(2);
ALTER TABLE clearing_records ADD COLUMN IF NOT EXISTS archive_reference VARCHAR(23);
