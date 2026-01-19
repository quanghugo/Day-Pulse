package com.daypulse.auth_serivce.repository;

import com.daypulse.auth_serivce.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserAuth, UUID> {
    boolean existsByEmail(String email);
    Optional<UserAuth> findByEmail(String email);
    Optional<UserAuth> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);
}
