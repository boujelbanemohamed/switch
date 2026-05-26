-- ========================================
-- MODULE E: CLEARING & SETTLEMENT
-- ========================================

-- Clearing records
CREATE TABLE clearing_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    clearing_date DATE NOT NULL,
    batch_number VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(64),
    acquiring_participant_id UUID REFERENCES participants(id),
    issuing_participant_id UUID REFERENCES participants(id),
    pan_hash VARCHAR(64),
    amount NUMERIC(18,3) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    interchange_amount NUMERIC(18,3) DEFAULT 0,
    fee_amount NUMERIC(18,3) DEFAULT 0,
    net_amount NUMERIC(18,3) DEFAULT 0,
    message_type VARCHAR(10),
    transaction_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'CLEARED', 'DISPUTED', 'REVERSED', 'SETTLED', 'FAILED'
    )),
    dispute_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Netting records
CREATE TABLE netting_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    netting_date DATE NOT NULL,
    participant_id UUID NOT NULL REFERENCES participants(id),
    counterparty_id UUID REFERENCES participants(id),
    total_sent NUMERIC(18,3) NOT NULL DEFAULT 0,
    total_received NUMERIC(18,3) NOT NULL DEFAULT 0,
    net_amount NUMERIC(18,3) NOT NULL DEFAULT 0,
    currency_code CHAR(3) NOT NULL,
    transaction_count INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'CONFIRMED', 'SETTLED', 'DISPUTED'
    )),
    settled_at TIMESTAMP WITH TIME ZONE,
    settlement_reference VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Reconciliation
CREATE TABLE reconciliation_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reconciliation_date DATE NOT NULL,
    participant_id UUID REFERENCES participants(id),
    merchant_id UUID REFERENCES merchants(id),
    source VARCHAR(20) CHECK (source IN ('SWITCH', 'PARTICIPANT', 'MERCHANT', 'SCHEME')),
    total_transactions INTEGER DEFAULT 0,
    total_amount NUMERIC(18,3) DEFAULT 0,
    total_fees NUMERIC(18,3) DEFAULT 0,
    matched_count INTEGER DEFAULT 0,
    unmatched_count INTEGER DEFAULT 0,
    discrepancy_count INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'MATCHED', 'PARTIALLY_MATCHED', 'DISCREPANCY', 'RESOLVED'
    )),
    resolved_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ========================================
-- MODULE F: FRAUD & RISK
-- ========================================

-- Fraud rules
CREATE TABLE fraud_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    rule_category VARCHAR(30) NOT NULL CHECK (rule_category IN (
        'VELOCITY', 'GEO', 'BEHAVIORAL', 'AMOUNT', 'MERCHANT', 'DEVICE', 'NETWORK', 'ML_MODEL', 'MANUAL'
    )),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    action VARCHAR(20) NOT NULL CHECK (action IN ('BLOCK', 'FLAG', 'CHALLENGE', 'MONITOR', '2FA', 'ALLOW')),
    condition_expression JSONB NOT NULL,
    score_weight INTEGER DEFAULT 50,
    cooldown_seconds INTEGER DEFAULT 300,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'TESTING')),
    false_positive_count INTEGER DEFAULT 0,
    true_positive_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Fraud alerts
CREATE TABLE fraud_alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    card_id UUID REFERENCES cards(id),
    cardholder_id UUID REFERENCES cardholders(id),
    transaction_id VARCHAR(64),
    rule_id UUID REFERENCES fraud_rules(id),
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    score INTEGER,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN (
        'OPEN', 'INVESTIGATING', 'CONFIRMED', 'FALSE_POSITIVE', 'CLOSED'
    )),
    assigned_to VARCHAR(100),
    decision VARCHAR(20) CHECK (decision IN ('APPROVED', 'DECLINED', 'REVIEW', 'ESCALATED')),
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Behavioral profiles
CREATE TABLE behavioral_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cardholder_id UUID NOT NULL REFERENCES cardholders(id),
    avg_transaction_amount NUMERIC(18,3) DEFAULT 0,
    avg_transactions_per_day NUMERIC(10,2) DEFAULT 0,
    typical_merchant_categories VARCHAR(4)[] DEFAULT '{}',
    typical_countries VARCHAR(2)[] DEFAULT '{}',
    typical_hours INTEGER[] DEFAULT '{}',
    typical_days INTEGER[] DEFAULT '{}',
    last_updated TIMESTAMP WITH TIME ZONE,
    profile_data JSONB DEFAULT '{}',
    model_version VARCHAR(20),
    risk_score INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ========================================
-- MODULE G: BACK-OFFICE & REPORTING
-- ========================================

-- Audit log
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(64),
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS', 'FAILURE', 'UNAUTHORIZED')),
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Reports
CREATE TABLE reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    report_type VARCHAR(30) NOT NULL CHECK (report_type IN (
        'TRANSACTION', 'SETTLEMENT', 'FRAUD', 'AUDIT', 'REGULATORY', 'PERFORMANCE', 'FINANCIAL', 'CUSTOM'
    )),
    parameters JSONB,
    file_path VARCHAR(512),
    file_format VARCHAR(10) CHECK (file_format IN ('CSV', 'PDF', 'XLSX', 'JSON', 'XML')),
    generated_by VARCHAR(100),
    generated_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING', 'GENERATING', 'COMPLETED', 'FAILED'
    )),
    error_message TEXT,
    scheduled BOOLEAN DEFAULT FALSE,
    schedule_cron VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Monitoring events
CREATE TABLE monitoring_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL')),
    source VARCHAR(100),
    message TEXT,
    metric_name VARCHAR(100),
    metric_value NUMERIC(18,3),
    threshold_value NUMERIC(18,3),
    details JSONB,
    acknowledged BOOLEAN DEFAULT FALSE,
    acknowledged_by VARCHAR(100),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_clearing_date ON clearing_records(clearing_date);
CREATE INDEX idx_clearing_status ON clearing_records(status);
CREATE INDEX idx_netting_date ON netting_records(netting_date);
CREATE INDEX idx_netting_participant ON netting_records(participant_id);
CREATE INDEX idx_recon_date ON reconciliation_records(reconciliation_date);
CREATE INDEX idx_fraud_rules_category ON fraud_rules(rule_category);
CREATE INDEX idx_fraud_alerts_card ON fraud_alerts(card_id);
CREATE INDEX idx_fraud_alerts_status ON fraud_alerts(status);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_time ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_reports_type ON reports(report_type);
CREATE INDEX idx_monitoring_events_type ON monitoring_events(event_type);
CREATE INDEX idx_monitoring_events_severity ON monitoring_events(severity);
CREATE INDEX idx_behavioral_profiles_cardholder ON behavioral_profiles(cardholder_id);
CREATE INDEX idx_clearing_records_tx ON clearing_records(transaction_id);
