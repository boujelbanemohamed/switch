CREATE TABLE settlement_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL,
    settlement_date DATE NOT NULL,
    total_amount DECIMAL(19,4) NOT NULL,
    total_fee DECIMAL(19,4) NOT NULL DEFAULT 0,
    net_amount DECIMAL(19,4) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    payment_ref VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    confirmed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_settlement_merchant ON settlement_records(merchant_id);
CREATE INDEX idx_settlement_date ON settlement_records(settlement_date);
