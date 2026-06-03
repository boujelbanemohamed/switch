CREATE TABLE IF NOT EXISTS backoffice_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64),
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64),
    resource_id VARCHAR(64),
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    status VARCHAR(15) NOT NULL DEFAULT 'SUCCESS',
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_backoffice_audit_logs_created_at ON backoffice_audit_logs(created_at DESC);
CREATE INDEX idx_backoffice_audit_logs_action ON backoffice_audit_logs(action);
CREATE INDEX idx_backoffice_audit_logs_user_id ON backoffice_audit_logs(user_id);
CREATE INDEX idx_backoffice_audit_logs_resource ON backoffice_audit_logs(resource_type, resource_id);
