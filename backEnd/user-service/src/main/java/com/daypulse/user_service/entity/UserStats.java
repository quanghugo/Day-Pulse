package com.daypulse.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "user_stats")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserStats {
    @Id
    UUID userId;

    @Builder.Default
    Integer followersCount = 0;

    @Builder.Default
    Integer followingCount = 0;

    @Builder.Default
    Integer pulsesCount = 0;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}
