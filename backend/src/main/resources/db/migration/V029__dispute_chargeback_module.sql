CREATE TABLE IF NOT EXISTS disputes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dispute_number VARCHAR(32) NOT NULL UNIQUE,
    transaction_id VARCHAR(64) NOT NULL,
    clearing_record_id UUID REFERENCES clearing_records(id),
    merchant_id UUID REFERENCES merchants(id),
    acquiring_participant_id UUID REFERENCES participants(id),
    issuing_participant_id UUID REFERENCES participants(id),
    amount NUMERIC(18,3) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    dispute_type VARCHAR(30) NOT NULL CHECK (dispute_type IN (
        'FRAUD','NOT_RECEIVED','DUPLICATE','INCORRECT_AMOUNT',
        'QUALITY_ISSUE','CANCELLED','CREDIT_NOT_PROCESSED','OTHER'
    )),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN' CHECK (status IN (
        'OPEN','UNDER_REVIEW','EVIDENCE_REQUESTED','EVIDENCE_SUBMITTED',
        'REPRESENTMENT','PRE_ARBITRATION','ARBITRATION','WON','LOST','WITHDRAWN'
    )),
    reason_code VARCHAR(10),
    reason_description TEXT,
    evidence_deadline TIMESTAMP WITH TIME ZONE,
    resolution_deadline TIMESTAMP WITH TIME ZONE,
    initiated_by VARCHAR(20) NOT NULL CHECK (initiated_by IN ('CARDHOLDER','MERCHANT','ISSUER','ACQUIRER')),
    initiated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolution_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS dispute_evidence (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dispute_id UUID NOT NULL REFERENCES disputes(id),
    submitted_by VARCHAR(20) NOT NULL,
    evidence_type VARCHAR(30) NOT NULL CHECK (evidence_type IN (
        'RECEIPT','CONTRACT','COMMUNICATION','DELIVERY_PROOF',
        'REFUND_PROOF','OTHER_DOCUMENT'
    )),
    description TEXT,
    file_reference VARCHAR(256),
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS dispute_timeline (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dispute_id UUID NOT NULL REFERENCES disputes(id),
    action VARCHAR(50) NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30),
    performed_by VARCHAR(64),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_disputes_transaction ON disputes(transaction_id);
CREATE INDEX IF NOT EXISTS idx_disputes_status ON disputes(status);
CREATE INDEX IF NOT EXISTS idx_disputes_merchant ON disputes(merchant_id);
CREATE INDEX IF NOT EXISTS idx_dispute_evidence_dispute ON dispute_evidence(dispute_id);
CREATE INDEX IF NOT EXISTS idx_dispute_timeline_dispute ON dispute_timeline(dispute_id);
