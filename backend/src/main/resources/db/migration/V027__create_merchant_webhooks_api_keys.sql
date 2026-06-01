-- Merchant webhook configuration
CREATE TABLE IF NOT EXISTS merchant_webhooks (
    id UUID PRIMARY KEY,
    merchant_code VARCHAR(15) NOT NULL,
    url VARCHAR(512) NOT NULL,
    event_types TEXT,
    secret VARCHAR(128),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mw_merchant_code ON merchant_webhooks(merchant_code);

-- Merchant API keys
CREATE TABLE IF NOT EXISTS merchant_api_keys (
    id UUID PRIMARY KEY,
    merchant_code VARCHAR(15) NOT NULL,
    api_key VARCHAR(64) NOT NULL UNIQUE,
    label VARCHAR(100),
    permissions VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mak_merchant_code ON merchant_api_keys(merchant_code);
CREATE INDEX IF NOT EXISTS idx_mak_api_key ON merchant_api_keys(api_key);
