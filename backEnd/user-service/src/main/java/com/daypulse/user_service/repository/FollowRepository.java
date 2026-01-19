package com.daypulse.user_service.repository;

import com.daypulse.user_service.entity.Follow;
import com.daypulse.user_service.entity.FollowId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FollowRepository extends JpaRepository<Follow, FollowId> {
    
    @Query("SELECT f FROM follows f WHERE f.id.followingId = :userId")
    Page<Follow> findFollowersByUserId(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT f FROM follows f WHERE f.id.followerId = :userId")
    Page<Follow> findFollowingByUserId(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT COUNT(f) FROM follows f WHERE f.id.followingId = :userId")
    long countFollowersByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT COUNT(f) FROM follows f WHERE f.id.followerId = :userId")
    long countFollowingByUserId(@Param("userId") UUID userId);
}
