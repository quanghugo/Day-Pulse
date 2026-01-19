package com.daypulse.user_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "follows")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Follow {
    @EmbeddedId
    FollowId id;

    @CreationTimestamp
    LocalDateTime createdAt;
}
