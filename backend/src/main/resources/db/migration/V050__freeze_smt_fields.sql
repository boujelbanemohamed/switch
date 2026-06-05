ALTER TABLE clearing_records ADD COLUMN IF NOT EXISTS card_brand VARCHAR(20);
ALTER TABLE clearing_records ADD COLUMN IF NOT EXISTS trading_name VARCHAR(255);
ALTER TABLE clearing_records ADD COLUMN IF NOT EXISTS slip_number VARCHAR(6);
ALTER TABLE clearing_records ADD COLUMN IF NOT EXISTS representation_flag BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE participants ADD COLUMN IF NOT EXISTS code_faconnier VARCHAR(6) DEFAULT '222222';
