package com.daypulse.auth_serivce.enums;

import java.util.Set;

/**
 * Role Enum for Day Pulse Chat Application
 * 
 * Defines hierarchical roles with associated permissions.
 * Each role includes all permissions from lower-level roles plus additional ones.
 */
public enum RoleEnum {
    /**
     * USER - Default role for all registered users
     * Basic chat functionality
     */
    USER(
        "Regular User", 
        Set.of(
            PermissionEnum.SEND_MESSAGE,
            PermissionEnum.JOIN_ROOM,
            PermissionEnum.VIEW_PROFILE,
            PermissionEnum.EDIT_OWN_PROFILE
        )
    ),
    
    /**
     * MODERATOR - Chat moderators with moderation capabilities
     * Includes all USER permissions plus moderation tools
     */
    MODERATOR(
        "Moderator",
        Set.of(
            // User permissions
            PermissionEnum.SEND_MESSAGE,
            PermissionEnum.JOIN_ROOM,
            PermissionEnum.VIEW_PROFILE,
            PermissionEnum.EDIT_OWN_PROFILE,
            // Moderator permissions
            PermissionEnum.DELETE_MESSAGE,
            PermissionEnum.MUTE_USER,
            PermissionEnum.BAN_USER,
            PermissionEnum.PIN_MESSAGE
        )
    ),
    
    /**
     * ADMIN - System administrators with full access
     * Includes all MODERATOR permissions plus administrative capabilities
     */
    ADMIN(
        "Administrator",
        Set.of(
            // User permissions
            PermissionEnum.SEND_MESSAGE,
            PermissionEnum.JOIN_ROOM,
            PermissionEnum.VIEW_PROFILE,
            PermissionEnum.EDIT_OWN_PROFILE,
            // Moderator permissions
            PermissionEnum.DELETE_MESSAGE,
            PermissionEnum.MUTE_USER,
            PermissionEnum.BAN_USER,
            PermissionEnum.PIN_MESSAGE,
            // Admin permissions
            PermissionEnum.MANAGE_ROOMS,
            PermissionEnum.MANAGE_USERS,
            PermissionEnum.MANAGE_ROLES,
            PermissionEnum.VIEW_ANALYTICS
        )
    );
    
    private final String displayName;
    private final Set<PermissionEnum> permissions;
    
    RoleEnum(String displayName, Set<PermissionEnum> permissions) {
        this.displayName = displayName;
        this.permissions = permissions;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Set<PermissionEnum> getPermissions() {
        return permissions;
    }
    
    /**
     * Returns the role name for JWT scope claim (with ROLE_ prefix)
     */
    public String getRoleName() {
        return "ROLE_" + this.name();
    }
    
    /**
     * Check if this role has a specific permission
     */
    public boolean hasPermission(PermissionEnum permission) {
        return permissions.contains(permission);
    }
}
