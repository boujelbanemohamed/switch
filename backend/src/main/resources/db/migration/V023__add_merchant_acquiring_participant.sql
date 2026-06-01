ALTER TABLE merchants ADD COLUMN IF NOT EXISTS acquiring_participant_id UUID REFERENCES participants(id);
CREATE INDEX IF NOT EXISTS idx_merchants_acquiring_participant ON merchants(acquiring_participant_id);
