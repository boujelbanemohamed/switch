-- ========================================
-- MODULE H: E-COMMERCE (ACS, EPG, 3DSS)
-- ========================================

-- ----------------------------------------
-- ACS: Access Control Server (3D Secure)
-- ----------------------------------------
CREATE TABLE acs_authentications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id VARCHAR(64) NOT NULL,
    card_id UUID REFERENCES cards(id),
    merchant_id UUID REFERENCES merchants(id),
    merchant_name VARCHAR(128),
    amount NUMERIC(18,3) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    pan_hash VARCHAR(128),
    card_type VARCHAR(20),
    card_brand VARCHAR(20),
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED' CHECK (status IN (
        'CREATED', 'CHALLENGE_REQUIRED', 'AUTHENTICATED', 'FAILED', 'DECLINED', 'EXPIRED'
    )),
    authentication_value VARCHAR(128),
    eci VARCHAR(2),
    ds_trans_id UUID,
    three_ds_version VARCHAR(10),
    ds_url VARCHAR(512),
    acs_url VARCHAR(512),
    challenge_canceled BOOLEAN DEFAULT FALSE,
    whitelist_status VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE acs_challenges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    authentication_id UUID NOT NULL REFERENCES acs_authentications(id),
    challenge_type VARCHAR(20) NOT NULL CHECK (challenge_type IN (
        'OTP', 'SMS', 'EMAIL', 'BIOMETRIC', 'APP_NOTIFICATION', 'PASSWORD'
    )),
    challenge_data TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'SENT', 'VERIFIED', 'FAILED', 'EXPIRED'
    )),
    attempts INTEGER DEFAULT 0,
    max_attempts INTEGER DEFAULT 3,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ----------------------------------------
-- EPG: Electronic Payment Gateway
-- ----------------------------------------
CREATE TABLE epg_merchant_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    api_key_hash VARCHAR(256) NOT NULL,
    api_secret_hash VARCHAR(256) NOT NULL,
    webhook_url VARCHAR(512),
    callback_url VARCHAR(512),
    allowed_currencies VARCHAR(3)[] DEFAULT '{}',
    allowed_card_brands VARCHAR(20)[] DEFAULT '{}',
    min_amount NUMERIC(18,3) DEFAULT 0,
    max_amount NUMERIC(18,3),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE epg_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    merchant_transaction_id VARCHAR(128) NOT NULL,
    amount NUMERIC(18,3) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    pan_hash VARCHAR(128),
    card_type VARCHAR(20),
    card_brand VARCHAR(20),
    cardholder_name VARCHAR(128),
    customer_email VARCHAR(256),
    customer_phone VARCHAR(32),
    customer_ip VARCHAR(45),
    user_agent TEXT,
    device_channel VARCHAR(10) DEFAULT 'WEB' CHECK (device_channel IN (
        'WEB', 'MOBILE', 'TABLET', 'API'
    )),
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED' CHECK (status IN (
        'INITIATED', 'AUTHENTICATED', 'AUTHORIZED', 'CAPTURED',
        'FAILED', 'REFUNDED', 'CHARGEBACK', 'CANCELED'
    )),
    three_ds_status VARCHAR(30),
    three_ds_required BOOLEAN DEFAULT FALSE,
    cavv VARCHAR(128),
    eci VARCHAR(2),
    xid VARCHAR(64),
    acs_transaction_id UUID REFERENCES acs_authentications(id),
    error_code VARCHAR(10),
    error_description TEXT,
    authorized_at TIMESTAMP WITH TIME ZONE,
    captured_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(merchant_id, merchant_transaction_id)
);

-- ----------------------------------------
-- 3DSS: 3D Secure Server
-- ----------------------------------------
CREATE TABLE three_ds_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    epg_transaction_id UUID REFERENCES epg_transactions(id),
    transaction_id VARCHAR(64) NOT NULL,
    card_id UUID REFERENCES cards(id),
    three_ds_version VARCHAR(10) NOT NULL DEFAULT '2.2.0',
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED' CHECK (status IN (
        'CREATED', 'AUTH_REQ_SENT', 'AUTH_REQ_RECEIVED',
        'CHALLENGE_SENT', 'CHALLENGE_RECEIVED', 'COMPLETED', 'ERROR', 'TIMEOUT'
    )),
    authentication_type VARCHAR(20) DEFAULT 'PAYMENT',
    acs_reference_number VARCHAR(64),
    ds_reference_number VARCHAR(64),
    acs_url VARCHAR(512),
    term_url VARCHAR(512),
    notification_url VARCHAR(512),
    creq TEXT,
    cres TEXT,
    authentication_value VARCHAR(128),
    eci VARCHAR(2),
    ds_trans_id VARCHAR(64),
    acs_trans_id VARCHAR(64),
    sdk_trans_id VARCHAR(64),
    three_ds_method_data TEXT,
    three_ds_method_url VARCHAR(512),
    challenge_request TEXT,
    challenge_response TEXT,
    device_info TEXT,
    browser_accept_header VARCHAR(512),
    browser_ip VARCHAR(45),
    browser_language VARCHAR(10),
    browser_color_depth VARCHAR(10),
    browser_screen_height INTEGER,
    browser_screen_width INTEGER,
    browser_timezone_offset INTEGER,
    browser_user_agent TEXT,
    error_description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);

-- ----------------------------------------
-- Indexes
-- ----------------------------------------
CREATE INDEX idx_acs_auth_transaction ON acs_authentications(transaction_id);
CREATE INDEX idx_acs_auth_card ON acs_authentications(card_id);
CREATE INDEX idx_acs_auth_status ON acs_authentications(status);
CREATE INDEX idx_acs_challenge_auth ON acs_challenges(authentication_id);
CREATE INDEX idx_epg_txn_merchant ON epg_transactions(merchant_id);
CREATE INDEX idx_epg_txn_status ON epg_transactions(status);
CREATE INDEX idx_epg_txn_created ON epg_transactions(created_at);
CREATE INDEX idx_epg_config_merchant ON epg_merchant_configs(merchant_id);
CREATE INDEX idx_three_ds_txn ON three_ds_sessions(transaction_id);
CREATE INDEX idx_three_ds_epg ON three_ds_sessions(epg_transaction_id);
