package com.daypulse.user_service.service;

import com.daypulse.user_service.dto.response.FollowResponse;
import com.daypulse.user_service.dto.response.UserSummaryResponse;
import com.daypulse.user_service.entity.Follow;
import com.daypulse.user_service.entity.FollowId;
import com.daypulse.user_service.entity.UserStats;
import com.daypulse.user_service.mapper.UserProfileMapper;
import com.daypulse.user_service.repository.FollowRepository;
import com.daypulse.user_service.repository.UserProfileRepository;
import com.daypulse.user_service.repository.UserStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowService {
    
    private final FollowRepository followRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserStatsRepository userStatsRepository;
    private final UserProfileMapper userProfileMapper;

    @Transactional
    public FollowResponse followUser(UUID followerId, UUID followingId) {
        if (followerId.equals(followingId)) {
            throw new RuntimeException("Cannot follow yourself");
        }

        // Check if target user exists
        if (!userProfileRepository.existsById(followingId)) {
            throw new RuntimeException("User not found");
        }

        FollowId followId = new FollowId(followerId, followingId);
        
        // Check if already following
        if (followRepository.existsById(followId)) {
            return FollowResponse.builder()
                    .success(true)
                    .message("Already following")
                    .build();
        }

        // Create follow relationship
        Follow follow = Follow.builder()
                .id(followId)
                .build();
        followRepository.save(follow);

        // Update stats
        updateFollowStats(followerId, followingId, true);

        // TODO: [FUTURE-KAFKA] Publish follow.created event
        // kafkaTemplate.send("user.follow.created", FollowCreatedEvent.builder()
        //     .followerId(followerId)
        //     .followingId(followingId)
        //     .timestamp(LocalDateTime.now())
        //     .build());

        // TODO: [FUTURE-REDIS] Invalidate caches
        // redisTemplate.delete("user:followers:" + followingId);
        // redisTemplate.delete("user:following:" + followerId);

        return FollowResponse.builder()
                .success(true)
                .message("Successfully followed user")
                .build();
    }

    @Transactional
    public FollowResponse unfollowUser(UUID followerId, UUID followingId) {
        FollowId followId = new FollowId(followerId, followingId);
        
        if (!followRepository.existsById(followId)) {
            return FollowResponse.builder()
                    .success(true)
                    .message("Not following")
                    .build();
        }

        followRepository.deleteById(followId);

        // Update stats
        updateFollowStats(followerId, followingId, false);

        // TODO: [FUTURE-KAFKA] Publish follow.deleted event
        // kafkaTemplate.send("user.follow.deleted", FollowDeletedEvent.builder()
        //     .followerId(followerId)
        //     .followingId(followingId)
        //     .timestamp(LocalDateTime.now())
        //     .build());

        return FollowResponse.builder()
                .success(true)
                .message("Successfully unfollowed user")
                .build();
    }

    public Page<UserSummaryResponse> getFollowers(UUID userId, Pageable pageable) {
        Page<Follow> follows = followRepository.findFollowersByUserId(userId, pageable);
        return follows.map(follow -> {
            UUID followerId = follow.getId().getFollowerId();
            return userProfileRepository.findById(followerId)
                    .map(userProfileMapper::toUserSummaryResponse)
                    .orElse(null);
        });
    }

    public Page<UserSummaryResponse> getFollowing(UUID userId, Pageable pageable) {
        Page<Follow> follows = followRepository.findFollowingByUserId(userId, pageable);
        return follows.map(follow -> {
            UUID followingId = follow.getId().getFollowingId();
            return userProfileRepository.findById(followingId)
                    .map(userProfileMapper::toUserSummaryResponse)
                    .orElse(null);
        });
    }

    private void updateFollowStats(UUID followerId, UUID followingId, boolean isFollow) {
        // PERFORMANCE FIX: Use orElseGet to avoid unnecessary object creation
        // Update follower's following count
        UserStats followerStats = userStatsRepository.findById(followerId)
                .orElseGet(() -> UserStats.builder()
                        .userId(followerId)
                        .followersCount(0)
                        .followingCount(0)
                        .pulsesCount(0)
                        .build());
        
        int currentFollowingCount = followerStats.getFollowingCount() != null ? followerStats.getFollowingCount() : 0;
        followerStats.setFollowingCount(currentFollowingCount + (isFollow ? 1 : -1));
        userStatsRepository.save(followerStats);

        // Update following's followers count
        UserStats followingStats = userStatsRepository.findById(followingId)
                .orElseGet(() -> UserStats.builder()
                        .userId(followingId)
                        .followersCount(0)
                        .followingCount(0)
                        .pulsesCount(0)
                        .build());
        
        int currentFollowersCount = followingStats.getFollowersCount() != null ? followingStats.getFollowersCount() : 0;
        followingStats.setFollowersCount(currentFollowersCount + (isFollow ? 1 : -1));
        userStatsRepository.save(followingStats);
        
        // TODO: [FUTURE-OPTIMIZATION] Use batch update or increment queries for better performance
        // @Query("UPDATE user_stats SET followingCount = followingCount + :delta WHERE userId = :userId")
    }
}
