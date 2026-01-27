package com.daypulse.auth_serivce.controller;

import com.daypulse.auth_serivce.dto.request.*;
import com.daypulse.auth_serivce.dto.response.*;
import com.daypulse.auth_serivce.exception.AppException;
import com.daypulse.auth_serivce.exception.ErrorCode;
import com.daypulse.auth_serivce.service.KeycloakAuthenticationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    KeycloakAuthenticationService authenticationService;

    /**
     * POST /auth/signup - Public endpoint
     * Creates a new user account in Keycloak. No authentication required.
     * Request body: email, password
     * Response: user info (email, id)
     */
    @PostMapping("/signup")
    public ApiBaseResponse<RegisterResponse> signup(@RequestBody @Valid RegisterRequest request) {
        var result = authenticationService.register(request);
        return ApiBaseResponse.<RegisterResponse>builder()
                .result(result)
                .build();
    }

    // Deprecated: Use /auth/signup instead
    @Deprecated
    @PostMapping("/register")
    public ApiBaseResponse<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        return signup(request);
    }

    /**
     * POST /auth/login - Public endpoint
     * Authenticates user with email and password using Keycloak.
     * Request body: email, password
     * Response: Keycloak access token, refresh token, user info
     */
    @PostMapping("/login")
    public ApiBaseResponse<AuthenticationResponse> login(@RequestBody @Valid LoginRequest request) {
        var result = authenticationService.authenticate(request);
        return ApiBaseResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    /**
     * POST /auth/introspect - Public endpoint (but requires token in body)
     * Validates if a Keycloak token is still valid (not expired or revoked).
     * Note: Gateway should validate tokens directly with Keycloak JWK endpoint.
     * This endpoint is kept for backward compatibility.
     * Request body: token
     * Response: valid (boolean)
     */
    @PostMapping("/introspect")
    public ApiBaseResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest introspectRequest) {
        var result = authenticationService.introspect(introspectRequest);
        return ApiBaseResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    /**
     * POST /auth/logout - Protected endpoint
     * Revokes user's Keycloak session and tokens.
     * Requires Authorization: Bearer <access_token> header
     * Request header: Authorization: Bearer <token>
     * Response: success message
     */
    @PostMapping("/logout")
    public ApiBaseResponse<Void> logout(@RequestHeader("Authorization") String authHeader) {
        // Extract token from "Bearer <token>" format
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        if (token == null || token.isEmpty()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        authenticationService.logout(token);
        return ApiBaseResponse.<Void>builder()
                .message("Logout successful")
                .build();
    }

    /**
     * POST /auth/refresh - Public endpoint (but requires refresh token)
     * Generates a new access token using a valid Keycloak refresh token.
     * Request body: token (refresh token)
     * Response: new accessToken, new refreshToken
     */
    @PostMapping("/refresh")
    public ApiBaseResponse<AuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        var result = authenticationService.refreshToken(request);
        return ApiBaseResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    // TODO: [FUTURE-EMAIL] Implement OTP verification with Keycloak
    @PostMapping("/verify-otp")
    public ApiBaseResponse<AuthenticationResponse> verifyOtp(@RequestBody @Valid VerifyOtpRequest request) {
        var result = authenticationService.verifyOtp(request);
        return ApiBaseResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    // TODO: [FUTURE-EMAIL] Implement forgot password with Keycloak
    @PostMapping("/forgot-password")
    public ApiBaseResponse<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        return authenticationService.forgotPassword(request);
    }
}
