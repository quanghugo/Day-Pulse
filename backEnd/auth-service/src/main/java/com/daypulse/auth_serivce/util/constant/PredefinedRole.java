package com.daypulse.auth_serivce.util.constant;

/**
 * @deprecated Use {@link com.daypulse.auth_serivce.enums.RoleEnum} instead
 * 
 * This class is kept for backward compatibility but will be removed in future versions.
 * The system now uses enum-based roles (RoleEnum) for compile-time safety.
 */
@Deprecated(since = "2.0", forRemoval = true)
public class PredefinedRole {
    /**
     * @deprecated Use {@link com.daypulse.auth_serivce.enums.RoleEnum#ADMIN} instead
     */
    @Deprecated
    public static final String ROLE_ADMIN = "ADMIN";
    
    /**
     * @deprecated Use {@link com.daypulse.auth_serivce.enums.RoleEnum#USER} instead
     */
    @Deprecated
    public static final String ROLE_USER = "USER";

    private PredefinedRole(){}
}
