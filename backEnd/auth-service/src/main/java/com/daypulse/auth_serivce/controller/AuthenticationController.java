package com.daypulse.auth_serivce.controller;

import com.daypulse.auth_serivce.dto.request.*;
import com.daypulse.auth_serivce.dto.response.*;
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

    @PostMapping("/register")
    public ApiBaseResponse<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        var result = authenticationService.register(request);
        return ApiBaseResponse.<RegisterResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/login")
    public ApiBaseResponse<AuthenticationResponse> login(@RequestBody @Valid LoginRequest request) {
        var result = authenticationService.authenticate(request);
        return ApiBaseResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/introspect")
    public ApiBaseResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest introspectRequest) {
        var result = authenticationService.introspect(introspectRequest);
        return ApiBaseResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/logout")
    public ApiBaseResponse<Void> logout(@RequestBody LogoutRequest logoutRequest) throws Exception {
        authenticationService.logout(logoutRequest.getToken());
        return ApiBaseResponse.<Void>builder().build();
    }

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
