package com.daypulse.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "user_profiles")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfile {
    @Id
    UUID id; // Same as users_auth.id from Auth Service

    @Column(unique = true, nullable = false, length = 50)
    String username;

    @Column(nullable = false, length = 100)
    String name;

    @Column(columnDefinition = "TEXT")
    String bio;

    @Column(length = 500)
    String avatarUrl;

    @Column(length = 50)
    @Builder.Default
    String timezone = "UTC";

    @Column(length = 5)
    @Builder.Default
    String language = "en";

    @Builder.Default
    Integer streak = 0;

    LocalDateTime lastPulseAt;

    @Builder.Default
    Boolean isOnline = false;

    LocalDateTime lastSeenAt;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}
