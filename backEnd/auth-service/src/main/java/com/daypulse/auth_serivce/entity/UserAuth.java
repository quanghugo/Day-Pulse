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

    /**
     * Keycloak User ID
     * This links our local user record to the Keycloak user
     * Password and OAuth credentials are stored in Keycloak
     */
    @Column(unique = true)
    UUID keycloakId;

    // Email verification status (managed by Keycloak but cached here)
    @Builder.Default
    Boolean isEmailVerified = false;

    @Builder.Default
    Boolean isSetupComplete = false;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

    // Role stored locally for quick access (synced from Keycloak)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    RoleEnum role = RoleEnum.USER;
}
