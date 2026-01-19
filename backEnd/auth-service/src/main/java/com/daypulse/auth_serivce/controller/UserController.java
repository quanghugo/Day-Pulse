package com.daypulse.auth_serivce.controller;

import com.daypulse.auth_serivce.dto.response.ApiBaseResponse;
import com.daypulse.auth_serivce.dto.response.UserResponse;
import com.daypulse.auth_serivce.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    // NOTE: User registration has been moved to /auth/register
    // NOTE: User profile management (CRUD) has been moved to User Service
    // This controller only keeps minimal auth-related user operations

    @GetMapping("/my-info")
    @PreAuthorize("isAuthenticated()")
    public ApiBaseResponse<UserResponse> getMyInfo() {
        return ApiBaseResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ApiBaseResponse<UserResponse> getUser(@PathVariable UUID userId) {
        return ApiBaseResponse.<UserResponse>builder()
                .result(userService.getUserById(userId))
                .build();
    }
}
