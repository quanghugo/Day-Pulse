package com.daypulse.api_gateway.security;

import com.daypulse.api_gateway.exception.AuthorizationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gateway Authorization Filter
 * 
 * Enforces role-based access control (RBAC) at the gateway level.
 * Validates that authenticated users have required roles for protected endpoints.
 * 
 * This filter runs after authentication and checks if the user's roles
 * match the endpoint's required roles.
 */
@Slf4j
@Component
public class GatewayAuthorizationFilter implements WebFilter {

    /**
     * Endpoints that require ADMIN role.
     */
    private static final Set<String> ADMIN_ENDPOINTS = Set.of(
            "/api/v1/admin/**"
    );

    /**
     * Endpoints that require MODERATOR role.
     */
    private static final Set<String> MODERATOR_ENDPOINTS = Set.of(
            "/api/v1/moderator/**"
    );

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Check if endpoint requires specific role
        String requiredRole = getRequiredRole(path);
        if (requiredRole == null) {
            // No specific role required, continue
            return chain.filter(exchange);
        }

        // Get authentication from security context
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    if (authentication == null || !authentication.isAuthenticated()) {
                        log.warn("Unauthenticated request to protected endpoint: {}", path);
                        return Mono.error(new AuthorizationException("Authentication required"));
                    }

                    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                    Set<String> userRoles = authorities.stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());

                    if (!hasRequiredRole(userRoles, requiredRole)) {
                        log.warn("User {} lacks required role {} for endpoint: {}",
                                authentication.getName(), requiredRole, path);
                        return Mono.error(new AuthorizationException(
                                "Insufficient permissions. Required role: " + requiredRole));
                    }

                    log.debug("Authorization granted for user {} on endpoint: {}",
                            authentication.getName(), path);
                    return chain.filter(exchange);
                })
                .switchIfEmpty(Mono.error(new AuthorizationException("Authentication required")));
    }

    /**
     * Get required role for endpoint path.
     */
    private String getRequiredRole(String path) {
        for (String adminEndpoint : ADMIN_ENDPOINTS) {
            if (matchesPath(path, adminEndpoint)) {
                return "ROLE_ADMIN";
            }
        }
        for (String moderatorEndpoint : MODERATOR_ENDPOINTS) {
            if (matchesPath(path, moderatorEndpoint)) {
                return "ROLE_MODERATOR";
            }
        }
        return null;
    }

    /**
     * Check if path matches pattern (simple wildcard matching).
     */
    private boolean matchesPath(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return path.equals(pattern);
    }

    /**
     * Check if user has required role.
     * ADMIN role has access to all endpoints.
     */
    private boolean hasRequiredRole(Set<String> userRoles, String requiredRole) {
        if (userRoles.contains("ROLE_ADMIN")) {
            return true; // Admin has access to everything
        }
        return userRoles.contains(requiredRole);
    }
}
