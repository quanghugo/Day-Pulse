package com.daypulse.auth_serivce.service;

import com.daypulse.auth_serivce.dto.request.*;
import com.daypulse.auth_serivce.dto.response.*;
import com.daypulse.auth_serivce.entity.UserAuth;
import com.daypulse.auth_serivce.enums.RoleEnum;
import com.daypulse.auth_serivce.exception.AppException;
import com.daypulse.auth_serivce.exception.ErrorCode;
import com.daypulse.auth_serivce.mapper.UserMapper;
import com.daypulse.auth_serivce.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.ws.rs.core.Response;
import java.util.*;

/**
 * Authentication Service - Keycloak Integration
 * 
 * This service integrates with Keycloak for authentication and user management:
 * - User registration via Keycloak Admin API
 * - Login via Keycloak token endpoint (Direct Access Grant / Resource Owner
 * Password Credentials)
 * - Token refresh using Keycloak
 * - Logout via Keycloak session termination
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KeycloakAuthenticationService {

    @Value("${keycloak.realm}")
    String realm;

    @Value("${keycloak.auth-server-url}")
    String authServerUrl;

    @Value("${keycloak.resource}")
    String clientId;

    @Value("${keycloak.credentials.secret}")
    String clientSecret;

    Keycloak keycloakAdminClient;
    UserRepository userRepository;
    UserMapper userMapper;
    RestTemplate restTemplate = new RestTemplate();

    /**
     * Register new user in Keycloak
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // Check if user already exists in local database
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        try {
            // Get users resource from Keycloak
            RealmResource realmResource = keycloakAdminClient.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Create user representation
            UserRepresentation keycloakUser = new UserRepresentation();
            keycloakUser.setUsername(request.getEmail());
            keycloakUser.setEmail(request.getEmail());
            keycloakUser.setEnabled(true);
            keycloakUser.setEmailVerified(false); // User needs to verify email

            // Create user in Keycloak
            Response response = usersResource.create(keycloakUser);

            if (response.getStatus() != 201) {
                log.error("Failed to create user in Keycloak. Status: {}", response.getStatus());
                throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
            }

            // Get the created user's ID from Location header
            String locationHeader = response.getHeaderString("Location");
            String keycloakUserId = locationHeader.substring(locationHeader.lastIndexOf('/') + 1);
            response.close();

            // Set user password
            UserResource userResource = usersResource.get(keycloakUserId);
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getPassword());
            credential.setTemporary(false); // Password is permanent
            userResource.resetPassword(credential);

            // Assign default role (USER)
            RoleRepresentation userRole = realmResource.roles().get("USER").toRepresentation();
            userResource.roles().realmLevel().add(Collections.singletonList(userRole));

            // Create local user record with Keycloak ID
            UserAuth localUser = UserAuth.builder()
                    .email(request.getEmail())
                    .keycloakId(UUID.fromString(keycloakUserId))
                    .role(RoleEnum.USER)
                    .isEmailVerified(false)
                    .isSetupComplete(false)
                    .build();

            userRepository.save(localUser);

            log.info("User registered successfully: {}", request.getEmail());

            // TODO: [FUTURE-KAFKA] Publish event: auth.user.registered
            // TODO: [FUTURE-EMAIL] Send verification email

            return RegisterResponse.builder()
                    .success(true)
                    .userId(localUser.getId())
                    .email(localUser.getEmail())
                    .build();

        } catch (Exception e) {
            log.error("Error registering user: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    /**
     * Authenticate user and get Keycloak tokens
     * Uses Direct Access Grant (Resource Owner Password Credentials flow)
     */
    public AuthenticationResponse authenticate(LoginRequest request) {
        try {
            // Get tokens from Keycloak token endpoint
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                    authServerUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("grant_type", OAuth2Constants.PASSWORD);
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);
            requestBody.add("username", request.getEmail());
            requestBody.add("password", request.getPassword());
            requestBody.add("scope", "openid profile email");

            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    httpEntity,
                    Map.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            Map<String, Object> tokenResponse = response.getBody();

            // Get user from local database
            UserAuth user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            // Build response
            return AuthenticationResponse.builder()
                    .user(userMapper.toUserSummary(user))
                    .tokens(TokenPair.builder()
                            .accessToken((String) tokenResponse.get("access_token"))
                            .refreshToken((String) tokenResponse.get("refresh_token"))
                            .expiresIn(((Number) tokenResponse.get("expires_in")).longValue())
                            .tokenType("Bearer")
                            .build())
                    .build();

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        try {
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                    authServerUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("grant_type", OAuth2Constants.REFRESH_TOKEN);
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);
            requestBody.add("refresh_token", request.getToken());

            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    httpEntity,
                    Map.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            Map<String, Object> tokenResponse = response.getBody();

            // Extract user email from the old refresh token or decode it
            // For now, we'll return just the tokens
            return AuthenticationResponse.builder()
                    .user(null) // Could extract from token claims if needed
                    .tokens(TokenPair.builder()
                            .accessToken((String) tokenResponse.get("access_token"))
                            .refreshToken((String) tokenResponse.get("refresh_token"))
                            .expiresIn(((Number) tokenResponse.get("expires_in")).longValue())
                            .tokenType("Bearer")
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    /**
     * Logout user by revoking tokens in Keycloak
     */
    @Transactional
    public void logout(String accessToken) {
        try {
            String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout",
                    authServerUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBearerAuth(accessToken);

            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(requestBody, headers);

            restTemplate.exchange(
                    logoutUrl,
                    HttpMethod.POST,
                    httpEntity,
                    Void.class);

            log.info("User logged out successfully");

        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    /**
     * Introspect token to check if it's valid
     * Note: This is typically done by the Gateway, but kept for backward
     * compatibility
     */
    public IntrospectResponse introspect(IntrospectRequest request) {
        try {
            String introspectUrl = String.format("%s/realms/%s/protocol/openid-connect/token/introspect",
                    authServerUrl, realm);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("token", request.getToken());
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    introspectUrl,
                    HttpMethod.POST,
                    httpEntity,
                    Map.class);

            if (response.getBody() != null) {
                Boolean active = (Boolean) response.getBody().get("active");
                return IntrospectResponse.builder()
                        .valid(Boolean.TRUE.equals(active))
                        .build();
            }

            return IntrospectResponse.builder()
                    .valid(false)
                    .build();

        } catch (Exception e) {
            log.error("Token introspection failed: {}", e.getMessage(), e);
            return IntrospectResponse.builder()
                    .valid(false)
                    .build();
        }
    }

    // Placeholder methods for future OTP functionality
    public AuthenticationResponse verifyOtp(VerifyOtpRequest request) {
        throw new UnsupportedOperationException("OTP verification not yet implemented with Keycloak");
    }

    public ApiBaseResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        // TODO: Implement using Keycloak's forgot password flow
        // This would typically trigger Keycloak to send a password reset email
        throw new UnsupportedOperationException("Forgot password not yet implemented with Keycloak");
    }
}
