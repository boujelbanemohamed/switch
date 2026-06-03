CREATE TABLE IF NOT EXISTS card_programs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(128) NOT NULL,
    description TEXT,
    program_type VARCHAR(30) NOT NULL CHECK (program_type IN (
        'CONSUMER','CORPORATE','STUDENT','PREMIUM','PLATINUM','SIGNATURE','BUSINESS','CLASSIC'
    )),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN (
        'DRAFT','ACTIVE','INACTIVE','ARCHIVED'
    )),
    brand VARCHAR(20),
    start_date DATE,
    end_date DATE,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS card_products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    program_id UUID NOT NULL REFERENCES card_programs(id),
    name VARCHAR(128) NOT NULL,
    description TEXT,
    product_code VARCHAR(20) NOT NULL UNIQUE,
    card_type VARCHAR(20) NOT NULL CHECK (card_type IN ('DEBIT','CREDIT','PREPAID','CHARGE','VIRTUAL')),
    card_brand VARCHAR(20) NOT NULL CHECK (card_brand IN ('VISA','MASTERCARD','AMEX','CB','VERVE','OTHER')),
    card_network VARCHAR(20) CHECK (card_network IN ('VISA_NET','MASTERCARD_NET','CB_NET','AMEX_NET','VERVE_NET')),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN (
        'DRAFT','ACTIVE','INACTIVE','ARCHIVED'
    )),
    contactless_enabled BOOLEAN DEFAULT TRUE,
    online_enabled BOOLEAN DEFAULT TRUE,
    international_enabled BOOLEAN DEFAULT FALSE,
    ecommerce_enabled BOOLEAN DEFAULT TRUE,
    atm_enabled BOOLEAN DEFAULT TRUE,
    mag_stripe_enabled BOOLEAN DEFAULT TRUE,
    chip_enabled BOOLEAN DEFAULT TRUE,
    is_renewable BOOLEAN DEFAULT TRUE,
    is_reissuable BOOLEAN DEFAULT TRUE,
    is_virtual_supported BOOLEAN DEFAULT TRUE,
    daily_limit NUMERIC(18,3),
    weekly_limit NUMERIC(18,3),
    monthly_limit NUMERIC(18,3),
    single_txn_limit NUMERIC(18,3),
    annual_fee NUMERIC(18,3) DEFAULT 0,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'TND',
    features JSONB,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_card_products_program ON card_products(program_id);
CREATE INDEX IF NOT EXISTS idx_card_products_brand_type ON card_products(card_brand, card_type);
CREATE INDEX IF NOT EXISTS idx_card_products_code ON card_products(product_code);
CREATE INDEX IF NOT EXISTS idx_card_programs_status ON card_programs(status);
