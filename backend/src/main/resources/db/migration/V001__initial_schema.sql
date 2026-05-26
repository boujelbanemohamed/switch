CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Network participants (acquirers, issuers, switches)
CREATE TABLE participants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('ACQUIRER', 'ISSUER', 'SWITCH', 'PROCESSOR')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    endpoint_url VARCHAR(512),
    endpoint_type VARCHAR(20) CHECK (endpoint_type IN ('TCP', 'HTTP', 'MQ', 'FILE')),
    supported_protocols VARCHAR(50)[] DEFAULT '{}',
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- BIN tables (Bank Identification Number)
CREATE TABLE bin_tables (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    bin VARCHAR(19) NOT NULL,
    bin_length INTEGER NOT NULL DEFAULT 6,
    participant_id UUID NOT NULL REFERENCES participants(id),
    card_brand VARCHAR(20) CHECK (card_brand IN ('VISA', 'MASTERCARD', 'AMEX', 'CB', 'OTHER')),
    card_type VARCHAR(20) CHECK (card_type IN ('CREDIT', 'DEBIT', 'PREPAID', 'CHARGE')),
    country_code VARCHAR(3),
    currency_code VARCHAR(3),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(bin, bin_length, participant_id)
);

-- Routing rules
CREATE TABLE routing_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    priority INTEGER NOT NULL DEFAULT 100,
    source_participant_id UUID REFERENCES participants(id),
    destination_participant_id UUID NOT NULL REFERENCES participants(id),
    condition_expression JSONB NOT NULL DEFAULT '{}',
    protocol VARCHAR(20) NOT NULL CHECK (protocol IN ('ISO8583', 'ISO20022', 'BOTH')),
    message_type VARCHAR(10),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Transactions ledger
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id VARCHAR(64) NOT NULL UNIQUE,
    message_type VARCHAR(10) NOT NULL,
    protocol VARCHAR(10) NOT NULL CHECK (protocol IN ('ISO8583', 'ISO20022')),
    stan VARCHAR(12),
    rrn VARCHAR(12),
    pan VARCHAR(19) ENCRYPTED,
    pan_hash VARCHAR(64) NOT NULL,
    amount NUMERIC(18, 3),
    currency_code VARCHAR(3),
    merchant_id VARCHAR(15),
    terminal_id VARCHAR(8),
    acquiring_participant_id UUID REFERENCES participants(id),
    issuing_participant_id UUID REFERENCES participants(id),
    source_participant_id UUID REFERENCES participants(id),
    destination_participant_id UUID REFERENCES participants(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'ROUTING', 'PROCESSING', 'COMPLETED', 'FAILED', 'TIMEOUT', 'REJECTED'
    )),
    response_code VARCHAR(2),
    original_message TEXT,
    parsed_message JSONB,
    response_message TEXT,
    routing_rule_id UUID REFERENCES routing_rules(id),
    processing_time_ms INTEGER,
    retry_count INTEGER DEFAULT 0,
    request_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    response_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Transaction audit log
CREATE TABLE transaction_audit (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transactions(id),
    event_type VARCHAR(50) NOT NULL,
    source VARCHAR(50),
    payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Message templates (ISO 8583)
CREATE TABLE message_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    mti VARCHAR(4) NOT NULL,
    protocol VARCHAR(10) NOT NULL CHECK (protocol IN ('ISO8583', 'ISO20022')),
    fields_definition JSONB NOT NULL DEFAULT '{}',
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Settlement records
CREATE TABLE settlements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    settlement_date DATE NOT NULL,
    participant_id UUID NOT NULL REFERENCES participants(id),
    currency_code VARCHAR(3) NOT NULL,
    total_count INTEGER NOT NULL DEFAULT 0,
    total_amount NUMERIC(18, 3) NOT NULL DEFAULT 0,
    fee_amount NUMERIC(18, 3) NOT NULL DEFAULT 0,
    net_amount NUMERIC(18, 3) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'IN_PROGRESS', 'COMPLETED', 'DISPUTED'
    )),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(settlement_date, participant_id, currency_code)
);

-- Indexes
CREATE INDEX idx_transactions_stan ON transactions(stan);
CREATE INDEX idx_transactions_rrn ON transactions(rrn);
CREATE INDEX idx_transactions_pan_hash ON transactions(pan_hash);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_request_at ON transactions(request_at);
CREATE INDEX idx_transactions_participants ON transactions(acquiring_participant_id, issuing_participant_id);
CREATE INDEX idx_transaction_audit_tx_id ON transaction_audit(transaction_id);
CREATE INDEX idx_routing_rules_priority ON routing_rules(priority, status);
CREATE INDEX idx_bin_tables_lookup ON bin_tables(bin, bin_length, is_active);
CREATE INDEX idx_settlements_date ON settlements(settlement_date, participant_id);
