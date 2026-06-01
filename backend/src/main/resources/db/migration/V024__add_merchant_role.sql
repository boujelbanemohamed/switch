-- V024__add_merchant_role.sql
-- 1. Modifier la contrainte CHECK pour inclure MERCHANT
ALTER TABLE auth_users
    DROP CONSTRAINT IF EXISTS auth_users_role_check;

ALTER TABLE auth_users
    ADD CONSTRAINT auth_users_role_check
    CHECK (role IN ('ADMIN','OPERATOR','ANALYST','AUDITOR','VIEWER','MERCHANT'));

-- 2. Ajouter la colonne merchantCode pour lier un user à un commerçant
ALTER TABLE auth_users
    ADD COLUMN IF NOT EXISTS merchant_code VARCHAR(32);
