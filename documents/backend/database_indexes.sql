-- ============================================
-- DayPulse Database Performance Indexes
-- ============================================
-- Run these scripts after initial startup to improve performance
-- Execute in order: auth-service first, then user-service
-- ============================================

-- ============================================
-- AUTH SERVICE DATABASE
-- ============================================
-- Connect to auth-service database first:
-- psql -U postgres -d auth-service

\echo '================================================'
\echo 'Creating indexes for auth-service database...'
\echo '================================================'

-- Refresh Tokens Performance
-- Used for: Token validation, logout operations
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id 
    ON refresh_tokens(user_id);
\echo '✓ Created index on refresh_tokens.user_id'

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash 
    ON refresh_tokens(token_hash);
\echo '✓ Created index on refresh_tokens.token_hash'

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_revoked_at 
    ON refresh_tokens(revoked_at);
\echo '✓ Created index on refresh_tokens.revoked_at'

-- Composite index for faster logout queries
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_revoked 
    ON refresh_tokens(user_id, revoked_at);
\echo '✓ Created composite index on refresh_tokens(user_id, revoked_at)'

-- OTP Codes Performance
-- Used for: Email verification, password reset
CREATE INDEX IF NOT EXISTS idx_otp_codes_user_id 
    ON otp_codes(user_id);
\echo '✓ Created index on otp_codes.user_id'

CREATE INDEX IF NOT EXISTS idx_otp_codes_expires_at 
    ON otp_codes(expires_at);
\echo '✓ Created index on otp_codes.expires_at'

-- Composite index for OTP validation
CREATE INDEX IF NOT EXISTS idx_otp_codes_user_type_expiry 
    ON otp_codes(user_id, type, expires_at);
\echo '✓ Created composite index on otp_codes(user_id, type, expires_at)'

-- Users Auth Performance
-- Email is already unique, but index helps with lookups
CREATE INDEX IF NOT EXISTS idx_users_auth_email 
    ON users_auth(email);
\echo '✓ Created index on users_auth.email'

-- For OAuth lookups
CREATE INDEX IF NOT EXISTS idx_users_auth_oauth 
    ON users_auth(oauth_provider, oauth_id) 
    WHERE oauth_provider IS NOT NULL;
\echo '✓ Created index on users_auth(oauth_provider, oauth_id)'

\echo ''
\echo 'Auth Service indexes created successfully!'
\echo ''

-- ============================================
-- USER SERVICE DATABASE
-- ============================================
-- Connect to user-service database:
-- \c user-service

\echo '================================================'
\echo 'Creating indexes for user-service database...'
\echo '================================================'

-- User Profiles Performance
-- Username is already unique, but index helps
CREATE INDEX IF NOT EXISTS idx_user_profiles_username 
    ON user_profiles(username);
\echo '✓ Created index on user_profiles.username'

-- For online user queries
CREATE INDEX IF NOT EXISTS idx_user_profiles_is_online 
    ON user_profiles(is_online);
\echo '✓ Created index on user_profiles.is_online'

-- For activity-based queries
CREATE INDEX IF NOT EXISTS idx_user_profiles_last_pulse_at 
    ON user_profiles(last_pulse_at);
\echo '✓ Created index on user_profiles.last_pulse_at'

-- Follows Performance (Critical for social features)
-- Used for: Follow/unfollow, follower lists, following lists
CREATE INDEX IF NOT EXISTS idx_follows_follower_id 
    ON follows(follower_id);
\echo '✓ Created index on follows.follower_id'

CREATE INDEX IF NOT EXISTS idx_follows_following_id 
    ON follows(following_id);
\echo '✓ Created index on follows.following_id'

-- Composite index for follow status checks
CREATE INDEX IF NOT EXISTS idx_follows_both_ids 
    ON follows(follower_id, following_id);
\echo '✓ Created composite index on follows(follower_id, following_id)'

-- User Stats Performance
-- Primary key is already indexed, but add covering index
CREATE INDEX IF NOT EXISTS idx_user_stats_counts 
    ON user_stats(user_id, followers_count, following_count, pulses_count);
\echo '✓ Created covering index on user_stats'

\echo ''
\echo 'User Service indexes created successfully!'
\echo ''

-- ============================================
-- Verification Queries
-- ============================================

\echo '================================================'
\echo 'Verifying indexes...'
\echo '================================================'

-- Check indexes on auth-service
\echo 'Auth Service Indexes:'
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
    AND tablename IN ('refresh_tokens', 'otp_codes', 'users_auth')
ORDER BY tablename, indexname;

-- Check indexes on user-service  
\echo 'User Service Indexes:'
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'public'
    AND tablename IN ('user_profiles', 'follows', 'user_stats')
ORDER BY tablename, indexname;

\echo ''
\echo '================================================'
\echo 'Index creation complete!'
\echo '================================================'
\echo ''
\echo 'Performance improvements applied:'
\echo '  ✓ Faster token validation'
\echo '  ✓ Faster logout operations'
\echo '  ✓ Faster follower/following queries'
\echo '  ✓ Faster user profile lookups'
\echo '  ✓ Optimized composite queries'
\echo ''
\echo 'Recommended next steps:'
\echo '  1. Restart services to ensure indexes are used'
\echo '  2. Run ANALYZE on tables: ANALYZE table_name;'
\echo '  3. Monitor query performance with EXPLAIN ANALYZE'
\echo ''

-- ============================================
-- Performance Testing Queries
-- ============================================

\echo 'Test index usage with these queries:'
\echo ''
\echo '-- Test token lookup (should use idx_refresh_tokens_token_hash):'
\echo 'EXPLAIN ANALYZE SELECT * FROM refresh_tokens WHERE token_hash = ''some_hash'';'
\echo ''
\echo '-- Test follow lookup (should use idx_follows_both_ids):'
\echo 'EXPLAIN ANALYZE SELECT * FROM follows WHERE follower_id = ''uuid'' AND following_id = ''uuid'';'
\echo ''
\echo '-- Test followers count (should use idx_follows_following_id):'
\echo 'EXPLAIN ANALYZE SELECT COUNT(*) FROM follows WHERE following_id = ''uuid'';'
\echo ''

-- ============================================
-- Maintenance Queries
-- ============================================

\echo 'Maintenance commands (run periodically):'
\echo ''
\echo '-- Update table statistics:'
\echo 'ANALYZE refresh_tokens;'
\echo 'ANALYZE otp_codes;'
\echo 'ANALYZE users_auth;'
\echo 'ANALYZE user_profiles;'
\echo 'ANALYZE follows;'
\echo 'ANALYZE user_stats;'
\echo ''
\echo '-- Reindex if needed (during low traffic):'
\echo 'REINDEX TABLE refresh_tokens;'
\echo 'REINDEX TABLE follows;'
\echo ''

-- ============================================
-- Drop Indexes (If Needed)
-- ============================================

\echo 'To remove indexes (if needed), run:'
\echo 'DROP INDEX IF EXISTS idx_refresh_tokens_user_id;'
\echo 'DROP INDEX IF EXISTS idx_follows_follower_id;'
\echo '-- (repeat for other indexes)'
\echo ''
