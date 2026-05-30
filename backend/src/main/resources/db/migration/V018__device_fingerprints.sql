CREATE TABLE device_fingerprints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id UUID NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    device_type VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
    os VARCHAR(64),
    browser VARCHAR(64),
    user_agent TEXT,
    ip_address VARCHAR(45),
    country VARCHAR(2),
    attributes JSONB,
    usage_count INTEGER NOT NULL DEFAULT 1,
    first_seen TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_seen TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_device_fp_card ON device_fingerprints(card_id);
CREATE INDEX idx_device_fp_device ON device_fingerprints(device_id);
CREATE UNIQUE INDEX idx_device_fp_unique ON device_fingerprints(card_id, device_id);
