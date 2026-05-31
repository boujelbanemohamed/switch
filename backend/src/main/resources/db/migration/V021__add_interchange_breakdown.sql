ALTER TABLE clearing_records ADD COLUMN IF NOT EXISTS interchange_breakdown TEXT;
ALTER TABLE clearing_records ADD COLUMN IF NOT EXISTS interchange_fee NUMERIC(18,3) DEFAULT 0;
