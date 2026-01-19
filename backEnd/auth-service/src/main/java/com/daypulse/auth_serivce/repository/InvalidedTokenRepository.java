package com.daypulse.auth_serivce.repository;

import com.daypulse.auth_serivce.entity.InvalidedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvalidedTokenRepository extends JpaRepository<InvalidedToken, String> {
}
