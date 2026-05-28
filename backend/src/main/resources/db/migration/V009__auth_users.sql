-- ========================================
-- MODULE G: AUTHENTICATION & AUTHORISATION
-- ========================================

CREATE TABLE auth_users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(256) NOT NULL,
    email VARCHAR(128) NOT NULL UNIQUE,
    display_name VARCHAR(64),
    role VARCHAR(20) NOT NULL DEFAULT 'OPERATOR'
        CHECK (role IN ('ADMIN', 'OPERATOR', 'ANALYST', 'AUDITOR', 'VIEWER')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    last_login TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auth_users_username ON auth_users(username);
CREATE INDEX idx_auth_users_email ON auth_users(email);
CREATE INDEX idx_auth_users_role ON auth_users(role);

-- Seed default admin user (password: admin123, BCrypt hash)
INSERT INTO auth_users (id, username, password, email, display_name, role, enabled)
VALUES (
    uuid_generate_v4(),
    'admin',
    '$2a$10$9egeu0x.U3XTdCz/KkrB5eOM/ZadzG9StpxQYPRYNZpXsHjtC/GHS',
    'admin@switch.local',
    'Administrator',
    'ADMIN',
    TRUE
);

-- Seed default operator user (password: operator123)
INSERT INTO auth_users (id, username, password, email, display_name, role, enabled)
VALUES (
    uuid_generate_v4(),
    'operator',
    '$2a$10$3VsrkaAW1N2WsLl.22RbaeTxgo/emW3QFSAq6Crc7kBluqAvp75r6',
    'operator@switch.local',
    'Operator',
    'OPERATOR',
    TRUE
);
