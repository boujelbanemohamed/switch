CREATE TABLE IF NOT EXISTS fee_schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(128) NOT NULL,
    description TEXT,
    schedule_type VARCHAR(30) NOT NULL CHECK (schedule_type IN (
        'INTERCHANGE','SCHEME','PROCESSING','CROSS_BORDER',
        'CURRENCY_CONVERSION','ATM','FIXED','COMPOSITE'
    )),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN (
        'DRAFT','ACTIVE','INACTIVE','ARCHIVED'
    )),
    priority INTEGER NOT NULL DEFAULT 0,
    currency_code CHAR(3) DEFAULT 'TND',
    effective_from DATE NOT NULL,
    effective_until DATE,
    participant_id UUID REFERENCES participants(id),
    merchant_id UUID REFERENCES merchants(id),
    card_product_id UUID,
    applies_to VARCHAR(20) DEFAULT 'ALL' CHECK (applies_to IN (
        'ALL','ISSUER','ACQUIRER','MERCHANT','PARTICIPANT'
    )),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fee_schedules_type_status ON fee_schedules(schedule_type, status);
CREATE INDEX IF NOT EXISTS idx_fee_schedules_dates ON fee_schedules(effective_from, effective_until);
CREATE INDEX IF NOT EXISTS idx_fee_schedules_participant ON fee_schedules(participant_id);

CREATE TABLE IF NOT EXISTS fee_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    schedule_id UUID NOT NULL REFERENCES fee_schedules(id) ON DELETE CASCADE,
    rule_name VARCHAR(128) NOT NULL,
    rule_order INTEGER NOT NULL DEFAULT 0,
    calc_method VARCHAR(20) NOT NULL CHECK (calc_method IN (
        'FLAT','PERCENTAGE','TIERED','MIXED','INTERCHANGE_LOOKUP'
    )),
    flat_amount NUMERIC(18,3),
    percentage_rate NUMERIC(10,6),
    min_amount NUMERIC(18,3),
    max_amount NUMERIC(18,3),
    min_tx_amount NUMERIC(18,3),
    max_tx_amount NUMERIC(18,3),
    brand_filter VARCHAR(20),
    card_type_filter VARCHAR(20),
    mcc_filter VARCHAR(4),
    region_filter VARCHAR(4),
    entry_mode_filter VARCHAR(20),
    is_waivable BOOLEAN DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fee_rules_schedule ON fee_rules(schedule_id);
CREATE INDEX IF NOT EXISTS idx_fee_rules_filters ON fee_rules(brand_filter, card_type_filter, mcc_filter);
