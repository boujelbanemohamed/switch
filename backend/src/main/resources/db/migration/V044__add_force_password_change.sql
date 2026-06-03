ALTER TABLE auth_users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP WITH TIME ZONE;

-- Set existing users as not needing password change
UPDATE auth_users SET must_change_password = false WHERE must_change_password IS NULL;
