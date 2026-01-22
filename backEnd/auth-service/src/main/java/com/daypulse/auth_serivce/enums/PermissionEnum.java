package com.daypulse.auth_serivce.enums;

/**
 * Permission Enum for Day Pulse Chat Application
 * 
 * Defines granular permissions that can be assigned to roles.
 * Used for fine-grained access control in JWT tokens and authorization checks.
 */
public enum PermissionEnum {
    // User basic permissions
    SEND_MESSAGE("Send messages in chat rooms"),
    JOIN_ROOM("Join and participate in chat rooms"),
    VIEW_PROFILE("View user profiles"),
    EDIT_OWN_PROFILE("Edit own profile information"),
    
    // Moderator permissions
    DELETE_MESSAGE("Delete messages in chat rooms"),
    MUTE_USER("Mute users temporarily"),
    BAN_USER("Ban users from rooms"),
    PIN_MESSAGE("Pin important messages"),
    
    // Admin permissions
    MANAGE_ROOMS("Create, edit, and delete chat rooms"),
    MANAGE_USERS("Manage user accounts"),
    MANAGE_ROLES("Assign roles to users"),
    VIEW_ANALYTICS("View system analytics and reports");
    
    private final String description;
    
    PermissionEnum(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns the permission name for JWT scope claim
     */
    public String getPermissionName() {
        return this.name();
    }
}
