package com.daypulse.auth_serivce.entity;

import com.daypulse.auth_serivce.enums.RoleEnum;
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
@Entity(name = "users_auth")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAuth {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(unique = true, nullable = false)
    String email;

    String passwordHash;

    // TODO: [FUTURE-OAUTH] OAuth provider integration (Google, Facebook, etc.)
    String oauthProvider;
    String oauthId;

    // TODO: [FUTURE-EMAIL] Email verification flow
    @Builder.Default
    Boolean isEmailVerified = false;

    @Builder.Default
    Boolean isSetupComplete = false;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

    // Simplified role model: Single role per user (enum-based for compile-time safety)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    RoleEnum role = RoleEnum.USER;
}
