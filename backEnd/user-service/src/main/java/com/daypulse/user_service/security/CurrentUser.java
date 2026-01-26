package com.daypulse.user_service.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utility class to extract current user information from security context.
 * 
 * This replaces manual @RequestHeader("X-User-Id") usage in controllers.
 */
public class CurrentUser {

    /**
     * Get current user ID from security context.
     * 
     * @return User ID as UUID
     * @throws IllegalStateException if user is not authenticated
     */
    public static UUID getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        String userId = authentication.getName();
        return UUID.fromString(userId);
    }

    /**
     * Get current user ID as String from security context.
     * 
     * @return User ID as String
     * @throws IllegalStateException if user is not authenticated
     */
    public static String getUserIdAsString() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        return authentication.getName();
    }

    /**
     * Check if user has a specific role.
     * 
     * @param role Role to check (e.g., "ROLE_ADMIN")
     * @return true if user has the role
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
