-- ========================================
-- Finding 1: Create missing netting_results table
-- ========================================
CREATE TABLE IF NOT EXISTS netting_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID,
    date DATE,
    gross_amount NUMERIC(18,3),
    total_fees NUMERIC(18,3),
    net_amount NUMERIC(18,3),
    transaction_count INTEGER DEFAULT 0,
    currency_code VARCHAR(3),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ========================================
-- Finding 2: Create missing notifications table
-- ========================================
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type VARCHAR(64),
    recipient VARCHAR(64),
    subject VARCHAR(255),
    body TEXT,
    channel VARCHAR(20),
    status VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- ========================================
-- Finding 10: Fix V006/V010 audit_logs conflict
-- V006 created audit_logs with BIGSERIAL id (orphaned schema)
-- V010 expects UUID id. On fresh DBs, V010 fails because table exists.
-- This migration DROPs + re-creates ONLY if the old BIGSERIAL schema is detected.
-- On existing DBs where V010 already ran (UUID schema), this is a no-op.
-- ========================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_logs'
        AND column_name = 'id'
        AND data_type = 'bigint'
    ) THEN
        DROP TABLE IF EXISTS audit_logs CASCADE;

        CREATE TABLE audit_logs (
            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
            user_id UUID REFERENCES auth_users(id),
            username VARCHAR(64),
            action VARCHAR(64) NOT NULL,
            resource_type VARCHAR(64) NOT NULL,
            resource_id VARCHAR(128),
            details TEXT,
            status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
            ip_address VARCHAR(45),
            user_agent VARCHAR(512),
            duration_ms INTEGER,
            created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
        );

        CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
        CREATE INDEX idx_audit_logs_action ON audit_logs(action);
        CREATE INDEX idx_audit_logs_resource_type ON audit_logs(resource_type);
        CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
    END IF;
END $$;
