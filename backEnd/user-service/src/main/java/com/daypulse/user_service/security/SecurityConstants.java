package com.daypulse.user_service.security;

/**
 * Security constants for user-service.
 * 
 * These constants must match the gateway's SecurityConstants to ensure
 * proper service-to-service authentication.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Utility class - prevent instantiation
    }

    /**
     * Internal header name for user ID from gateway.
     */
    public static final String HEADER_USER_ID = "X-User-Id";

    /**
     * Internal header name for user roles from gateway.
     */
    public static final String HEADER_USER_ROLES = "X-User-Roles";

    /**
     * Internal header name for gateway signature.
     */
    public static final String HEADER_GATEWAY_SIGNATURE = "X-Gateway-Signature";

    /**
     * Internal header name for request timestamp.
     */
    public static final String HEADER_GATEWAY_TIMESTAMP = "X-Gateway-Timestamp";

    /**
     * Maximum age for gateway signature timestamp (in seconds).
     */
    public static final long MAX_SIGNATURE_AGE_SECONDS = 300; // 5 minutes
}
