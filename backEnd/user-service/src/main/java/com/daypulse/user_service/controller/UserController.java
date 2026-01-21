package com.daypulse.user_service.controller;

import com.daypulse.user_service.dto.request.ProfileSetupRequest;
import com.daypulse.user_service.dto.request.ProfileUpdateRequest;
import com.daypulse.user_service.dto.response.*;
import com.daypulse.user_service.service.FollowService;
import com.daypulse.user_service.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserProfileService userProfileService;
    private final FollowService followService;

    /**
     * POST /users/me/setup - Protected endpoint
     * STANDARD: Client sends Authorization: Bearer <token> to API Gateway
     * Gateway extracts userId from JWT and forwards as X-User-Id header
     * Sets up user profile after registration
     */
    @PostMapping("/me/setup")
    public ApiResponse<UserResponse> setupProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody @Valid ProfileSetupRequest request) {
        log.info("Setting up profile for user: {}", userId);
        UserResponse response = userProfileService.setupProfile(UUID.fromString(userId), request);
        return ApiResponse.<UserResponse>builder()
                .result(response)
                .build();
    }

    /**
     * GET /users/me - Protected endpoint
     * STANDARD: Client sends Authorization: Bearer <token> to API Gateway
     * Gateway extracts userId from JWT and forwards as X-User-Id header
     * Returns current user's profile
     */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyProfile(@RequestHeader("X-User-Id") String userId) {
        UserResponse response = userProfileService.getMyProfile(UUID.fromString(userId));
        return ApiResponse.<UserResponse>builder()
                .result(response)
                .build();
    }

    /**
     * PATCH /users/me - Protected endpoint
     * STANDARD: Client sends Authorization: Bearer <token> to API Gateway
     * Gateway extracts userId from JWT and forwards as X-User-Id header
     * Updates current user's profile
     */
    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMyProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody @Valid ProfileUpdateRequest request) {
        log.info("Updating profile for user: {}", userId);
        UserResponse response = userProfileService.updateMyProfile(UUID.fromString(userId), request);
        return ApiResponse.<UserResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable UUID id) {
        UserResponse response = userProfileService.getUserById(id);
        return ApiResponse.<UserResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/{id}/followers")
    public ApiResponse<Page<UserSummaryResponse>> getFollowers(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserSummaryResponse> response = followService.getFollowers(id, pageable);
        return ApiResponse.<Page<UserSummaryResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/{id}/following")
    public ApiResponse<Page<UserSummaryResponse>> getFollowing(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserSummaryResponse> response = followService.getFollowing(id, pageable);
        return ApiResponse.<Page<UserSummaryResponse>>builder()
                .result(response)
                .build();
    }

    @PostMapping("/{id}/follow")
    public ApiResponse<FollowResponse> followUser(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        log.info("User {} following user {}", userId, id);
        FollowResponse response = followService.followUser(UUID.fromString(userId), id);
        return ApiResponse.<FollowResponse>builder()
                .result(response)
                .build();
    }

    @DeleteMapping("/{id}/follow")
    public ApiResponse<FollowResponse> unfollowUser(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        log.info("User {} unfollowing user {}", userId, id);
        FollowResponse response = followService.unfollowUser(UUID.fromString(userId), id);
        return ApiResponse.<FollowResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/suggested")
    public ApiResponse<List<UserSummaryResponse>> getSuggestedUsers() {
        List<UserSummaryResponse> response = userProfileService.getSuggestedUsers();
        return ApiResponse.<List<UserSummaryResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/available")
    public ApiResponse<List<UserSummaryResponse>> getAvailableUsers() {
        List<UserSummaryResponse> response = userProfileService.getAvailableUsers();
        return ApiResponse.<List<UserSummaryResponse>>builder()
                .result(response)
                .build();
    }
}
