-- Migration: Add enum-based role column and remove old many-to-many relationship
-- This migration script transitions from database-driven roles to enum-based roles

-- Step 1: Add new role column (nullable initially to allow data migration)
ALTER TABLE users_auth ADD COLUMN IF NOT EXISTS role VARCHAR(50);

-- Step 2: Migrate existing user roles from many-to-many to single role column
-- Default all users to USER role initially
UPDATE users_auth SET role = 'USER' WHERE role IS NULL;

-- Step 3: Update users who have ADMIN role in the old system
-- This assumes junction table exists: user_roles(user_id, role_name)
UPDATE users_auth u
SET role = 'ADMIN'
WHERE EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = u.id AND ur.role_name = 'ADMIN'
);

-- Step 4: Update users who have MODERATOR role (if it exists in old system)
UPDATE users_auth u
SET role = 'MODERATOR'
WHERE EXISTS (
    SELECT 1 FROM user_roles ur 
    WHERE ur.user_id = u.id AND ur.role_name = 'MODERATOR'
)
AND role != 'ADMIN'; -- Don't downgrade admins

-- Step 5: Make role column non-nullable with default value
ALTER TABLE users_auth ALTER COLUMN role SET NOT NULL;
ALTER TABLE users_auth ALTER COLUMN role SET DEFAULT 'USER';

-- Step 6: Drop old many-to-many relationship tables
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS role_permissions;

-- Step 7: Drop old role and permission tables
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS permissions;

-- Step 8: Add index on role column for faster queries
CREATE INDEX IF NOT EXISTS idx_users_auth_role ON users_auth(role);
