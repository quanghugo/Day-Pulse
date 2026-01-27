package com.daypulse.api_gateway.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Keycloak JWT validation in API Gateway
 * 
 * Prerequisites:
 * - Keycloak must be running on http://localhost:8888
 * - Realm 'daypulse' must exist
 * - A valid Keycloak JWT token is required for testing
 * 
 * Note: These tests require a real Keycloak token. You can obtain one by:
 * 1. Logging in via Keycloak authentication service
 * 2. Using Keycloak's token endpoint directly
 * 3. Using a test token from Keycloak admin console
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "keycloak.issuer-uri=http://localhost:8888/realms/daypulse",
    "keycloak.jwk-set-uri=http://localhost:8888/realms/daypulse/protocol/openid-connect/certs"
})
class KeycloakJwtValidationTest {

    @Autowired
    private ReactiveJwtDecoder jwtDecoder;

    // This should be a valid Keycloak JWT token for testing
    // In a real scenario, you would obtain this from Keycloak after authentication
    private String validKeycloakToken;

    @BeforeEach
    void setUp() {
        // TODO: Obtain a valid Keycloak token for testing
        // This can be done by:
        // 1. Calling the auth service login endpoint
        // 2. Using Keycloak's token endpoint directly
        // 3. Using a test token from Keycloak
        validKeycloakToken = System.getenv("TEST_KEYCLOAK_TOKEN");
    }

    @Test
    void testJwtDecoderBeanExists() {
        assertNotNull(jwtDecoder, "JWT decoder bean should be configured");
    }

    @Test
    void testDecodeValidKeycloakToken() {
        if (validKeycloakToken == null || validKeycloakToken.isEmpty()) {
            // Skip test if no token provided
            System.out.println("Skipping test - no valid Keycloak token provided");
            return;
        }

        // When
        Mono<Jwt> jwtMono = jwtDecoder.decode(validKeycloakToken);

        // Then
        StepVerifier.create(jwtMono)
                .assertNext(jwt -> {
                    assertNotNull(jwt, "JWT should not be null");
                    assertNotNull(jwt.getId(), "JWT should have an ID");
                    assertNotNull(jwt.getSubject(), "JWT should have a subject");
                    assertTrue(jwt.getExpiresAt().isAfter(Instant.now()), 
                            "JWT should not be expired");
                })
                .verifyComplete();
    }

    @Test
    void testExtractKeycloakClaims() {
        if (validKeycloakToken == null || validKeycloakToken.isEmpty()) {
            System.out.println("Skipping test - no valid Keycloak token provided");
            return;
        }

        // When
        Mono<Jwt> jwtMono = jwtDecoder.decode(validKeycloakToken);

        // Then
        StepVerifier.create(jwtMono)
                .assertNext(jwt -> {
                    // Check standard claims
                    assertNotNull(jwt.getSubject(), "Subject claim should exist");
                    assertNotNull(jwt.getIssuer(), "Issuer claim should exist");
                    
                    // Check Keycloak-specific claims
                    String preferredUsername = jwt.getClaimAsString("preferred_username");
                    String email = jwt.getClaimAsString("email");
                    
                    // At least one of these should be present
                    assertTrue(preferredUsername != null || email != null, 
                            "Either preferred_username or email should be present");
                    
                    // Check realm_access.roles
                    @SuppressWarnings("unchecked")
                    Map<String, Object> realmAccess = jwt.getClaim("realm_access");
                    if (realmAccess != null) {
                        @SuppressWarnings("unchecked")
                        List<String> roles = (List<String>) realmAccess.get("roles");
                        assertNotNull(roles, "Roles should be present in realm_access");
                    }
                })
                .verifyComplete();
    }

    @Test
    void testInvalidToken() {
        String invalidToken = "invalid.token.here";

        // When
        Mono<Jwt> jwtMono = jwtDecoder.decode(invalidToken);

        // Then
        StepVerifier.create(jwtMono)
                .expectError()
                .verify();
    }

    @Test
    void testExpiredToken() {
        // This test requires an expired token
        // In a real scenario, you would create an expired token or wait for one to expire
        String expiredToken = System.getenv("TEST_EXPIRED_KEYCLOAK_TOKEN");
        
        if (expiredToken == null || expiredToken.isEmpty()) {
            System.out.println("Skipping test - no expired Keycloak token provided");
            return;
        }

        // When
        Mono<Jwt> jwtMono = jwtDecoder.decode(expiredToken);

        // Then - Should still decode but validation should fail
        StepVerifier.create(jwtMono)
                .expectError()
                .verify();
    }
}
