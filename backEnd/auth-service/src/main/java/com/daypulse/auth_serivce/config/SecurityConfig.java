package com.daypulse.auth_serivce.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * PUBLIC_ENDPOINTS: No authentication required
     * - /auth/signup, /auth/register: Create new account (uses Keycloak Admin API)
     * - /auth/login: Authenticate with credentials (exchanges for Keycloak token)
     * - /auth/refresh: Get new access token using refresh token
     * - /auth/verify-otp, /auth/forgot-password: Future email verification features
     * 
     * Note: /auth/introspect endpoint removed - Gateway now validates JWT directly
     * with Keycloak
     * Note: /auth/logout now requires authentication (moved to PROTECTED_ENDPOINTS)
     */
    private final String[] PUBLIC_ENDPOINTS = {
            "/users",
            "/auth/signup",
            "/auth/register", // Keep for backward compatibility
            "/auth/login",
            "/auth/introspect",
            "/auth/refresh",
            "/auth/verify-otp",
            "/auth/forgot-password"
    };

    /**
     * PROTECTED_ENDPOINTS: Require valid Keycloak JWT in Authorization header
     * - /auth/logout: Revoke tokens (requires authenticated user)
     */
    private final String[] PROTECTED_ENDPOINTS = {
            "/auth/logout",
            "/users/my-info"
    };

    /**
     * ADMIN_ENDPOINTS: Require ADMIN role from Keycloak
     * - /admin/**: All admin operations
     */
    private final String[] ADMIN_ENDPOINTS = {
            "/admin/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests(request -> request
                // Public endpoints - no authentication required
                .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_ENDPOINTS).permitAll()
                // Admin endpoints - require ADMIN role from Keycloak
                .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")
                // Protected endpoints - require valid Keycloak JWT
                .requestMatchers(HttpMethod.POST, PROTECTED_ENDPOINTS).authenticated()
                .requestMatchers(HttpMethod.GET, PROTECTED_ENDPOINTS).authenticated()
                // All other requests require authentication
                .anyRequest().authenticated());

        // Configure OAuth2 Resource Server to validate Keycloak JWTs
        httpSecurity.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()));

        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    /**
     * Convert Keycloak JWT claims to Spring Security authorities
     * Maps Keycloak realm roles to Spring Security roles (with ROLE_ prefix)
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // Keycloak stores realm roles in "realm_access.roles" claim
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");

        // Spring Security expects "ROLE_" prefix
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
