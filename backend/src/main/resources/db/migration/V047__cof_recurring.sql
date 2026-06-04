CREATE TABLE cof_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pan_display VARCHAR(8) NOT NULL,
    pan_reference VARCHAR(64) NOT NULL,
    expiry_month INT,
    expiry_year INT,
    cardholder_name VARCHAR(100),
    participant_id UUID REFERENCES participants(id),
    token_type VARCHAR(20) NOT NULL DEFAULT 'UNSCHEDULED',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE recurring_schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cof_token_id UUID NOT NULL REFERENCES cof_tokens(id),
    amount NUMERIC(18,3) NOT NULL,
    currency_code CHAR(3) NOT NULL DEFAULT 'TND',
    frequency VARCHAR(20) NOT NULL,
    next_run_date DATE NOT NULL,
    end_date DATE,
    max_occurrences INT,
    occurrences_processed INT NOT NULL DEFAULT 0,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cof_tokens_participant ON cof_tokens(participant_id);
CREATE INDEX idx_cof_tokens_status ON cof_tokens(status);
CREATE INDEX idx_recurring_next_run ON recurring_schedules(next_run_date, status);
CREATE INDEX idx_recurring_cof_token ON recurring_schedules(cof_token_id);
