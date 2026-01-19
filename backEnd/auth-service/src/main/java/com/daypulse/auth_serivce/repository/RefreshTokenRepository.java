package com.daypulse.auth_serivce.repository;

import com.daypulse.auth_serivce.entity.RefreshToken;
import com.daypulse.auth_serivce.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);
    void deleteByUser(UserAuth user);
    boolean existsByTokenHash(String tokenHash);
}
