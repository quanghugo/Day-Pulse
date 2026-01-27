package com.daypulse.api_gateway.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * Configuration for JWT decoder using Keycloak JWK endpoint
 * Validates Keycloak-issued JWT tokens using public keys from JWK set
 */
@Configuration
public class JwtDecoderConfig {

    @Value("${keycloak.jwk-set-uri}")
    private String jwkSetUri;

    /**
     * Creates a ReactiveJwtDecoder that validates Keycloak JWTs
     * using the JWK (JSON Web Key) set endpoint
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri)
                .build();
    }
}
