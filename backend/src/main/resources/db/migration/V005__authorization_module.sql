-- ========================================
-- MODULE C: AUTHORIZATION (Décision Temps Réel)
-- ========================================

-- Authorization rules engine
CREATE TABLE auth_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    rule_type VARCHAR(30) NOT NULL CHECK (rule_type IN (
        'SOLDE', 'LIMIT', 'VELOCITY', 'FRAUD', 'RISK', 'MERCHANT', 'PRODUCT', 'GEO', 'TIME', 'CUSTOM'
    )),
    priority INTEGER NOT NULL DEFAULT 100,
    action VARCHAR(20) NOT NULL CHECK (action IN ('APPROVE', 'DECLINE', 'CHALLENGE', 'REVIEW', '2FA', 'PIN')),
    response_code VARCHAR(2),
    condition_expression JSONB NOT NULL,
    card_type VARCHAR(20),
    card_brand VARCHAR(20),
    merchant_category VARCHAR(4),
    country_code VARCHAR(2),
    time_start TIME,
    time_end TIME,
    day_of_week VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'TEST')),
    apply_to_all BOOLEAN DEFAULT FALSE,
    failure_count INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Authorization decisions log
CREATE TABLE auth_decisions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(64) NOT NULL,
    card_id UUID REFERENCES cards(id),
    cardholder_id UUID REFERENCES cardholders(id),
    merchant_id VARCHAR(15),
    terminal_id VARCHAR(8),
    amount NUMERIC(18,3) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    mti VARCHAR(4),
    stan VARCHAR(12),
    rrn VARCHAR(12),
    pan_hash VARCHAR(64),
    decision VARCHAR(20) NOT NULL CHECK (decision IN ('APPROVED', 'DECLINED', 'CHALLENGED', 'REVIEW', 'TIMEOUT', 'ERROR')),
    response_code VARCHAR(2),
    response_reason VARCHAR(255),
    rule_id UUID REFERENCES auth_rules(id),
    rule_name VARCHAR(255),
    processing_time_ms INTEGER,
    card_balance_before NUMERIC(18,3),
    card_balance_after NUMERIC(18,3),
    fraud_score INTEGER,
    risk_score INTEGER,
    velocity_used INTEGER,
    velocity_max INTEGER,
    limit_used NUMERIC(18,3),
    limit_max NUMERIC(18,3),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    decided_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Card limits (daily/weekly/monthly tracking)
CREATE TABLE card_limits_usage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    card_id UUID NOT NULL REFERENCES cards(id),
    limit_type VARCHAR(20) NOT NULL CHECK (limit_type IN ('DAILY', 'WEEKLY', 'MONTHLY', 'SINGLE', 'CONSECUTIVE')),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    limit_amount NUMERIC(18,3) NOT NULL,
    used_amount NUMERIC(18,3) NOT NULL DEFAULT 0,
    count_used INTEGER DEFAULT 0,
    count_max INTEGER,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'TND',
    reset_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(card_id, limit_type, period_start)
);

-- Velocity checks
CREATE TABLE velocity_checks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    card_id UUID NOT NULL REFERENCES cards(id),
    velocity_type VARCHAR(30) NOT NULL CHECK (velocity_type IN (
        'TXNS_PER_HOUR', 'TXNS_PER_DAY', 'AMOUNT_PER_HOUR', 'AMOUNT_PER_DAY',
        'ATM_PER_DAY', 'ECOMM_PER_DAY', 'CONTACTLESS_PER_DAY', 'PIN_FAILURES',
        'MERCHANT_PER_DAY', 'COUNTRY_PER_DAY'
    )),
    window_start TIMESTAMP WITH TIME ZONE NOT NULL,
    window_end TIMESTAMP WITH TIME ZONE NOT NULL,
    current_count INTEGER DEFAULT 0,
    current_amount NUMERIC(18,3) DEFAULT 0,
    max_count INTEGER,
    max_amount NUMERIC(18,3),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(card_id, velocity_type, window_start)
);

-- Indexes
CREATE INDEX idx_auth_rules_priority ON auth_rules(priority, status);
CREATE INDEX idx_auth_rules_type ON auth_rules(rule_type);
CREATE INDEX idx_auth_decisions_transaction ON auth_decisions(transaction_id);
CREATE INDEX idx_auth_decisions_card ON auth_decisions(card_id);
CREATE INDEX idx_auth_decisions_time ON auth_decisions(requested_at);
CREATE INDEX idx_card_limits_card ON card_limits_usage(card_id);
CREATE INDEX idx_velocity_card ON velocity_checks(card_id);
