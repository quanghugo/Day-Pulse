package com.daypulse.auth_serivce.controller;

import com.daypulse.auth_serivce.dto.request.*;
import com.daypulse.auth_serivce.dto.response.*;
import com.daypulse.auth_serivce.exception.AppException;
import com.daypulse.auth_serivce.exception.ErrorCode;
import com.daypulse.auth_serivce.service.AuthenticationService;
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
    AuthenticationService authenticationService;

    /**
     * POST /auth/signup - Public endpoint
     * Creates a new user account. No authentication required.
     * Request body: email, password
     * Response: user info (email, id) and optionally auto-login tokens
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
     * Authenticates user with email and password. No prior authentication required.
     * Request body: email, password
     * Response: accessToken, refreshToken, user info
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
     * Validates if a token is still valid (not expired or revoked).
     * Used by API Gateway and other services for token validation.
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
     * Revokes user's refresh tokens and optionally blacklists the access token.
     * STANDARD: Requires Authorization: Bearer <access_token> header
     * Request header: Authorization: Bearer <token>
     * Response: success message
     */
    @PostMapping("/logout")
    public ApiBaseResponse<Void> logout(@RequestHeader("Authorization") String authHeader) throws Exception {
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
     * Generates a new access token using a valid refresh token.
     * STANDARD: Refresh token can be sent in request body.
     * In production, prefer HttpOnly cookies for refresh tokens.
     * Request body: token (refresh token)
     * Response: new accessToken, new refreshToken
     */
    @PostMapping("/refresh")
    public ApiBaseResponse<AuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest request) throws Exception {
        var result = authenticationService.refreshToken(request);
        return ApiBaseResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    // TODO: [FUTURE-EMAIL] Implement OTP verification endpoint
    @PostMapping("/verify-otp")
    public ApiBaseResponse<AuthenticationResponse> verifyOtp(@RequestBody @Valid VerifyOtpRequest request) {
        var result = authenticationService.verifyOtp(request);
        return ApiBaseResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    // TODO: [FUTURE-EMAIL] Implement forgot password endpoint
    @PostMapping("/forgot-password")
    public ApiBaseResponse<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        return authenticationService.forgotPassword(request);
    }
}
