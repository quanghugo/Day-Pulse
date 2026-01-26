package com.daypulse.api_gateway.security;

/**
 * Centralized security constants for the API Gateway.
 * 
 * These constants define header names, claim names, and other security-related
 * values used throughout the authentication and authorization flow.
 * 
 * STANDARD: Following OAuth 2.0 Bearer Token Usage (RFC 6750) and
 * Spring Security conventions.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Utility class - prevent instantiation
    }

    /**
     * Authorization header prefix for Bearer tokens.
     * STANDARD: RFC 6750 - OAuth 2.0 Bearer Token Usage
     */
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * Internal header name for user ID.
     * Used by gateway to forward authenticated user ID to downstream services.
     * Downstream services MUST validate this header comes from trusted gateway.
     */
    public static final String HEADER_USER_ID = "X-User-Id";

    /**
     * Internal header name for user roles.
     * Used by gateway to forward user roles/authorities to downstream services.
     * Format: space-separated roles (e.g., "ROLE_USER ROLE_ADMIN")
     */
    public static final String HEADER_USER_ROLES = "X-User-Roles";

    /**
     * Internal header name for gateway signature.
     * Used for service-to-service authentication to verify request comes from gateway.
     * Format: HMAC-SHA256 signature of request data
     */
    public static final String HEADER_GATEWAY_SIGNATURE = "X-Gateway-Signature";

    /**
     * Internal header name for request timestamp.
     * Used with gateway signature for replay attack prevention.
     * Format: Unix timestamp in seconds
     */
    public static final String HEADER_GATEWAY_TIMESTAMP = "X-Gateway-Timestamp";

    /**
     * JWT claim name for user ID.
     * Extracted from JWT token and forwarded to downstream services.
     */
    public static final String CLAIM_USER_ID = "userId";

    /**
     * JWT claim name for user scope/roles.
     * Contains space-separated roles (e.g., "ROLE_USER ROLE_ADMIN")
     */
    public static final String CLAIM_SCOPE = "scope";

    /**
     * JWT claim name for subject (typically user email).
     */
    public static final String CLAIM_SUBJECT = "sub";

    /**
     * Default role assigned when no scope is present in JWT.
     */
    public static final String DEFAULT_ROLE = "ROLE_USER";

    /**
     * Maximum age for gateway signature timestamp (in seconds).
     * Prevents replay attacks by rejecting old requests.
     */
    public static final long MAX_SIGNATURE_AGE_SECONDS = 300; // 5 minutes

    /**
     * Cache TTL for token introspection results (in seconds).
     * Reduces load on auth-service while maintaining security.
     */
    public static final long INTROSPECTION_CACHE_TTL_SECONDS = 30;
}
