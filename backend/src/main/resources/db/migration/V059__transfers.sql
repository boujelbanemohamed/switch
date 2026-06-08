CREATE TABLE transfers (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_type           VARCHAR(10)  NOT NULL,                     -- A2A / P2P
    source_account_id       UUID         NOT NULL REFERENCES card_accounts(id),
    destination_account_id  UUID         REFERENCES card_accounts(id), -- nullable until resolved
    source_reference        VARCHAR(64),                                -- PAN hash / IBAN saisi
    destination_reference   VARCHAR(64),                                -- PAN hash / IBAN / alias
    amount                  NUMERIC(18,3) NOT NULL,
    currency_code           VARCHAR(3)   NOT NULL DEFAULT 'TND',
    fee_amount              NUMERIC(18,3) NOT NULL DEFAULT 0,
    fee_currency            VARCHAR(3)   NOT NULL DEFAULT 'TND',
    status                  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',   -- PENDING / COMPLETED / FAILED / REVERSED
    failure_reason          TEXT,
    ledger_journal_id       UUID         REFERENCES journal_entries(id),
    reversed_journal_id     UUID         REFERENCES journal_entries(id),
    channel                 VARCHAR(10)  NOT NULL DEFAULT 'BACKOFFICE', -- ATM / MOBILE / WEB / BACKOFFICE
    original_transfer_id    UUID         REFERENCES transfers(id),     -- non-null pour un reversal
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    completed_at            TIMESTAMPTZ,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_transfers_source_acc ON transfers(source_account_id);
CREATE INDEX idx_transfers_dest_acc  ON transfers(destination_account_id);
CREATE INDEX idx_transfers_status    ON transfers(status);
CREATE INDEX idx_transfers_created   ON transfers(created_at DESC);

-- Limites paramétrables par type de transfert
CREATE TABLE transfer_limits (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transfer_type       VARCHAR(10) NOT NULL,              -- A2A / P2P / BOTH
    per_transfer_max    NUMERIC(18,3) NOT NULL,
    daily_max_amount    NUMERIC(18,3) NOT NULL,
    daily_max_count     INTEGER      NOT NULL DEFAULT 10,
    currency_code       VARCHAR(3)   NOT NULL DEFAULT 'TND',
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Bénéficiaires P2P enregistrés
CREATE TABLE transfer_beneficiaries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_cardholder_id UUID         NOT NULL,
    alias               VARCHAR(50)  NOT NULL,
    masked_pan          VARCHAR(20),
    account_number      VARCHAR(34),
    iban                VARCHAR(34),
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_benef_owner ON transfer_beneficiaries(owner_cardholder_id);

-- Compte ledger pour les frais de transfert
INSERT INTO ledger_accounts (account_number, account_type, currency, label)
VALUES ('TRANSFER_FEE_INCOME', 'INCOME', 'TND', 'Frais sur transferts A2A / P2P');

-- Limites par défaut (paramétrables, à valider par le client)
INSERT INTO transfer_limits (transfer_type, per_transfer_max, daily_max_amount, daily_max_count)
VALUES ('A2A', 10000.000, 50000.000, 10);

INSERT INTO transfer_limits (transfer_type, per_transfer_max, daily_max_amount, daily_max_count)
VALUES ('P2P', 5000.000, 20000.000, 5);
