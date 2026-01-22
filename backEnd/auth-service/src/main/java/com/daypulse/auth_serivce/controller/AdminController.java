package com.daypulse.auth_serivce.controller;

import com.daypulse.auth_serivce.dto.request.UpdateRoleRequest;
import com.daypulse.auth_serivce.dto.response.ApiBaseResponse;
import com.daypulse.auth_serivce.dto.response.RoleInfoResponse;
import com.daypulse.auth_serivce.dto.response.UserSummary;
import com.daypulse.auth_serivce.entity.UserAuth;
import com.daypulse.auth_serivce.enums.RoleEnum;
import com.daypulse.auth_serivce.mapper.UserMapper;
import com.daypulse.auth_serivce.service.UserRoleService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin Controller
 * 
 * Provides administrative endpoints for user and role management.
 * All endpoints require ADMIN role.
 * 
 * Endpoints:
 * - PATCH /admin/users/{id}/role - Update user role
 * - GET /admin/roles - List all available roles with permissions
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminController {
    
    UserRoleService userRoleService;
    UserMapper userMapper;
    
    /**
     * Update a user's role
     * 
     * ADMIN only - requires ROLE_ADMIN authority
     * 
     * @param userId User ID to update
     * @param request New role information
     * @return Updated user summary
     */
    @PatchMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiBaseResponse<UserSummary> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody @Valid UpdateRoleRequest request) {
        
        log.info("Admin updating user role: userId={}, newRole={}", userId, request.getRole());
        
        UserAuth updatedUser = userRoleService.updateUserRole(userId, request.getRole());
        
        return ApiBaseResponse.<UserSummary>builder()
                .result(userMapper.toUserSummary(updatedUser))
                .build();
    }
    
    /**
     * Get all available roles with their permissions
     * 
     * ADMIN only - requires ROLE_ADMIN authority
     * 
     * @return List of role information
     */
    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiBaseResponse<List<RoleInfoResponse>> getAllRoles() {
        log.info("Admin fetching all roles");
        
        List<RoleInfoResponse> roles = Arrays.stream(RoleEnum.values())
                .map(role -> RoleInfoResponse.builder()
                        .name(role.name())
                        .displayName(role.getDisplayName())
                        .permissions(role.getPermissions().stream()
                                .map(Enum::name)
                                .collect(Collectors.toSet()))
                        .build())
                .collect(Collectors.toList());
        
        return ApiBaseResponse.<List<RoleInfoResponse>>builder()
                .result(roles)
                .build();
    }
}
