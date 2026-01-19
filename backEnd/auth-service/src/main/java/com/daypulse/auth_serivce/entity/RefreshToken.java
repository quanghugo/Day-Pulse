package com.daypulse.auth_serivce.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "refresh_tokens")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    UserAuth user;

    @Column(nullable = false)
    String tokenHash;

    // TODO: [FUTURE-DEVICE] Track device information for multi-device support
    @Column(columnDefinition = "TEXT")
    String deviceInfo;

    @Column(nullable = false)
    LocalDateTime expiresAt;

    LocalDateTime revokedAt;

    @CreationTimestamp
    LocalDateTime createdAt;
}
