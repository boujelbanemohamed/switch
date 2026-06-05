ALTER TABLE transactions
    ADD COLUMN pos_entry_mode VARCHAR(3),
    ADD COLUMN pos_condition_code VARCHAR(2),
    ADD COLUMN channel VARCHAR(4),
    ADD COLUMN transaction_type VARCHAR(4);

CREATE INDEX idx_transactions_channel ON transactions(channel);
CREATE INDEX idx_transactions_transaction_type ON transactions(transaction_type);
CREATE INDEX idx_transactions_pos_entry_mode ON transactions(pos_entry_mode);
