CREATE TABLE IF NOT EXISTS virtual_cards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    funding_card_id UUID REFERENCES cards(id),
    card_product_id UUID REFERENCES card_products(id),
    cardholder_id UUID NOT NULL REFERENCES cardholders(id),
    external_id VARCHAR(64) UNIQUE,
    pan_hash VARCHAR(64) NOT NULL,
    pan_suffix VARCHAR(4) NOT NULL,
    expiry_date DATE NOT NULL,
    cvv_hash VARCHAR(64),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_ACTIVATION' CHECK (status IN (
        'PENDING_ACTIVATION','ACTIVE','SUSPENDED','EXPIRED','CONSUMED','CANCELLED'
    )),
    usage_type VARCHAR(20) NOT NULL CHECK (usage_type IN (
        'SINGLE_USE','MULTI_USE','RECURRING'
    )),
    name_on_card VARCHAR(128),
    amount_limit NUMERIC(18,3),
    amount_used NUMERIC(18,3) DEFAULT 0,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'TND',
    merchant_locked VARCHAR(256),
    merchant_category VARCHAR(4),
    mcc_locks TEXT,
    max_transactions INTEGER,
    transaction_count INTEGER DEFAULT 0,
    single_use_amount NUMERIC(18,3),
    expires_at TIMESTAMP WITH TIME ZONE,
    activated_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancel_reason VARCHAR(255),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_virtual_cards_status ON virtual_cards(status);
CREATE INDEX IF NOT EXISTS idx_virtual_cards_cardholder ON virtual_cards(cardholder_id);
CREATE INDEX IF NOT EXISTS idx_virtual_cards_funding ON virtual_cards(funding_card_id);
CREATE INDEX IF NOT EXISTS idx_virtual_cards_external ON virtual_cards(external_id);
