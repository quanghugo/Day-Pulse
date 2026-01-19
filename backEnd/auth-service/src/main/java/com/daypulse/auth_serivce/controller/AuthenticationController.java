package com.daypulse.auth_serivce.controller;

import com.daypulse.auth_serivce.dto.request.AuthenticationRequest;
import com.daypulse.auth_serivce.dto.request.IntrospectRequest;
import com.daypulse.auth_serivce.dto.request.LogoutRequest;
import com.daypulse.auth_serivce.dto.request.RefreshTokenRequest;
import com.daypulse.auth_serivce.dto.response.ApiBaseResponse;
import com.daypulse.auth_serivce.dto.response.AuthenticationResponse;
import com.daypulse.auth_serivce.dto.response.IntrospectResponse;
import com.daypulse.auth_serivce.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/token")
    public ApiBaseResponse<AuthenticationResponse> login(@RequestBody AuthenticationRequest authenticationRequest) {
        var result = authenticationService.authenticate(authenticationRequest);
        return ApiBaseResponse.<AuthenticationResponse>builder()
                .result(AuthenticationResponse.builder()
                        .authenticated(result.isAuthenticated())
                        .token(result.getToken())
                        .build())
                .build();
    }

    @PostMapping("/introspect")
    ApiBaseResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest introspectRequest) {
        var result = authenticationService.introspect(introspectRequest);
        return ApiBaseResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/logout")
    ApiBaseResponse<Void> logout(@RequestBody LogoutRequest logoutRequest) throws Exception {
        authenticationService.logout(logoutRequest.getToken());
        return ApiBaseResponse.<Void>builder().build();
    }

    @PostMapping("/refresh-token")
    public ApiBaseResponse<AuthenticationResponse> refreshToken
            (@RequestBody RefreshTokenRequest request) throws Exception {
        var result = authenticationService.refreshToken(request);
        return ApiBaseResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }
}
