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
                        // Public endpoints - no authentication required
                        .pathMatchers("/auth-service/users").permitAll()
                        .pathMatchers("/auth-service/auth/token").permitAll()
                        .pathMatchers("/auth-service/auth/introspect").permitAll()
                        .pathMatchers("/auth-service/auth/logout").permitAll()
                        .pathMatchers("/auth-service/auth/refresh-token").permitAll()
                        .pathMatchers("/genzf/assets/**").permitAll()
                        .pathMatchers("/genzf/chart-data/**").permitAll()
                        .pathMatchers("/genzf/swagger-ui/**").permitAll()
                        .pathMatchers("/genzf/api-docs/**").permitAll()
                        .pathMatchers("/genzf/swagger-ui.html").permitAll()
                        // All other requests require authentication
                        .anyExchange().authenticated()
                )
                // Add custom JWT filter before authorization
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }

}
