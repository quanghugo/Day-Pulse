-- Keycloak Integration Migration
-- Add keycloak_id column and remove password-related columns

-- Add Keycloak user ID column
ALTER TABLE users_auth ADD COLUMN IF NOT EXISTS keycloak_id UUID UNIQUE;

-- Remove password hash column (now stored in Keycloak)
ALTER TABLE users_auth DROP COLUMN IF EXISTS password_hash;

-- Remove OAuth provider columns (now managed in Keycloak)
ALTER TABLE users_auth DROP COLUMN IF EXISTS oauth_provider;
ALTER TABLE users_auth DROP COLUMN IF EXISTS oauth_id;

-- Drop refresh_tokens table (Keycloak manages refresh tokens)
DROP TABLE IF EXISTS refresh_tokens;

-- Create index on keycloak_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_auth_keycloak_id ON users_auth(keycloak_id);

-- Update existing users: set keycloak_id to null initially
-- Users will need to re-register or be migrated manually
UPDATE users_auth SET keycloak_id = NULL WHERE keycloak_id IS NULL;
