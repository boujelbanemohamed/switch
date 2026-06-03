CREATE TABLE IF NOT EXISTS refresh_token_blacklist (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    username VARCHAR(64) NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_rtb_token_hash ON refresh_token_blacklist(token_hash);
CREATE INDEX IF NOT EXISTS idx_rtb_expires_at ON refresh_token_blacklist(expires_at);
