package com.daypulse.user_service.service;

import com.daypulse.user_service.dto.request.ProfileSetupRequest;
import com.daypulse.user_service.dto.request.ProfileUpdateRequest;
import com.daypulse.user_service.dto.response.UserResponse;
import com.daypulse.user_service.dto.response.UserSummaryResponse;
import com.daypulse.user_service.entity.UserProfile;
import com.daypulse.user_service.entity.UserStats;
import com.daypulse.user_service.mapper.UserProfileMapper;
import com.daypulse.user_service.repository.UserProfileRepository;
import com.daypulse.user_service.repository.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {
    
    private final UserProfileRepository userProfileRepository;
    private final UserStatsRepository userStatsRepository;
    private final UserProfileMapper userProfileMapper;

    @Transactional
    public UserResponse setupProfile(UUID userId, ProfileSetupRequest request) {
        // Check if username is already taken
        if (userProfileRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        UserProfile profile = userProfileMapper.toUserProfile(request);
        profile.setId(userId);
        
        UserProfile savedProfile = userProfileRepository.save(profile);

        // Initialize user stats
        UserStats stats = UserStats.builder()
                .userId(userId)
                .followersCount(0)
                .followingCount(0)
                .pulsesCount(0)
                .build();
        userStatsRepository.save(stats);

        // TODO: [FUTURE-KAFKA] Publish profile.created event
        // kafkaTemplate.send("user.profile.created", ProfileCreatedEvent.builder()
        //     .userId(userId)
        //     .username(request.getUsername())
        //     .build());

        return userProfileMapper.toUserResponse(savedProfile);
    }

    public UserResponse getMyProfile(UUID userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return userProfileMapper.toUserResponse(profile);
    }

    @Transactional
    public UserResponse updateMyProfile(UUID userId, ProfileUpdateRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        // Check if new username is taken by another user
        if (request.getUsername() != null && !request.getUsername().equals(profile.getUsername())) {
            if (userProfileRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
        }

        userProfileMapper.updateUserProfile(profile, request);
        UserProfile updatedProfile = userProfileRepository.save(profile);

        // TODO: [FUTURE-KAFKA] Publish profile.updated event
        // kafkaTemplate.send("user.profile.updated", ProfileUpdatedEvent.builder()
        //     .userId(userId)
        //     .build());

        // TODO: [FUTURE-REDIS] Invalidate cache
        // redisTemplate.delete("user:profile:" + userId);

        return userProfileMapper.toUserResponse(updatedProfile);
    }

    public UserResponse getUserById(UUID userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userProfileMapper.toUserResponse(profile);
    }

    public List<UserSummaryResponse> getSuggestedUsers() {
        // TODO: Implement suggestion logic (e.g., popular users, mutual connections)
        Page<UserProfile> profiles = userProfileRepository.findAll(Pageable.ofSize(10));
        return profiles.stream()
                .map(userProfileMapper::toUserSummaryResponse)
                .toList();
    }

    public List<UserSummaryResponse> getAvailableUsers() {
        // TODO: Implement available users logic
        Page<UserProfile> profiles = userProfileRepository.findAll(Pageable.ofSize(20));
        return profiles.stream()
                .map(userProfileMapper::toUserSummaryResponse)
                .toList();
    }

    // Internal API method
    public UserSummaryResponse getUserSummary(UUID userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userProfileMapper.toUserSummaryResponse(profile);
    }

    // Internal API method
    @Transactional
    public void initUserProfile(UUID userId) {
        // Initialize empty profile for new user
        UserProfile profile = UserProfile.builder()
                .id(userId)
                .build();
        userProfileRepository.save(profile);

        UserStats stats = UserStats.builder()
                .userId(userId)
                .build();
        userStatsRepository.save(stats);
    }
}
