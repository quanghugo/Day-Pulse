package com.daypulse.user_service.controller;

import com.daypulse.user_service.dto.response.ApiResponse;
import com.daypulse.user_service.dto.response.UserSummaryResponse;
import com.daypulse.user_service.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Internal API endpoints for inter-service communication.
 * These endpoints are not exposed through the API Gateway and are only accessible
 * from other services within the internal network.
 * 
 * NOTE: In production, these endpoints should be protected by service-to-service authentication
 * TODO: [FUTURE-SECURITY] Add service-to-service authentication using mTLS or service tokens
 */
@Slf4j
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {
    
    private final UserProfileService userProfileService;

    /**
     * Called by Feed Service to get user info for denormalization
     * When a user creates a status/pulse, the Feed Service needs to embed user info
     */
    @GetMapping("/{id}/summary")
    public ApiResponse<UserSummaryResponse> getUserSummary(@PathVariable UUID id) {
        log.info("Internal API: Getting user summary for user: {}", id);
        UserSummaryResponse response = userProfileService.getUserSummary(id);
        
        // TODO: [FUTURE-REDIS] Cache this response
        // redisTemplate.opsForValue().set("user:summary:" + id, response, Duration.ofMinutes(15));
        
        return ApiResponse.<UserSummaryResponse>builder()
                .result(response)
                .build();
    }

    /**
     * Called by Auth Service after successful registration
     * Initializes user profile and stats with default values
     */
    @PostMapping("/{id}/init")
    public ApiResponse<Void> initUserProfile(@PathVariable UUID id) {
        log.info("Internal API: Initializing user profile for user: {}", id);
        userProfileService.initUserProfile(id);
        return ApiResponse.<Void>builder()
                .message("User profile initialized successfully")
                .build();
    }
}
