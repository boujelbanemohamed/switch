CREATE TABLE stand_in_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    issuer_participant_id UUID REFERENCES participants(id),
    card_brand VARCHAR(20) DEFAULT 'ALL',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_amount NUMERIC(18,3) NOT NULL,
    daily_count_limit INTEGER NOT NULL DEFAULT 5,
    daily_amount_limit NUMERIC(18,3) NOT NULL,
    allowed_mcc TEXT DEFAULT '*',
    decline_if_no_rule BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE stand_in_authorizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id VARCHAR(64) NOT NULL,
    card_suffix VARCHAR(4),
    issuer_participant_id UUID,
    amount NUMERIC(18,3) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    decision VARCHAR(10) NOT NULL CHECK (decision IN ('APPROVED','DECLINED')),
    reason VARCHAR(100),
    reconciled BOOLEAN NOT NULL DEFAULT FALSE,
    authorized_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_standin_auth_reconciled ON stand_in_authorizations(reconciled);
CREATE INDEX idx_standin_auth_issuer ON stand_in_authorizations(issuer_participant_id);
