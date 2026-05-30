CREATE TABLE pin_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id UUID NOT NULL,
    pin_hash TEXT,
    pin_block_hash TEXT,
    algorithm VARCHAR(32) NOT NULL DEFAULT 'PBKDF2',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE token_vault (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id UUID NOT NULL,
    fpan_suffix VARCHAR(4),
    dpan VARCHAR(19) UNIQUE NOT NULL,
    dpan_suffix VARCHAR(4) NOT NULL,
    wallet_provider VARCHAR(32) NOT NULL,
    device_id VARCHAR(128),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_pin_records_card ON pin_records(card_id);
CREATE INDEX idx_token_vault_card ON token_vault(card_id);
CREATE INDEX idx_token_vault_dpan ON token_vault(dpan);
