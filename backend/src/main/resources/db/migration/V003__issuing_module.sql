-- ========================================
-- MODULE A: ISSUING (Émission Cartes/Wallets)
-- ========================================

-- Cardholders
CREATE TABLE cardholders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    external_id VARCHAR(64) UNIQUE,
    title VARCHAR(10),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20),
    mobile VARCHAR(20),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    country_code VARCHAR(2) DEFAULT 'TN',
    nationality VARCHAR(2),
    id_document_type VARCHAR(20) CHECK (id_document_type IN ('PASSPORT', 'NATIONAL_ID', 'DRIVING_LICENSE', 'RESIDENCE')),
    id_document_number VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'DECEASED')),
    kyc_level INTEGER DEFAULT 1 CHECK (kyc_level BETWEEN 1 AND 5),
    risk_profile VARCHAR(20) DEFAULT 'STANDARD' CHECK (risk_profile IN ('LOW', 'STANDARD', 'MEDIUM', 'HIGH', 'VERY_HIGH')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Card accounts
CREATE TABLE card_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cardholder_id UUID NOT NULL REFERENCES cardholders(id),
    account_number VARCHAR(34) NOT NULL UNIQUE,
    iban VARCHAR(34),
    account_type VARCHAR(20) NOT NULL CHECK (account_type IN ('CHECKING', 'SAVINGS', 'PREPAID', 'CREDIT', 'LOAN')),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'TND',
    balance NUMERIC(18,3) NOT NULL DEFAULT 0,
    ledger_balance NUMERIC(18,3) NOT NULL DEFAULT 0,
    available_balance NUMERIC(18,3) NOT NULL DEFAULT 0,
    hold_amount NUMERIC(18,3) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'CLOSED')),
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Cards
CREATE TABLE cards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cardholder_id UUID NOT NULL REFERENCES cardholders(id),
    card_account_id UUID REFERENCES card_accounts(id),
    card_number_hash VARCHAR(64) NOT NULL,
    card_number_suffix VARCHAR(4) NOT NULL,
    card_type VARCHAR(20) NOT NULL CHECK (card_type IN ('DEBIT', 'CREDIT', 'PREPAID', 'CHARGE', 'VIRTUAL')),
    card_brand VARCHAR(20) NOT NULL CHECK (card_brand IN ('VISA', 'MASTERCARD', 'AMEX', 'CB', 'VERVE', 'OTHER')),
    card_network VARCHAR(20) CHECK (card_network IN ('VISA_NET', 'MASTERCARD_NET', 'CB_NET', 'AMEX_NET', 'VERVE_NET')),
    product_code VARCHAR(20),
    embossed_line1 VARCHAR(255),
    embossed_line2 VARCHAR(255),
    expiry_date DATE NOT NULL,
    cvv_hash VARCHAR(64),
    pin_block VARCHAR(128),
    pin_attempts INTEGER DEFAULT 0,
    pin_max_attempts INTEGER DEFAULT 3,
    pin_last_updated TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_ACTIVATION' CHECK (status IN (
        'PENDING_ACTIVATION', 'ACTIVE', 'INACTIVE', 'BLOCKED', 'SUSPENDED',
        'EXPIRED', 'STOLEN', 'LOST', 'DAMAGED', 'CLOSED', 'RENEWED'
    )),
    status_reason VARCHAR(255),
    activation_date TIMESTAMP WITH TIME ZONE,
    block_date TIMESTAMP WITH TIME ZONE,
    renewal_date TIMESTAMP WITH TIME ZONE,
    expiry_notification_sent BOOLEAN DEFAULT FALSE,
    is_renewable BOOLEAN DEFAULT TRUE,
    is_reissuable BOOLEAN DEFAULT TRUE,
    reissue_reason VARCHAR(50),
    embossed BOOLEAN DEFAULT TRUE,
    contactless_enabled BOOLEAN DEFAULT TRUE,
    online_enabled BOOLEAN DEFAULT TRUE,
    international_enabled BOOLEAN DEFAULT TRUE,
    ecommerce_enabled BOOLEAN DEFAULT TRUE,
    atm_enabled BOOLEAN DEFAULT TRUE,
    mag_stripe_enabled BOOLEAN DEFAULT TRUE,
    chip_enabled BOOLEAN DEFAULT TRUE,
    daily_limit NUMERIC(18,3),
    weekly_limit NUMERIC(18,3),
    monthly_limit NUMERIC(18,3),
    single_txn_limit NUMERIC(18,3),
    requestor_reference VARCHAR(35),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Card operations log
CREATE TABLE card_operations (
    id BIGSERIAL PRIMARY KEY,
    card_id UUID NOT NULL REFERENCES cards(id),
    operation_type VARCHAR(50) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    reason VARCHAR(255),
    performed_by VARCHAR(100),
    ip_address VARCHAR(45),
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Wallet tokens
CREATE TABLE wallet_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    card_id UUID NOT NULL REFERENCES cards(id),
    token VARCHAR(64) NOT NULL UNIQUE,
    token_type VARCHAR(20) NOT NULL CHECK (token_type IN ('DEVICE', 'SERVER', 'MERCHANT')),
    wallet_provider VARCHAR(20) CHECK (wallet_provider IN ('APPLE_PAY', 'GOOGLE_PAY', 'SAMSUNG_PAY', 'OTHER')),
    device_id VARCHAR(100),
    device_name VARCHAR(255),
    cryptogram VARCHAR(128),
    token_expiry DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'TERMINATED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- PIN management
CREATE TABLE pin_management (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    card_id UUID NOT NULL REFERENCES cards(id),
    pin_type VARCHAR(20) NOT NULL CHECK (pin_type IN ('TRANSACTION', 'ADMIN', 'PHONE')),
    pin_block VARCHAR(128) NOT NULL,
    pin_format VARCHAR(10) DEFAULT 'ISO9564-1',
    pin_attempts INTEGER DEFAULT 0,
    max_attempts INTEGER DEFAULT 3,
    last_attempt TIMESTAMP WITH TIME ZONE,
    blocked_until TIMESTAMP WITH TIME ZONE,
    last_changed TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_cards_holder ON cards(cardholder_id);
CREATE INDEX idx_cards_account ON cards(card_account_id);
CREATE INDEX idx_cards_status ON cards(status);
CREATE INDEX idx_cards_brand ON cards(card_brand);
CREATE INDEX idx_cards_suffix ON cards(card_number_suffix);
CREATE INDEX idx_card_operations_card ON card_operations(card_id);
CREATE INDEX idx_card_operations_type ON card_operations(operation_type);
CREATE INDEX idx_wallet_tokens_card ON wallet_tokens(card_id);
CREATE INDEX idx_wallet_tokens_provider ON wallet_tokens(wallet_provider);
CREATE INDEX idx_card_accounts_holder ON card_accounts(cardholder_id);
CREATE INDEX idx_cardholders_email ON cardholders(email);

-- Extended cardholder KYC table
CREATE TABLE kyc_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cardholder_id UUID NOT NULL REFERENCES cardholders(id),
    document_type VARCHAR(30) NOT NULL,
    document_number VARCHAR(50),
    issuing_country VARCHAR(2),
    expiry_date DATE,
    file_path VARCHAR(512),
    verification_status VARCHAR(20) DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED')),
    verified_by VARCHAR(100),
    verified_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
