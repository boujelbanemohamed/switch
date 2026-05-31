-- ========================================
-- V020: ACS Card Enrollments (3D Secure enrollment)
-- ========================================

CREATE TABLE acs_card_enrollments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    card_id UUID NOT NULL REFERENCES cards(id),
    cardholder_id UUID REFERENCES cardholders(id),
    merchant_id UUID REFERENCES merchants(id),
    status VARCHAR(30) NOT NULL DEFAULT 'ENROLLED' CHECK (status IN (
        'ENROLLED', 'ACTIVE', 'SUSPENDED', 'CANCELLED'
    )),
    enrolled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    card_number_hash VARCHAR(128),
    card_brand VARCHAR(20),
    card_type VARCHAR(20),
    phone_number VARCHAR(20),
    email VARCHAR(256),
    canceled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_acs_enrollment_card ON acs_card_enrollments(card_id);
CREATE INDEX idx_acs_enrollment_cardholder ON acs_card_enrollments(cardholder_id);
CREATE INDEX idx_acs_enrollment_status ON acs_card_enrollments(status);
CREATE INDEX idx_acs_enrollment_merchant ON acs_card_enrollments(merchant_id);
