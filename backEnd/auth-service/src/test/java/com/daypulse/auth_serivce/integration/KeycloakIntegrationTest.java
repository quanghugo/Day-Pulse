package com.daypulse.auth_serivce.integration;

import com.daypulse.auth_serivce.dto.request.LoginRequest;
import com.daypulse.auth_serivce.dto.request.RegisterRequest;
import com.daypulse.auth_serivce.dto.response.AuthenticationResponse;
import com.daypulse.auth_serivce.dto.response.IntrospectResponse;
import com.daypulse.auth_serivce.dto.response.RegisterResponse;
import com.daypulse.auth_serivce.service.KeycloakAuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Keycloak authentication service
 * 
 * Prerequisites:
 * - Keycloak must be running on http://localhost:8888
 * - Realm 'daypulse' must exist
 * - Client 'daypulse-backend' must be configured
 * - Test user should be created in Keycloak (or created via registration test)
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "keycloak.realm=daypulse",
    "keycloak.auth-server-url=http://localhost:8888",
    "keycloak.resource=daypulse-backend",
    "keycloak.credentials.secret=${KEYCLOAK_CLIENT_SECRET:qtgqKuMmHvCjuvDZxWJjZgcCSh6wQtin}",
    "keycloak.admin.server-url=http://localhost:8888",
    "keycloak.admin.realm=master",
    "keycloak.admin.client-id=admin-cli",
    "keycloak.admin.username=admin",
    "keycloak.admin.password=admin"
})
class KeycloakIntegrationTest {

    @Autowired
    private KeycloakAuthenticationService keycloakAuthenticationService;

    private static final String TEST_EMAIL = "testuser@example.com";
    private static final String TEST_PASSWORD = "TestPassword123!";
    private static final String TEST_EMAIL_2 = "testuser2@example.com";
    private static final String TEST_PASSWORD_2 = "TestPassword456!";

    @BeforeEach
    void setUp() {
        // Clean up test users if they exist (optional - can be done manually)
    }

    @Test
    void testUserRegistration() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .email(TEST_EMAIL_2)
                .password(TEST_PASSWORD_2)
                .build();

        // When
        RegisterResponse response = keycloakAuthenticationService.register(request);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getUserId());
        assertEquals(TEST_EMAIL_2, response.getEmail());
    }

    @Test
    void testUserLogin() {
        // Given - User must exist in Keycloak (create via registration or manually)
        LoginRequest request = LoginRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();

        // When
        AuthenticationResponse response = keycloakAuthenticationService.authenticate(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getTokens());
        assertNotNull(response.getTokens().getAccessToken());
        assertNotNull(response.getTokens().getRefreshToken());
        assertNotNull(response.getUser());
        assertEquals("Bearer", response.getTokens().getTokenType());
        assertTrue(response.getTokens().getExpiresIn() > 0);
    }

    @Test
    void testTokenRefresh() {
        // Given - First login to get tokens
        LoginRequest loginRequest = LoginRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();
        AuthenticationResponse loginResponse = keycloakAuthenticationService.authenticate(loginRequest);
        String refreshToken = loginResponse.getTokens().getRefreshToken();

        // When - Refresh token
        com.daypulse.auth_serivce.dto.request.RefreshTokenRequest refreshRequest = 
                com.daypulse.auth_serivce.dto.request.RefreshTokenRequest.builder()
                        .token(refreshToken)
                        .build();
        AuthenticationResponse refreshResponse = keycloakAuthenticationService.refreshToken(refreshRequest);

        // Then
        assertNotNull(refreshResponse);
        assertNotNull(refreshResponse.getTokens());
        assertNotNull(refreshResponse.getTokens().getAccessToken());
        assertNotNull(refreshResponse.getTokens().getRefreshToken());
        // New tokens should be different from old ones
        assertNotEquals(loginResponse.getTokens().getAccessToken(), 
                refreshResponse.getTokens().getAccessToken());
    }

    @Test
    void testTokenIntrospection() {
        // Given - Login to get a token
        LoginRequest loginRequest = LoginRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();
        AuthenticationResponse loginResponse = keycloakAuthenticationService.authenticate(loginRequest);
        String accessToken = loginResponse.getTokens().getAccessToken();

        // When - Introspect token
        com.daypulse.auth_serivce.dto.request.IntrospectRequest introspectRequest = 
                com.daypulse.auth_serivce.dto.request.IntrospectRequest.builder()
                        .token(accessToken)
                        .build();
        IntrospectResponse introspectResponse = keycloakAuthenticationService.introspect(introspectRequest);

        // Then
        assertNotNull(introspectResponse);
        assertTrue(introspectResponse.isValid());
    }

    @Test
    void testInvalidLogin() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("WrongPassword")
                .build();

        // When/Then
        assertThrows(Exception.class, () -> {
            keycloakAuthenticationService.authenticate(request);
        });
    }

    @Test
    void testDuplicateRegistration() {
        // Given - Register a user first
        RegisterRequest firstRequest = RegisterRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();
        keycloakAuthenticationService.register(firstRequest);

        // When/Then - Try to register same email again
        RegisterRequest duplicateRequest = RegisterRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .build();
        assertThrows(Exception.class, () -> {
            keycloakAuthenticationService.register(duplicateRequest);
        });
    }
}
