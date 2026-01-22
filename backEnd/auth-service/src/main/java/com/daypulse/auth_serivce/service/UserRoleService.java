package com.daypulse.auth_serivce.service;

import com.daypulse.auth_serivce.entity.UserAuth;
import com.daypulse.auth_serivce.enums.RoleEnum;
import com.daypulse.auth_serivce.exception.AppException;
import com.daypulse.auth_serivce.exception.ErrorCode;
import com.daypulse.auth_serivce.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing user roles
 * 
 * Provides operations to:
 * - Update user roles (admin only)
 * - Retrieve user roles
 * - List available roles
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserRoleService {
    
    UserRepository userRepository;
    
    /**
     * Update a user's role
     * 
     * @param userId User ID to update
     * @param newRole New role to assign
     * @return Updated user
     * @throws AppException if user not found
     */
    @Transactional
    public UserAuth updateUserRole(UUID userId, RoleEnum newRole) {
        UserAuth user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        RoleEnum oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);
        
        log.info("Updated user role: userId={}, oldRole={}, newRole={}", 
                userId, oldRole, newRole);
        
        return user;
    }
    
    /**
     * Get a user's current role
     * 
     * @param userId User ID
     * @return User's current role
     * @throws AppException if user not found
     */
    public RoleEnum getUserRole(UUID userId) {
        UserAuth user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        return user.getRole();
    }
    
    /**
     * Get all available roles
     * 
     * @return Array of all role enums
     */
    public RoleEnum[] getAllRoles() {
        return RoleEnum.values();
    }
}
