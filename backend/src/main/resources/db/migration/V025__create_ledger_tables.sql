-- V025__create_ledger_tables.sql
-- Ledger comptable double entry

CREATE TABLE ledger_accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number  VARCHAR(34)  NOT NULL UNIQUE,
    account_type    VARCHAR(20)  NOT NULL,
    currency        VARCHAR(3)   NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    balance         NUMERIC(18,3) NOT NULL DEFAULT 0,
    label           VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE journal_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference       VARCHAR(64)  NOT NULL,
    posting_date    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    description     VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE ledger_entries (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_id           UUID         NOT NULL REFERENCES journal_entries(id),
    account_id           UUID         NOT NULL REFERENCES ledger_accounts(id),
    debit_amount         NUMERIC(18,3) NOT NULL DEFAULT 0,
    credit_amount        NUMERIC(18,3) NOT NULL DEFAULT 0,
    currency             VARCHAR(3)   NOT NULL,
    transaction_reference VARCHAR(64),
    description          VARCHAR(255),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE accounting_transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_id       UUID         NOT NULL REFERENCES journal_entries(id),
    transaction_type VARCHAR(32)  NOT NULL,
    reference        VARCHAR(64)  NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'POSTED',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_ledger_entries_journal   ON ledger_entries(journal_id);
CREATE INDEX idx_ledger_entries_account   ON ledger_entries(account_id);
CREATE INDEX idx_ledger_entries_txn_ref   ON ledger_entries(transaction_reference);
CREATE INDEX idx_journal_entries_status   ON journal_entries(status);
CREATE INDEX idx_journal_entries_date     ON journal_entries(posting_date);
CREATE INDEX idx_accounting_txn_journal   ON accounting_transactions(journal_id);
CREATE INDEX idx_accounting_txn_ref       ON accounting_transactions(reference);

-- Seed ledger accounts (Settlement, Interchange, Fees)
INSERT INTO ledger_accounts (account_number, account_type, currency, label)
VALUES
    ('SETTLEMENT_MAIN',   'LIABILITY',  'TND', 'Compte règlement principal'),
    ('INTERCHANGE_POOL',  'LIABILITY',  'TND', 'Pool interchange'),
    ('FEE_INCOME',        'INCOME',     'TND', 'Frais et commission'),
    ('SUSPENSE',          'CONTINGENT', 'TND', 'Compte d''attente'),
    ('MERCHANT_ACQUIRER', 'LIABILITY',  'TND', 'Compte acquéreur commerçants'),
    ('HOLD_RESERVE',      'CONTINGENT', 'TND', 'Réservation de fonds (holds)');
