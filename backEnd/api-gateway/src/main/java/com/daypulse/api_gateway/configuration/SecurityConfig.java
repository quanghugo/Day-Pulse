package com.daypulse.api_gateway.configuration;

import com.daypulse.api_gateway.security.GatewayJwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final GatewayJwtAuthenticationFilter jwtAuthenticationFilter;

    private final String[] PUBLIC_ENDPOINTS = {
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/refresh",
        "/api/v1/auth/verify-otp",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/introspect"
    };

    private final String[] PROTECTED_ENDPOINTS = {
        "/api/v1/auth/logout",
        "/api/v1/users/**"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints - Authentication not required
                        .pathMatchers(PUBLIC_ENDPOINTS).permitAll()
                        
                        // Protected endpoints - Authentication required
                        .pathMatchers(PROTECTED_ENDPOINTS).authenticated()
                        
                        // All other requests require authentication
                        .anyExchange().authenticated()
                )
                // Add custom JWT filter before authorization
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }

}
