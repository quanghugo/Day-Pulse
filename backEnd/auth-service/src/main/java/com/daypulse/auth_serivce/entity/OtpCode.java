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
@Entity(name = "otp_codes")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtpCode {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    UserAuth user;

    @Column(nullable = false, length = 6)
    String code;

    @Column(nullable = false, length = 20)
    String type; // "email_verify", "password_reset"

    @Column(nullable = false)
    LocalDateTime expiresAt;

    LocalDateTime usedAt;

    @CreationTimestamp
    LocalDateTime createdAt;
}
