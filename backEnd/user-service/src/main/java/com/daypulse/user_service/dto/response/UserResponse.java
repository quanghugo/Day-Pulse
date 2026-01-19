package com.daypulse.user_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    UUID id;
    String username;
    String name;
    String bio;
    String avatarUrl;
    String timezone;
    String language;
    Integer streak;
    LocalDateTime lastPulseAt;
    Boolean isOnline;
    LocalDateTime lastSeenAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
