CREATE TABLE IF NOT EXISTS batch_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_name VARCHAR(100) NOT NULL,
    job_type VARCHAR(30) NOT NULL CHECK (job_type IN (
        'EOD_CLEARING','BOD_POSITIONS','SETTLEMENT_FILE',
        'RECONCILIATION','INTERCHANGE_CALC','REPORT_GENERATION',
        'NOTIFICATION_DIGEST','EXPIRY_CHECK','DISPUTE_DEADLINE_CHECK'
    )),
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN (
        'SCHEDULED','RUNNING','COMPLETED','FAILED','CANCELLED'
    )),
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    records_processed INTEGER DEFAULT 0,
    records_failed INTEGER DEFAULT 0,
    error_message TEXT,
    result_summary JSONB,
    triggered_by VARCHAR(64) DEFAULT 'SCHEDULER',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_batch_jobs_type_status ON batch_jobs(job_type, status);
CREATE INDEX IF NOT EXISTS idx_batch_jobs_scheduled ON batch_jobs(scheduled_at);
