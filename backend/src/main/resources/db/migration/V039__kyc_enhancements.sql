ALTER TABLE kyc_documents ADD COLUMN IF NOT EXISTS document_hash VARCHAR(64);
ALTER TABLE kyc_documents ADD COLUMN IF NOT EXISTS mime_type VARCHAR(64);
ALTER TABLE kyc_documents ADD COLUMN IF NOT EXISTS file_size BIGINT;
ALTER TABLE kyc_documents ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE kyc_documents ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS kyc_verifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    cardholder_id UUID NOT NULL REFERENCES cardholders(id),
    verification_type VARCHAR(30) NOT NULL CHECK (verification_type IN (
        'IDENTITY','ADDRESS','INCOME','FACE_MATCH','PHONE','EMAIL','BANK_ACCOUNT','SOURCE_OF_FUNDS'
    )),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN (
        'PENDING','IN_PROGRESS','VERIFIED','REJECTED','EXPIRED'
    )),
    requested_level INTEGER NOT NULL CHECK (requested_level BETWEEN 1 AND 5),
    verified_by VARCHAR(100),
    verified_at TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    rejection_reason TEXT,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_kyc_verifications_cardholder ON kyc_verifications(cardholder_id);
CREATE INDEX IF NOT EXISTS idx_kyc_verifications_status ON kyc_verifications(status);
CREATE INDEX IF NOT EXISTS idx_kyc_documents_cardholder ON kyc_documents(cardholder_id);
CREATE INDEX IF NOT EXISTS idx_kyc_documents_status ON kyc_documents(verification_status);
