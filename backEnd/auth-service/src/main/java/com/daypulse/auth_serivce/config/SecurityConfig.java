package com.daypulse.auth_serivce.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
     * - /auth/signup, /auth/register: Create new account (no token needed)
     * - /auth/login: Authenticate with credentials (no token needed)
     * - /auth/introspect: Validate token (used by gateway and services)
     * - /auth/refresh: Get new access token using refresh token
     * - /auth/verify-otp, /auth/forgot-password: Future email verification features
     */
    private final String[] PUBLIC_ENDPOINTS = {
            "/users",
            "/auth/signup",
            "/auth/register",  // Keep for backward compatibility
            "/auth/login",
            "/auth/introspect",
            "/auth/refresh",
            "/auth/verify-otp",
            "/auth/forgot-password"
    };

    /**
     * PROTECTED_ENDPOINTS: Require valid JWT in Authorization header
     * - /auth/logout: Revoke tokens (requires authenticated user)
     */
    private final String[] PROTECTED_ENDPOINTS = {
            "/auth/logout"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity, CustomJwtDecoder customJwtDecoder) throws Exception {
        httpSecurity.authorizeHttpRequests(request ->
                request
                        // Public endpoints - no authentication required
                        .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()
                        // Protected endpoints - require valid JWT in Authorization header
                        .requestMatchers(HttpMethod.POST, PROTECTED_ENDPOINTS).authenticated()
                        // All other requests require authentication
                        .anyRequest().authenticated());

        httpSecurity.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwtConfigurer ->
                                jwtConfigurer.decoder(customJwtDecoder)
                                        .jwtAuthenticationConverter(jwtAuthenticationConverter())) // When decoding the JWT, convert to Auth object
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint()) // When authentication fails
        );
        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(){
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    CustomJwtDecoder customJwtDecoder(@Value("${jwt.signing-key}") String signingKey) {
        return new CustomJwtDecoder(signingKey);
    }
}
