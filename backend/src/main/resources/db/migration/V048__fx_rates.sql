CREATE TABLE fx_rates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_currency CHAR(3) NOT NULL,
    target_currency CHAR(3) NOT NULL,
    rate NUMERIC(18,8) NOT NULL,
    margin_percentage NUMERIC(8,4) NOT NULL DEFAULT 0,
    effective_date DATE NOT NULL,
    expiry_date DATE,
    source VARCHAR(50) DEFAULT 'MANUAL',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fx_rates_pair ON fx_rates(source_currency, target_currency, effective_date);
