-- ========================================
-- MODULE B: ACQUIRING (Acceptation Commerçants)
-- ========================================

-- Merchants
CREATE TABLE merchants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id VARCHAR(15) NOT NULL UNIQUE,
    merchant_category_code VARCHAR(4),
    legal_name VARCHAR(255) NOT NULL,
    trading_name VARCHAR(255),
    registration_number VARCHAR(50),
    tax_id VARCHAR(50),
    email VARCHAR(255),
    phone VARCHAR(20),
    website VARCHAR(255),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    country_code CHAR(2) DEFAULT 'TN',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_ONBOARDING' CHECK (status IN (
        'PENDING_ONBOARDING', 'ACTIVE', 'SUSPENDED', 'TERMINATED', 'UNDER_REVIEW'
    )),
    risk_level VARCHAR(20) DEFAULT 'STANDARD' CHECK (risk_level IN ('LOW', 'STANDARD', 'MEDIUM', 'HIGH')),
    onboarding_date TIMESTAMP WITH TIME ZONE,
    activation_date TIMESTAMP WITH TIME ZONE,
    termination_date TIMESTAMP WITH TIME ZONE,
    settlement_method VARCHAR(20) DEFAULT 'TARGET' CHECK (settlement_method IN ('TARGET', 'DIRECT', 'NETTING', 'GROSS')),
    settlement_currency CHAR(3) DEFAULT 'TND',
    settlement_account_iban VARCHAR(34),
    settlement_cycle VARCHAR(10) DEFAULT 'D+1' CHECK (settlement_cycle IN ('D', 'D+1', 'D+2', 'WEEKLY', 'BIWEEKLY', 'MONTHLY')),
    mdr_percentage NUMERIC(5,2),
    mdr_fixed_fee NUMERIC(18,3) DEFAULT 0,
    mdr_plan_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Terminals / TPE
CREATE TABLE terminals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    terminal_id VARCHAR(8) NOT NULL UNIQUE,
    serial_number VARCHAR(50) UNIQUE,
    terminal_type VARCHAR(30) CHECK (terminal_type IN (
        'PHYSICAL_TPE', 'SOFT_POS', 'ECOMMERCE', 'MOTO', 'ATM', 'KIOSK', 'MOBILE'
    )),
    manufacturer VARCHAR(100),
    model VARCHAR(100),
    firmware_version VARCHAR(50),
    installation_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN (
        'ACTIVE', 'INACTIVE', 'SUSPENDED', 'RETIRED', 'MALFUNCTION'
    )),
    location_name VARCHAR(255),
    location_address VARCHAR(255),
    city VARCHAR(100),
    country_code CHAR(2),
    contactless_supported BOOLEAN DEFAULT TRUE,
    chip_supported BOOLEAN DEFAULT TRUE,
    mag_stripe_supported BOOLEAN DEFAULT TRUE,
    pin_supported BOOLEAN DEFAULT TRUE,
    supported_card_brands VARCHAR(50)[] DEFAULT ARRAY['VISA','MASTERCART','AMEX'],
    supported_currencies VARCHAR(3)[] DEFAULT ARRAY['TND'],
    encryption_key_id VARCHAR(100),
    last_contact TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Merchant Discount Rate plans
CREATE TABLE mdr_plans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    merchant_id UUID REFERENCES merchants(id),
    card_brand VARCHAR(20),
    card_type VARCHAR(20),
    transaction_type VARCHAR(20) CHECK (transaction_type IN ('PURCHASE', 'REFUND', 'CHARGEBACK', 'WITHDRAWAL')),
    domestic_rate NUMERIC(6,4),
    international_rate NUMERIC(6,4),
    fixed_fee_domestic NUMERIC(18,3) DEFAULT 0,
    fixed_fee_international NUMERIC(18,3) DEFAULT 0,
    currency_code CHAR(3) DEFAULT 'TND',
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Merchant settlement records
CREATE TABLE merchant_settlements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    settlement_date DATE NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'TND',
    total_transactions INTEGER NOT NULL DEFAULT 0,
    total_amount NUMERIC(18,3) NOT NULL DEFAULT 0,
    total_fees NUMERIC(18,3) NOT NULL DEFAULT 0,
    total_commission NUMERIC(18,3) NOT NULL DEFAULT 0,
    net_amount NUMERIC(18,3) NOT NULL DEFAULT 0,
    batch_number VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'CONFIRMED', 'PAID', 'DISPUTED', 'CANCELLED'
    )),
    paid_at TIMESTAMP WITH TIME ZONE,
    payment_reference VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(merchant_id, settlement_date, currency_code)
);

-- Merchant transaction logs
CREATE TABLE merchant_transactions (
    id BIGSERIAL PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    terminal_id UUID REFERENCES terminals(id),
    transaction_id UUID,
    card_brand VARCHAR(20),
    card_type VARCHAR(20),
    amount NUMERIC(18,3) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    fee_amount NUMERIC(18,3) DEFAULT 0,
    commission_amount NUMERIC(18,3) DEFAULT 0,
    mdr_rate NUMERIC(6,4),
    transaction_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_merchants_status ON merchants(status);
CREATE INDEX idx_merchants_mcc ON merchants(merchant_category_code);
CREATE INDEX idx_terminals_merchant ON terminals(merchant_id);
CREATE INDEX idx_terminals_status ON terminals(status);
CREATE INDEX idx_mdr_plans_merchant ON mdr_plans(merchant_id);
CREATE INDEX idx_mdr_plans_effective ON mdr_plans(effective_from, effective_to);
CREATE INDEX idx_merchant_settlements_date ON merchant_settlements(settlement_date, merchant_id);
CREATE INDEX idx_merchant_transactions_date ON merchant_transactions(transaction_date);
