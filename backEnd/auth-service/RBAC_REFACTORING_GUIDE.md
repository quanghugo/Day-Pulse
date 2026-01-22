# RBAC Refactoring Guide - Enum-Based Roles & Permissions

## Overview

The Day Pulse authentication system has been refactored from a generic database-driven RBAC (Role-Based Access Control) model to a **compile-time safe enum-based system** optimized for chat applications.

### Key Changes

| Before | After |
|--------|-------|
| Separate `Role` and `Permission` JPA entities | `RoleEnum` and `PermissionEnum` Java enums |
| Many-to-many relationships (`user_roles`, `role_permissions` tables) | Single `role` column in `users_auth` table |
| Runtime role definitions (database) | Compile-time role definitions (enums) |
| No default data initialization | Automatic admin user creation on startup |
| Generic `/roles` and `/permissions` endpoints | Admin-protected `/admin/users/{id}/role` endpoint |

---

## Role Hierarchy

The system now implements a **3-tier role hierarchy** tailored for chat applications:

```
┌─────────────────────────────────────────────────────────┐
│                        ADMIN                             │
│  Full system access + all permissions below             │
│  • MANAGE_ROOMS, MANAGE_USERS, MANAGE_ROLES             │
│  • VIEW_ANALYTICS                                        │
├─────────────────────────────────────────────────────────┤
│                      MODERATOR                           │
│  Chat moderation + all USER permissions                 │
│  • DELETE_MESSAGE, MUTE_USER, BAN_USER, PIN_MESSAGE     │
├─────────────────────────────────────────────────────────┤
│                        USER                              │
│  Default role for all registered users                  │
│  • SEND_MESSAGE, JOIN_ROOM, VIEW_PROFILE                │
│  • EDIT_OWN_PROFILE                                      │
└─────────────────────────────────────────────────────────┘
```

---

## Enums Reference

### RoleEnum

**Location**: `com.daypulse.auth_serivce.enums.RoleEnum`

```java
public enum RoleEnum {
    USER,       // Default role
    MODERATOR,  // Chat moderators
    ADMIN       // System administrators
}
```

**Methods**:
- `getDisplayName()` - Human-readable name
- `getPermissions()` - Set of associated permissions
- `getRoleName()` - JWT scope format (e.g., "ROLE_USER")
- `hasPermission(PermissionEnum)` - Check permission

### PermissionEnum

**Location**: `com.daypulse.auth_serivce.enums.PermissionEnum`

```java
public enum PermissionEnum {
    // User permissions
    SEND_MESSAGE,
    JOIN_ROOM,
    VIEW_PROFILE,
    EDIT_OWN_PROFILE,
    
    // Moderator permissions
    DELETE_MESSAGE,
    MUTE_USER,
    BAN_USER,
    PIN_MESSAGE,
    
    // Admin permissions
    MANAGE_ROOMS,
    MANAGE_USERS,
    MANAGE_ROLES,
    VIEW_ANALYTICS
}
```

---

## JWT Token Changes

### JWT Scope Claim Format

**Before**:
```json
{
  "scope": "ROLE_USER",
  "userId": "abc-123-..."
}
```

**After** (includes permissions):
```json
{
  "scope": "ROLE_USER SEND_MESSAGE JOIN_ROOM VIEW_PROFILE EDIT_OWN_PROFILE",
  "userId": "abc-123-..."
}
```

The `buildScope()` method now automatically includes all permissions for the user's role.

---

## API Changes

### Deprecated Endpoints (Removed)

❌ **Old Role/Permission Management**:
- `POST /roles` - Create role
- `GET /roles` - List roles
- `DELETE /roles/{name}` - Delete role
- `POST /permissions` - Create permission
- `GET /permissions` - List permissions
- `DELETE /permissions/{name}` - Delete permission

### New Admin Endpoints

✅ **Admin-Only Role Management**:

#### 1. Update User Role
```http
PATCH /admin/users/{userId}/role
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "role": "MODERATOR"
}
```

**Response**:
```json
{
  "code": 200,
  "message": "Success",
  "result": {
    "id": "abc-123-...",
    "email": "user@example.com",
    "role": "MODERATOR"
  }
}
```

**Authorization**: Requires `ROLE_ADMIN` authority

#### 2. List Available Roles
```http
GET /admin/roles
Authorization: Bearer <admin-token>
```

**Response**:
```json
{
  "code": 200,
  "message": "Success",
  "result": [
    {
      "name": "USER",
      "displayName": "Regular User",
      "permissions": ["SEND_MESSAGE", "JOIN_ROOM", "VIEW_PROFILE", "EDIT_OWN_PROFILE"]
    },
    {
      "name": "MODERATOR",
      "displayName": "Moderator",
      "permissions": ["SEND_MESSAGE", "JOIN_ROOM", "VIEW_PROFILE", "EDIT_OWN_PROFILE", 
                      "DELETE_MESSAGE", "MUTE_USER", "BAN_USER", "PIN_MESSAGE"]
    },
    {
      "name": "ADMIN",
      "displayName": "Administrator",
      "permissions": ["...all permissions..."]
    }
  ]
}
```

**Authorization**: Requires `ROLE_ADMIN` authority

---

## Security Configuration

### SecurityConfig Updates

**Admin endpoints** are now protected at the security filter level:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity httpSecurity) {
    httpSecurity.authorizeHttpRequests(request ->
        request
            .requestMatchers("/admin/**").hasRole("ADMIN")  // ← New protection
            .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
            .anyRequest().authenticated()
    );
    return httpSecurity.build();
}
```

Additionally, `@PreAuthorize("hasRole('ADMIN')")` annotations are used on controller methods for defense in depth.

---

## Database Migration

### Migration Script

**File**: `src/main/resources/db/migration/V2__add_role_enum_column.sql`

The migration performs these steps:

1. Add `role` column to `users_auth` (VARCHAR(50), nullable)
2. Migrate existing roles: `user_roles` → `users_auth.role`
3. Default all users to `USER` role
4. Upgrade users with `ADMIN` in old system
5. Make `role` column NOT NULL with default `'USER'`
6. Drop junction tables: `user_roles`, `role_permissions`
7. Drop old tables: `roles`, `permissions`
8. Add index on `role` column

### Running the Migration

If using **Flyway** (recommended):
```bash
# Flyway will automatically detect and run V2 migration
mvn spring-boot:run
```

If **not using Flyway** (manual execution):
```bash
psql -U postgres -d auth-service -f src/main/resources/db/migration/V2__add_role_enum_column.sql
```

**Note**: If you already have the old tables, the migration handles data preservation. If starting fresh, Hibernate will auto-create the schema with the `role` column.

---

## Default Admin User

### Auto-Creation on Startup

The `DataInitializer` component automatically creates a default admin user if none exists:

**Default Credentials**:
- Email: `admin@daypulse.com`
- Password: `Admin@123`

⚠️ **IMPORTANT**: Change the admin password immediately after first login!

### Logs on Startup
```
=====================================================
DEFAULT ADMIN USER CREATED
Email: admin@daypulse.com
Password: Admin@123
IMPORTANT: Change the admin password after first login!
=====================================================
```

---

## Usage Examples

### 1. Register New User (Gets USER Role by Default)

```bash
curl -X POST http://localhost:8188/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "newuser@example.com",
    "password": "securePass123"
  }'
```

User is automatically assigned `RoleEnum.USER`.

### 2. Login as Admin

```bash
curl -X POST http://localhost:8188/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@daypulse.com",
    "password": "Admin@123"
  }'
```

**Response includes JWT with admin scope**:
```json
{
  "code": 200,
  "result": {
    "user": {
      "id": "...",
      "email": "admin@daypulse.com"
    },
    "tokens": {
      "accessToken": "eyJhbGci...",
      "refreshToken": "eyJhbGci...",
      "expiresIn": 3600,
      "tokenType": "Bearer"
    }
  }
}
```

JWT decoded:
```json
{
  "sub": "admin@daypulse.com",
  "userId": "...",
  "scope": "ROLE_ADMIN SEND_MESSAGE JOIN_ROOM ... MANAGE_ROLES VIEW_ANALYTICS",
  "exp": 1698764800
}
```

### 3. Promote User to Moderator (Admin Only)

```bash
curl -X PATCH http://localhost:8188/api/v1/admin/users/{userId}/role \
  -H "Authorization: Bearer <admin-access-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "role": "MODERATOR"
  }'
```

### 4. Check Available Roles (Admin Only)

```bash
curl -X GET http://localhost:8188/api/v1/admin/roles \
  -H "Authorization: Bearer <admin-access-token>"
```

---

## Code Examples

### Check User Role in Service

```java
@Service
public class ChatService {
    
    public void deleteMessage(UserAuth user, String messageId) {
        // Check if user has moderator or admin role
        if (user.getRole() == RoleEnum.ADMIN || user.getRole() == RoleEnum.MODERATOR) {
            // Delete message
        } else {
            throw new ForbiddenException("Insufficient permissions");
        }
    }
}
```

### Check Specific Permission

```java
public void performAdminAction(UserAuth user) {
    if (user.getRole().hasPermission(PermissionEnum.MANAGE_USERS)) {
        // Perform action
    } else {
        throw new ForbiddenException("Missing MANAGE_USERS permission");
    }
}
```

### Use in Controller with Spring Security

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
public ApiBaseResponse<Void> deleteUser(@PathVariable UUID id) {
    userService.deleteUser(id);
    return ApiBaseResponse.<Void>builder().build();
}
```

```java
@PreAuthorize("hasAuthority('DELETE_MESSAGE')")
@DeleteMapping("/messages/{id}")
public ApiBaseResponse<Void> deleteMessage(@PathVariable String id) {
    messageService.deleteMessage(id);
    return ApiBaseResponse.<Void>builder().build();
}
```

---

## Testing

### Unit Tests

```java
@Test
void testUserHasCorrectPermissions() {
    UserAuth user = UserAuth.builder()
        .email("user@example.com")
        .role(RoleEnum.USER)
        .build();
    
    assertTrue(user.getRole().hasPermission(PermissionEnum.SEND_MESSAGE));
    assertFalse(user.getRole().hasPermission(PermissionEnum.DELETE_MESSAGE));
}

@Test
void testModeratorHasUserPermissions() {
    UserAuth moderator = UserAuth.builder()
        .role(RoleEnum.MODERATOR)
        .build();
    
    // Has user permissions
    assertTrue(moderator.getRole().hasPermission(PermissionEnum.SEND_MESSAGE));
    // Has moderator permissions
    assertTrue(moderator.getRole().hasPermission(PermissionEnum.DELETE_MESSAGE));
    // Does not have admin permissions
    assertFalse(moderator.getRole().hasPermission(PermissionEnum.MANAGE_USERS));
}
```

### Integration Tests

```java
@Test
@WithMockUser(roles = "ADMIN")
void testAdminCanUpdateUserRole() throws Exception {
    mockMvc.perform(patch("/admin/users/{id}/role", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"role\":\"MODERATOR\"}"))
        .andExpect(status().isOk());
}

@Test
@WithMockUser(roles = "USER")
void testUserCannotAccessAdminEndpoints() throws Exception {
    mockMvc.perform(get("/admin/roles"))
        .andExpect(status().isForbidden());
}
```

---

## Troubleshooting

### Issue: "Access Denied" on /admin endpoints

**Cause**: JWT doesn't contain `ROLE_ADMIN` authority

**Solution**: 
1. Ensure user has `role = RoleEnum.ADMIN` in database
2. Login again to get new JWT with correct scope
3. Verify JWT contains `ROLE_ADMIN` in scope claim

### Issue: Migration fails with "table doesn't exist"

**Cause**: Old tables (`roles`, `permissions`) don't exist in fresh database

**Solution**: Migration script uses `IF EXISTS` clauses - this is safe. Hibernate will create the new schema automatically.

### Issue: Default admin not created

**Cause**: An admin user already exists

**Solution**: Check logs. If admin exists, the initializer skips creation. Use existing admin credentials.

### Issue: Compilation errors after refactoring

**Cause**: Old code still references deleted `Role`/`Permission` entities

**Solution**: 
1. Replace `Set<Role> roles` with `RoleEnum role` in all DTOs
2. Update mappers to handle single role instead of Set
3. Run `mvn clean compile` to detect remaining issues

---

## Benefits of Enum-Based RBAC

✅ **Compile-time Safety**: Roles and permissions are validated at compile time  
✅ **Simplified Model**: Single role per user (appropriate for chat apps)  
✅ **No Database Seeding**: Roles defined in code, always available  
✅ **Better Performance**: No joins for role/permission lookups  
✅ **Cleaner Code**: Type-safe enum methods instead of string comparisons  
✅ **Easy Versioning**: Role changes tracked in source control  

---

## Future Enhancements

### Possible Extensions

1. **Fine-grained permissions on specific resources**
   ```java
   @PreAuthorize("@chatRoomSecurity.canModerate(#roomId, authentication)")
   ```

2. **Temporary role assignments**
   ```java
   TemporaryRole.assignModeratorFor(userId, roomId, Duration.ofHours(24))
   ```

3. **Permission-based endpoints** (in addition to role-based)
   ```java
   @PreAuthorize("hasAuthority('DELETE_MESSAGE')")
   ```

4. **Audit logging for role changes**
   ```java
   @Audited
   private RoleEnum role;
   ```

---

## Migration Checklist

- [x] Create `RoleEnum` and `PermissionEnum`
- [x] Update `UserAuth` entity to use single `RoleEnum role`
- [x] Update `AuthenticationService.buildScope()` for enum-based roles
- [x] Create `UserRoleService` for role management
- [x] Create `AdminController` with protected endpoints
- [x] Update `SecurityConfig` to protect admin routes
- [x] Remove old `Role`/`Permission` entities and services
- [x] Create database migration script
- [x] Add `DataInitializer` for default admin
- [x] Update DTOs to use `RoleEnum`
- [x] Test admin endpoints with different roles
- [ ] Update frontend to call new admin endpoints
- [ ] Update API documentation (Swagger/OpenAPI)
- [ ] Train team on new role management workflow

---

## Support

For questions or issues with the new RBAC system:
1. Check this guide first
2. Review the enum javadocs
3. Examine `AdminController` for API usage
4. Check logs for role-related errors
5. Contact the backend team

---

**Last Updated**: 2025-01-21  
**Version**: 2.0 (Enum-based RBAC)
