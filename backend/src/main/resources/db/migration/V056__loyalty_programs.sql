CREATE TABLE loyalty_programs (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    earning_rate NUMERIC(10,4) NOT NULL DEFAULT 0.1,
    currency VARCHAR(3) NOT NULL DEFAULT 'TND',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE loyalty_tiers (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES loyalty_programs(id),
    name VARCHAR(50) NOT NULL,
    min_lifetime_points NUMERIC(18,3) NOT NULL DEFAULT 0,
    earning_multiplier NUMERIC(5,2) NOT NULL DEFAULT 1.00,
    benefits JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE loyalty_memberships (
    id UUID PRIMARY KEY,
    cardholder_id UUID NOT NULL REFERENCES cardholders(id),
    program_id UUID NOT NULL REFERENCES loyalty_programs(id),
    tier_id UUID NOT NULL REFERENCES loyalty_tiers(id),
    points_balance NUMERIC(18,3) NOT NULL DEFAULT 0,
    lifetime_points NUMERIC(18,3) NOT NULL DEFAULT 0,
    enrolled_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    UNIQUE(cardholder_id, program_id)
);

CREATE TABLE loyalty_transactions (
    id UUID PRIMARY KEY,
    membership_id UUID NOT NULL REFERENCES loyalty_memberships(id),
    type VARCHAR(20) NOT NULL,
    points NUMERIC(18,3) NOT NULL,
    transaction_ref VARCHAR(64),
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE loyalty_rewards (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES loyalty_programs(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    points_cost NUMERIC(18,3) NOT NULL,
    stock INT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE loyalty_redemptions (
    id UUID PRIMARY KEY,
    membership_id UUID NOT NULL REFERENCES loyalty_memberships(id),
    reward_id UUID REFERENCES loyalty_rewards(id),
    points_spent NUMERIC(18,3) NOT NULL,
    balance_credit_amount NUMERIC(18,3),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Seed default program with tiers
INSERT INTO loyalty_programs (id, name, description, earning_rate, currency)
VALUES ('11111111-1111-1111-1111-111111111111', 'Standard', 'Programme de fidélité standard', 0.1, 'TND');

INSERT INTO loyalty_tiers (id, program_id, name, min_lifetime_points, earning_multiplier, benefits)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'Silver',  0,     1.0, '{"discount_pct": 0, "priority_support": false, "fee_waiver": false}'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'Gold',    10000, 1.5, '{"discount_pct": 5, "priority_support": true, "fee_waiver": false}'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', '11111111-1111-1111-1111-111111111111', 'Platinum',50000, 2.0, '{"discount_pct": 10, "priority_support": true, "fee_waiver": true}');
