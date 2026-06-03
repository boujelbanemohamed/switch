CREATE TABLE IF NOT EXISTS multilateral_netting_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CALCULATING' CHECK (status IN (
        'CALCULATING','CALCULATED','CONFIRMED','SETTLED','FAILED'
    )),
    total_gross_amount NUMERIC(18,3),
    total_net_amount NUMERIC(18,3),
    netting_efficiency NUMERIC(5,2),
    currency_code CHAR(3) NOT NULL DEFAULT 'TND',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS multilateral_positions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NOT NULL REFERENCES multilateral_netting_sessions(id),
    participant_id UUID NOT NULL REFERENCES participants(id),
    gross_debit NUMERIC(18,3) NOT NULL DEFAULT 0,
    gross_credit NUMERIC(18,3) NOT NULL DEFAULT 0,
    net_position NUMERIC(18,3) NOT NULL,
    position_type VARCHAR(10) NOT NULL CHECK (position_type IN ('DEBIT','CREDIT','NEUTRAL')),
    settlement_status VARCHAR(20) DEFAULT 'PENDING',
    settlement_reference VARCHAR(64),
    settled_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_ml_positions_session ON multilateral_positions(session_id);
CREATE INDEX IF NOT EXISTS idx_ml_positions_participant ON multilateral_positions(participant_id);
