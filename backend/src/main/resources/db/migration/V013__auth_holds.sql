CREATE TABLE auth_holds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id VARCHAR(64) NOT NULL,
    card_id UUID NOT NULL,
    card_account_id UUID NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    released_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_auth_holds_card ON auth_holds(card_id);
CREATE INDEX idx_auth_holds_account ON auth_holds(card_account_id);
CREATE INDEX idx_auth_holds_status ON auth_holds(status);
CREATE INDEX idx_auth_holds_transaction ON auth_holds(transaction_id);
