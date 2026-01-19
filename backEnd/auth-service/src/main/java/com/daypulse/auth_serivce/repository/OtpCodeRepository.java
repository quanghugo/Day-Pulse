package com.daypulse.auth_serivce.repository;

import com.daypulse.auth_serivce.entity.OtpCode;
import com.daypulse.auth_serivce.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {
    Optional<OtpCode> findByUserAndCodeAndTypeAndUsedAtIsNullAndExpiresAtAfter(
            UserAuth user, String code, String type, LocalDateTime currentTime);

    Optional<OtpCode> findFirstByUserAndTypeAndUsedAtIsNullOrderByCreatedAtDesc(
            UserAuth user, String type);
}
