CREATE TABLE credit_lines (
    id UUID PRIMARY KEY,
    card_account_id UUID NOT NULL REFERENCES card_accounts(id),
    credit_limit NUMERIC(18,3) NOT NULL DEFAULT 0,
    current_balance NUMERIC(18,3) NOT NULL DEFAULT 0,
    hold_amount NUMERIC(18,3) NOT NULL DEFAULT 0,
    available_credit NUMERIC(18,3) NOT NULL DEFAULT 0,
    apr NUMERIC(5,2) NOT NULL DEFAULT 18.00,
    statement_day INT NOT NULL DEFAULT 1 CHECK (statement_day BETWEEN 1 AND 28),
    payment_due_days INT NOT NULL DEFAULT 20,
    min_payment_pct NUMERIC(5,2) NOT NULL DEFAULT 5.00,
    min_payment_floor NUMERIC(18,3) NOT NULL DEFAULT 10.000,
    currency_code VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE credit_statements (
    id UUID PRIMARY KEY,
    credit_line_id UUID NOT NULL REFERENCES credit_lines(id),
    statement_date DATE NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    opening_balance NUMERIC(18,3) NOT NULL DEFAULT 0,
    purchases_total NUMERIC(18,3) NOT NULL DEFAULT 0,
    payments_total NUMERIC(18,3) NOT NULL DEFAULT 0,
    interest_charged NUMERIC(18,3) NOT NULL DEFAULT 0,
    fees_charged NUMERIC(18,3) NOT NULL DEFAULT 0,
    closing_balance NUMERIC(18,3) NOT NULL DEFAULT 0,
    minimum_payment NUMERIC(18,3) NOT NULL DEFAULT 0,
    due_date DATE NOT NULL,
    paid_in_full BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE installment_plans (
    id UUID PRIMARY KEY,
    credit_line_id UUID NOT NULL REFERENCES credit_lines(id),
    original_transaction_ref VARCHAR(64),
    total_amount NUMERIC(18,3) NOT NULL,
    installment_count INT NOT NULL,
    installment_amount NUMERIC(18,3) NOT NULL,
    fee_amount NUMERIC(18,3) NOT NULL DEFAULT 0,
    apr NUMERIC(5,2),
    start_date DATE NOT NULL,
    remaining_count INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE installment_entries (
    id UUID PRIMARY KEY,
    installment_plan_id UUID NOT NULL REFERENCES installment_plans(id),
    sequence_number INT NOT NULL,
    due_date DATE NOT NULL,
    amount NUMERIC(18,3) NOT NULL,
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    statement_id UUID REFERENCES credit_statements(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Seed credit ledger accounts
INSERT INTO ledger_accounts (account_number, account_type, currency, label)
VALUES
    ('CREDIT_RECEIVABLE', 'ASSET',     'TND', 'Créances crédit (cardholders)'),
    ('CREDIT_FUNDING',    'LIABILITY', 'TND', 'Financement lignes de crédit')
ON CONFLICT (account_number) DO NOTHING;
