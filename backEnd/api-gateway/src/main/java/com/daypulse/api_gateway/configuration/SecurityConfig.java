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

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints - Authentication not required
                        .pathMatchers("/api/v1/auth/login").permitAll()
                        .pathMatchers("/api/v1/auth/register").permitAll()
                        .pathMatchers("/api/v1/auth/refresh").permitAll()
                        .pathMatchers("/api/v1/auth/verify-otp").permitAll()
                        .pathMatchers("/api/v1/auth/forgot-password").permitAll()
                        .pathMatchers("/api/v1/auth/introspect").permitAll()
                        
                        // Protected endpoints - Authentication required
                        .pathMatchers("/api/v1/auth/logout").authenticated()
                        .pathMatchers("/api/v1/users/**").authenticated()
                        
                        // All other requests require authentication
                        .anyExchange().authenticated()
                )
                // Add custom JWT filter before authorization
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }

}
