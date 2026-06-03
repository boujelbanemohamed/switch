CREATE TABLE IF NOT EXISTS live_config (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    config_key      VARCHAR(100) NOT NULL UNIQUE,
    config_value    TEXT NOT NULL,
    description     VARCHAR(500),
    data_type       VARCHAR(20) NOT NULL DEFAULT 'STRING',
    category        VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    mutable         BOOLEAN NOT NULL DEFAULT TRUE,
    updated_by      VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_live_config_category ON live_config(category);

INSERT INTO live_config (config_key, config_value, description, data_type, category, mutable) VALUES
('transaction.max_amount', '1000000', 'Maximum transaction amount in smallest currency unit', 'LONG', 'TRANSACTION', TRUE),
('transaction.timeout_ms', '30000', 'Transaction processing timeout in milliseconds', 'INTEGER', 'TRANSACTION', TRUE),
('transaction.max_daily_volume', '100000000', 'Maximum daily transaction volume per participant', 'LONG', 'TRANSACTION', TRUE),
('auth.max_pin_attempts', '3', 'Maximum PIN entry attempts before card lock', 'INTEGER', 'AUTH', TRUE),
('auth.session_timeout_minutes', '30', 'JWT session idle timeout in minutes', 'INTEGER', 'AUTH', TRUE),
('auth.max_daily_auth_requests', '10000', 'Maximum authorization requests per day per card', 'LONG', 'AUTH', TRUE),
('fraud.max_velocity_minutes', '5', 'Velocity check time window in minutes', 'INTEGER', 'FRAUD', TRUE),
('fraud.max_txns_per_window', '10', 'Maximum transactions allowed in velocity window', 'INTEGER', 'FRAUD', TRUE),
('fraud.high_risk_threshold', '80', 'Risk score threshold for automatic blocking', 'INTEGER', 'FRAUD', TRUE),
('fee.default_interchange_rate', '0.012', 'Default interchange fee rate (1.2%)', 'DECIMAL', 'FEE', TRUE),
('fee.mdr_default_rate', '0.015', 'Default MDR rate for merchants (1.5%)', 'DECIMAL', 'FEE', TRUE),
('fee.max_fee_per_transaction', '50000', 'Maximum fee charged per transaction', 'LONG', 'FEE', TRUE),
('clearing.settlement_delay_days', '1', 'Days after clearing date for settlement', 'INTEGER', 'CLEARING', TRUE),
('clearing.min_netting_threshold', '1000', 'Minimum net amount to trigger settlement', 'LONG', 'CLEARING', TRUE),
('monitoring.alert_dashboard_refresh_seconds', '30', 'Dashboard auto-refresh interval', 'INTEGER', 'MONITORING', TRUE),
('monitoring.critical_threshold_failure_rate', '0.05', 'Failure rate threshold for critical alerts (5%)', 'DECIMAL', 'MONITORING', TRUE),
('monitoring.warning_threshold_processing_time', '5000', 'Processing time threshold for warnings in ms', 'INTEGER', 'MONITORING', TRUE),
('batch.eod_hour', '23', 'Hour of day for EOD batch execution (0-23)', 'INTEGER', 'BATCH', TRUE),
('batch.bod_hour', '6', 'Hour of day for BOD batch execution (0-23)', 'INTEGER', 'BATCH', TRUE),
('kyc.max_document_size_mb', '10', 'Maximum KYC document upload size in MB', 'INTEGER', 'KYC', TRUE),
('kyc.auto_upgrade_level', '3', 'KYC level granted after auto-verification', 'INTEGER', 'KYC', TRUE),
('switch.maintenance_mode', 'false', 'Enable maintenance mode (reject all transactions)', 'BOOLEAN', 'SWITCH', TRUE),
('switch.logging_level', 'INFO', 'Runtime logging level override', 'STRING', 'SWITCH', TRUE);
